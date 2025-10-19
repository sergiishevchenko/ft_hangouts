package com.example.ft_hangouts_42.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ft_hangouts_42.data.MessageRepository
import com.example.ft_hangouts_42.data.room.MessageEntity
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(
    contactName: String,
    contactId: Long,
    repo: MessageRepository,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf<MessageEntity>()) }
    var inputText by remember { mutableStateOf("") }

    // Загрузка сообщений
    LaunchedEffect(contactId) {
        messages = repo.getMessagesForContact(contactId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName) },
                actions = {
                    TextButton(onClick = onClose) { Text("Close", color = Color.White) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    MessageItem(msg)
                }
            }

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (inputText.isNotBlank()) {
                        scope.launch {
                            val newMsg = MessageEntity(
                                contactId = contactId,
                                text = inputText,
                                timestamp = System.currentTimeMillis(),
                                isSent = true
                            )
                            repo.addMessage(newMsg)
                            messages = repo.getMessagesForContact(contactId)
                            inputText = ""
                        }
                    }
                }) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: MessageEntity) {
    val alignment = if (message.isSent) Arrangement.End else Arrangement.Start
    val bgColor = if (message.isSent) Color(0xFFDCF8C6) else Color(0xFFEFEFEF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = alignment
    ) {
        Box(
            modifier = Modifier
                .background(color = bgColor, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Text(message.text)
        }
    }
}
