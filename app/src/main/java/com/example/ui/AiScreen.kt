package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Query template suggestions
    val suggestions = listOf(
        "¿De dónde saca el BCV esta tasa?",
        "¿Cómo afecta el tipo de cambio bcv hoy?",
        "Tasa oficial vs paralela",
        "Convertir 100 USD a Bs"
    )

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(chatMessages.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header Section ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "ASISTENTENTE FINANCIERO IA",
                    color = MutedOlive,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Consultoría de Mercado",
                    color = NaturalCharcoal,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { viewModel.clearChat() },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("clear_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Limpiar Conversación",
                    tint = MutedOlive,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // --- Standard Suggestions Rows ---
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Preguntas Sugeridas",
                color = MutedOlive,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.take(2).forEach { text ->
                    SuggestionChipItem(text = text) {
                        viewModel.sendChatMessage(text)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.drop(2).forEach { text ->
                    SuggestionChipItem(text = text) {
                        viewModel.sendChatMessage(text)
                    }
                }
            }
        }

        // --- Scrollable Dialogue Bubble List ---
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .testTag("ai_chat_log_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages) { messagePair ->
                    val (text, isUser) = messagePair
                    ChatBubble(text = text, isUser = isUser)
                }

                if (isChatLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MossGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Analizando divisas...",
                                color = MutedOlive,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // --- Chat Query Text Input Box ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Escribe una pregunta sobre la tasa...", color = MutedOlive.copy(alpha = 0.5f), fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MossGreen,
                    unfocusedBorderColor = StoneBeige,
                    cursorColor = MossGreen
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field")
            )

            IconButton(
                onClick = {
                    if (inputQuery.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(inputQuery)
                        inputQuery = ""
                    }
                },
                enabled = !isChatLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isChatLoading) StoneBeige else SageGreen)
                    .testTag("btn_chat_send")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = if (isChatLoading) MutedOlive else SageDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SuggestionChipItem(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ClayGrey)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = MutedOlive,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ChatBubble(
    text: String,
    isUser: Boolean
) {
    val bubbleColor = if (isUser) SageGreen else ClayGrey
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val txtColor = if (isUser) SageDark else NaturalCharcoal
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = text,
                color = txtColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
        }
    }
}
