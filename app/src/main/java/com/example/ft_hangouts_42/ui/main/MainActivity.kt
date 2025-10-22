package com.example.ft_hangouts_42.ui.main

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.MessageRepository
import com.example.ft_hangouts_42.data.room.ContactEntity
import com.example.ft_hangouts_42.ui.contact.ContactEditScreen
import com.example.ft_hangouts_42.ui.conversation.ConversationScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import com.example.ft_hangouts_42.utils.LocaleHelper
import android.content.Context
import androidx.compose.material.icons.filled.Language
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var contactRepo: ContactRepository
    private lateinit var messageRepo: MessageRepository

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleHelper.getSavedLanguage(newBase)
        val contextWithLocale = LocaleHelper.setLocale(newBase, lang)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestSmsPermission()

        contactRepo = ContactRepository(this)
        messageRepo = MessageRepository(this)

        setContent {
            MaterialTheme {
                MainScreen(contactRepo, messageRepo)
            }
        }
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 100)
        }
    }

    @Suppress("Deprecated")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
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
    val context = LocalContext.current

    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    var showEdit by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<ContactEntity?>(null) }

    var showConversation by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<ContactEntity?>(null) }

    var topBarColor by remember { mutableStateOf(Color(0xFF6200EE)) }

    LaunchedEffect(Unit) {
        contacts = contactRepo.getAllContacts()
    }

    val prefs = context.getSharedPreferences("prefs", 0)
    val lastTs = prefs.getLong("last_background_ts", 0L)

    val wasLanguageChanged = remember {
        context.getSharedPreferences("app_prefs", 0)
            .getBoolean("language_changed", false)
    }

    LaunchedEffect(lastTs, wasLanguageChanged) {
        if (lastTs != 0L && !wasLanguageChanged) {
            val s = SimpleDateFormat.getDateTimeInstance().format(Date(lastTs))
            Toast.makeText(context, "Last backgrounded at $s", Toast.LENGTH_LONG).show()
        }

        if (wasLanguageChanged) {
            context.getSharedPreferences("app_prefs", 0)
                .edit()
                .putBoolean("language_changed", false)
                .apply()
        }
    }

    val textColor = remember(topBarColor) {
        if (topBarColor.red + topBarColor.green + topBarColor.blue > 1.5f) {
            Color.Black
        } else {
            Color.White
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("42-hangouts", color = textColor) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = textColor,
                    actionIconContentColor = textColor
                ),
                actions = {
                    IconButton(onClick = {
                        val current = LocaleHelper.getSavedLanguage(context)
                        val newLang = if (current == "fr") "en" else "fr"
                        LocaleHelper.saveLanguage(context, newLang)
                        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("language_changed", true)
                            .apply()

                        (context as? ComponentActivity)?.recreate()
                    }) {
                        Icon(Icons.Default.Language, contentDescription = "Switch Language")
                    }

                    IconButton(onClick = {
                        topBarColor = if (topBarColor == Color.Red) Color.Blue else Color.Red
                    }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Change Color")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showConversation && !showEdit) {
                FloatingActionButton(onClick = {
                    contactToEdit = null
                    showEdit = true
                }) {
                    Text("+")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(contacts) { contact ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .combinedClickable(
                                onClick = {
                                    selectedContact = contact
                                    showConversation = true
                                },
                                onLongClick = {
                                    contactToEdit = contact
                                    showEdit = true
                                }
                            ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = contact.phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            contact.email?.let { email ->
                                if (email.isNotBlank()) {
                                    Text(
                                        text = "📧 $email",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            contact.address?.let { addr ->
                                if (addr.isNotBlank()) {
                                    Text(
                                        text = "📍 $addr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            contact.notes?.let { note ->
                                if (note.isNotBlank()) {
                                    val preview = if (note.length > 30) "${note.take(30)}…" else note
                                    Text(
                                        text = "📝 $preview",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

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

            if (showConversation && selectedContact != null) {
                ConversationScreen(
                    contactName = selectedContact!!.name,
                    contactId = selectedContact!!.id,
                    repo = messageRepo,
                    onNavigateToContacts = {
                        showConversation = false
                        selectedContact = null
                    }
                )
            }
        }
    }
}