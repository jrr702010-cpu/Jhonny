package com.example.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScrapResult(
    val success: Boolean,
    val usd: Double = 0.0,
    val eur: Double = 0.0,
    val cny: Double = 0.0,
    val tryLira: Double = 0.0,
    val rub: Double = 0.0,
    val dateText: String = "",
    val errorMsg: String? = null
)

object BcvScraper {
    private const val TAG = "BcvScraper"
    private const val BCV_URL = "http://www.bcv.org.ve" // Primary HTTP site (more stable than HTTPS on old devices)

    // Relaxed SSL client to prevent SSL Handshake failures on BCV's misconfigured server certs
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                        .header("Accept-Language", "es-ES,es;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Connection", "keep-alive")
                        .build()
                    chain.proceed(request)
                }
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error installing relaxed TrustManager", e)
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }
    }

    suspend fun fetchRatesDirectly(): ScrapResult = withContext(Dispatchers.IO) {
        val client = getUnsafeOkHttpClient()
        val request = Request.Builder().url(BCV_URL).build()

        try {
            Log.d(TAG, "Requesting BCV HTML from $BCV_URL")
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ScrapResult(
                    success = false,
                    errorMsg = "Código de respuesta no exitoso: ${response.code}"
                )
            }

            val html = response.body?.string() ?: ""
            if (html.isEmpty()) {
                return@withContext ScrapResult(
                    success = false,
                    errorMsg = "La respuesta del servidor está vacía"
                )
            }

            Log.d(TAG, "BCV Response acquired, body length: ${html.length}")

            // Extract rates using regex substring blocks
            val usd = extractRate(html, "dolar") ?: 0.0
            val eur = extractRate(html, "euro") ?: 0.0
            val cny = extractRate(html, "yuan") ?: 0.0
            val tryLira = extractRate(html, "lira") ?: 0.0
            val rub = extractRate(html, "rublo") ?: 0.0
            val dateText = extractDate(html)

            if (usd == 0.0) {
                Log.e(TAG, "Could not scrape USD rate from HTML. Sample: ${html.take(500)}")
                return@withContext ScrapResult(
                    success = false,
                    errorMsg = "No se pudo extraer la tasa oficial del Dólar"
                )
            }

            Log.d(TAG, "Scrape succeeded. USD: $usd, EUR: $eur, Date: $dateText")
            return@withContext ScrapResult(
                success = true,
                usd = usd,
                eur = eur,
                cny = cny,
                tryLira = tryLira,
                rub = rub,
                dateText = dateText
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during direct scraping", e)
            return@withContext ScrapResult(
                success = false,
                errorMsg = "Error de red o conexión: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    private fun extractRate(html: String, currencyId: String): Double? {
        val targetIndex = html.indexOf("id=\"$currencyId\"")
        val finalStart = if (targetIndex != -1) {
            targetIndex
        } else {
            val altIndex = html.indexOf("id='$currencyId'")
            if (altIndex != -1) altIndex else return null
        }

        val limit = (finalStart + 1500).coerceAtMost(html.length)
        val chunk = html.substring(finalStart, limit)

        // Try Pattern 1: Strong containing a comma-separated decimal number
        val regexStrong = """<strong>\s*([0-9.,]+)\s*</strong>""".toRegex()
        val matchStrong = regexStrong.find(chunk)
        if (matchStrong != null) {
            val rawValue = matchStrong.groupValues[1]
            val cleanValue = rawValue.replace(".", "").replace(",", ".").trim()
            val parsed = cleanValue.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) return parsed
        }

        // Try Pattern 2: Field-content container with numbers inside
        val regexField = """field-content[^>]*>\s*([0-9.,]+)\s*<""".toRegex()
        val matchField = regexField.find(chunk)
        if (matchField != null) {
            val rawValue = matchField.groupValues[1]
            val cleanValue = rawValue.replace(".", "").replace(",", ".").trim()
            val parsed = cleanValue.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) return parsed
        }

        // Try Pattern 3: Find any float-like decimal (Spanish locale, e.g. "36,4203") inside target chunk
        val regexGenericDecimal = """([0-9]+,[0-9]{3,10})""".toRegex()
        val matchGeneric = regexGenericDecimal.find(chunk)
        if (matchGeneric != null) {
            val rawValue = matchGeneric.groupValues[1]
            val cleanValue = rawValue.replace(".", "").replace(",", ".").trim()
            val parsed = cleanValue.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) return parsed
        }

        return null
    }

    private fun extractDate(html: String): String {
        // Try searching for date-display-single pattern
        val dateDisplayRegex = """class="date-display-single"[^>]*>\s*([^<]+)\s*</span>""".toRegex()
        val matchDisplay = dateDisplayRegex.find(html)
        if (matchDisplay != null) {
            val rawDate = matchDisplay.groupValues[1].trim()
            if (rawDate.isNotEmpty()) {
                return cleanDateText(rawDate)
            }
        }

        // Try searching for "Fecha de Valor:" block
        val valueDateIndex = html.indexOf("Fecha de Valor:")
        val indexToUse = if (valueDateIndex != -1) valueDateIndex else html.indexOf("Fecha Valor:")
        if (indexToUse != -1) {
            val limit = (indexToUse + 250).coerceAtMost(html.length)
            val chunk = html.substring(indexToUse, limit)
            val innerTextRegex = """>([^<]{10,50})<""".toRegex()
            val matches = innerTextRegex.findAll(chunk)
            for (m in matches) {
                val candidate = m.groupValues[1].trim()
                if (candidate.any { it.isLetter() } && candidate.any { it.isDigit() }) {
                    return cleanDateText(candidate)
                }
            }
        }

        // Return current date localized to Venezuela
        val formatter = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "VE"))
        return formatter.format(Date())
    }

    private fun cleanDateText(input: String): String {
        return input.replace("&nbsp;", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
