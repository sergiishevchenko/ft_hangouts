package com.example.ft_hangouts_42.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.MessageRepository
import com.example.ft_hangouts_42.data.room.ContactEntity
import com.example.ft_hangouts_42.ui.contact.ContactEditScreen
import com.example.ft_hangouts_42.ui.conversation.ConversationScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var contactRepo: ContactRepository
    private lateinit var messageRepo: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactRepo = ContactRepository(this)
        messageRepo = MessageRepository(this)

        setContent {
            MaterialTheme {
                MainScreen(contactRepo, messageRepo)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putLong("last_background_ts", System.currentTimeMillis())
            .apply()
    }
}

@Composable
fun MainScreen(contactRepo: ContactRepository, messageRepo: MessageRepository) {
    val scope = rememberCoroutineScope()

    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    var showEdit by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<ContactEntity?>(null) }

    var showConversation by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<ContactEntity?>(null) }

    var topBarColor by remember { mutableStateOf(Color(0xFF6200EE)) }

    // Загрузка контактов из Room
    LaunchedEffect(Unit) {
        contacts = contactRepo.getAllContacts()
    }

    // Toast для времени последнего фонового состояния
    val prefs = LocalContext.current.getSharedPreferences("prefs", 0)
    val lastTs = prefs.getLong("last_background_ts", 0L)
    LaunchedEffect(lastTs) {
        if (lastTs != 0L) {
            val s = SimpleDateFormat.getDateTimeInstance().format(Date(lastTs))
            Toast.makeText(LocalContext.current, "Last backgrounded at $s", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FtHangouts") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor),
                actions = {
                    IconButton(onClick = { topBarColor = if (topBarColor == Color.Red) Color.Blue else Color.Red }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Change Color")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                contactToEdit = null
                showEdit = true
            }) { Text("+") }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(contacts) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .clickable {
                                selectedContact = contact
                                showConversation = true
                            },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(contact.name, style = MaterialTheme.typography.titleMedium)
                            Text(contact.phone, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Редактирование контакта
            if (showEdit) {
                ContactEditScreen(
                    repo = contactRepo,
                    contact = contactToEdit,
                    onClose = {
                        scope.launch { contacts = contactRepo.getAllContacts() }
                        showEdit = false
                    }
                )
            }

            // Экран переписки
            if (showConversation && selectedContact != null) {
                ConversationScreen(
                    contactName = selectedContact!!.name,
                    contactId = selectedContact!!.id,
                    repo = messageRepo,
                    onClose = { showConversation = false }
                )
            }
        }
    }
}
