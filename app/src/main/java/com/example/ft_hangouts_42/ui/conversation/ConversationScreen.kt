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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import com.example.ft_hangouts_42.data.MessageRepository
import com.example.ft_hangouts_42.data.room.MessageEntity
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.res.stringResource
import com.example.ft_hangouts_42.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    contactName: String,
    contactId: Long,
    repo: MessageRepository,
    onNavigateToContacts: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val messages by repo.getMessagesForContact(contactId).collectAsState(initial = emptyList())
    var inputText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contactName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToContacts) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to contacts"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                reverseLayout = false
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageItem(msg, contactName)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .padding(bottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.type_message)) }
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
                            inputText = ""
                        }
                    }
                }) {
                    Text(stringResource(R.string.send))
                }
            }
        }
    }
}

@Composable
fun MessageItem(message: MessageEntity, senderName: String) {
    val alignment = if (message.isSent) Arrangement.End else Arrangement.Start
    val bgColor = if (message.isSent) Color(0xFFDCF8C6) else Color(0xFFEFEFEF)
    val senderLabel = if (message.isSent) "Me" else senderName

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isSent) Alignment.End else Alignment.Start
        ) {
            Text(
                text = senderLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
                // modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .background(color = bgColor, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(message.text)
            }
        }
    }
}