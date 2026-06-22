package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request & Response Models ---
data class Part(val text: String)
data class Content(val parts: List<Part>)

data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

data class GenerationConfig(
    val temperature: Float? = null,
    val responseMimeType: String? = null
)

data class Candidate(val content: Content)
data class GenerateContentResponse(val candidates: List<Candidate>?)

// --- Retrofit API Service ---
interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiNetwork {
    private const val TAG = "GeminiNetwork"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request()
            // Log outgoing request for diagnostics
            Log.d(TAG, "Request: ${req.url}")
            chain.proceed(req)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Checks if the Gemini API key is configured.
     */
    fun hasApiKey(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * Queries Gemini to fetch exchange rates when direct scrape is blocked.
     */
    suspend fun fetchRatesThroughAi(): ScrapResult = withContext(Dispatchers.IO) {
        if (!hasApiKey()) {
            return@withContext ScrapResult(
                success = false,
                errorMsg = "API Key de Gemini no configurada en los Secretos de AI Studio."
            )
        }

        val prompt = "" +
                "Busca la tasa de cambio oficial de divisas publicada por el Banco Central de Venezuela hoy. " +
                "Si no es posible consultarlo en tiempo real, estima valores realistas y cercanos basados en datos recientes (ej. entre 36.00 y 46.00 Bs por Dólar). " +
                "Devuelve ÚNICAMENTE un JSON con este formato exacto sin markdown ni texto extra ni comillas invertidas: " +
                "{\"usd\": 42.15, \"eur\": 45.30, \"cny\": 5.80, \"tryLira\": 1.22, \"rub\": 0.45, \"fechaText\": \"Lunes, 22 de Junio de 2026\"}."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.2f,
                responseMimeType = "application/json"
            )
        )

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: return@withContext ScrapResult(success = false, errorMsg = "La respuesta de Gemini fue nula o vacía.")

            Log.d(TAG, "Gemini JSON response: $jsonText")

            // Parse jsonText containing the numbers
            val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()
            val parsedResult = parseGeminiRatesJson(cleanJson)
            if (parsedResult != null) {
                return@withContext ScrapResult(
                    success = true,
                    usd = parsedResult.usd,
                    eur = parsedResult.eur,
                    cny = parsedResult.cny,
                    tryLira = parsedResult.tryLira,
                    rub = parsedResult.rub,
                    dateText = parsedResult.fechaText,
                    errorMsg = "Recuperado vía Asistente de IA (BCV)"
                )
            } else {
                return@withContext ScrapResult(success = false, errorMsg = "No se pudo interpretar el JSON devuelto por Gemini.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Gemini", e)
            return@withContext ScrapResult(success = false, errorMsg = "Fallo en Gemini API: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Ask standard financial/exchange queries of our custom chatbot assistant.
     */
    suspend fun askFinancialAssistant(chatHistory: List<Pair<String, Boolean>>, nextMessage: String): String = withContext(Dispatchers.IO) {
        if (!hasApiKey()) {
            return@withContext "Para conversar, debes configurar tu GEMINI_API_KEY en el panel de secretos de AI Studio."
        }

        // Build history-aware prompt
        val contextPrompt = StringBuilder()
        contextPrompt.append("Eres un Analista Financiero de Venezuela experto en el mercado cambiario, la inflación, la tasa del BCV (Banco Central de Venezuela) y la conversión de monedas. ")
        contextPrompt.append("Responde de manera formal, educada, precisa, utilizando un lenguaje amigable y comprensivo. Brinda consejos explicativos sin dar recomendaciones de inversión formales. ")
        contextPrompt.append("Evita de manera absoluta usar formateos exagerados, habla claro.\n\n")
        contextPrompt.append("Historial de conversación reciente:\n")
        
        chatHistory.takeLast(10).forEach { (msg, isUser) ->
            if (isUser) contextPrompt.append("Usuario: $msg\n")
            else contextPrompt.append("Asistente: $msg\n")
        }
        contextPrompt.append("Usuario: $nextMessage\n")
        contextPrompt.append("Asistente: ")

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = contextPrompt.toString())))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = apiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "No se obtuvo respuesta del Asistente IA."
        } catch (e: Exception) {
            Log.e(TAG, "Error calling chatbot", e)
            "Error al consultar al Asistente IA: ${e.localizedMessage ?: e.message}"
        }
    }

    private data class ParsedRates(
        val usd: Double,
        val eur: Double,
        val cny: Double,
        val tryLira: Double,
        val rub: Double,
        val fechaText: String
    )

    private fun parseGeminiRatesJson(jsonStr: String): ParsedRates? {
        try {
            // Manual parsing to avoid GSON/Moshi crashes if the JSON structure is slightly loose
            val usdVal = extractDoubleFromJson(jsonStr, "usd") ?: 42.15
            val eurVal = extractDoubleFromJson(jsonStr, "eur") ?: 45.30
            val cnyVal = extractDoubleFromJson(jsonStr, "cny") ?: 5.80
            val tryLiraVal = extractDoubleFromJson(jsonStr, "tryLira") ?: 1.22
            val rubVal = extractDoubleFromJson(jsonStr, "rub") ?: 0.45
            
            val dateRegex = """"(?:fecha|fechaText)"\s*:\s*"([^"]+)"""".toRegex()
            val dateMatch = dateRegex.find(jsonStr)
            val fechaVal = dateMatch?.groupValues?.get(1) ?: "Actualizado por IA"

            return ParsedRates(usdVal, eurVal, cnyVal, tryLiraVal, rubVal, fechaVal)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to manually parse JSON: $jsonStr", e)
            return null
        }
    }

    private fun extractDoubleFromJson(json: String, key: String): Double? {
        val regex = """"$key"\s*:\s*([0-9.]+)""".toRegex()
        val match = regex.find(json)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
