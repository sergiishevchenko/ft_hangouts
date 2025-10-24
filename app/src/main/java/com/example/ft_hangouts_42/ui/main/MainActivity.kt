package com.example.ft_hangouts_42.ui.main

import com.example.ft_hangouts_42.utils.toArgbInt
import com.example.ft_hangouts_42.utils.colorFromArgbInt
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.MessageRepository
import com.example.ft_hangouts_42.data.room.ContactEntity
import com.example.ft_hangouts_42.ui.contact.ContactEditScreen
import com.example.ft_hangouts_42.ui.conversation.ConversationScreen
import com.example.ft_hangouts_42.utils.LocaleHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.animateContentSize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration

class MainActivity : ComponentActivity() {
    private lateinit var contactRepo: ContactRepository
    private lateinit var messageRepo: MessageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSmsPermission()
        contactRepo = ContactRepository(this)
        messageRepo = MessageRepository(this)

        setContent {
            val savedLang = LocaleHelper.getSavedLanguage(this)
            var currentLang by remember { mutableStateOf(savedLang) }
            val contextWithLocale = remember(currentLang) {
                LocaleHelper.setLocale(this, currentLang)
            }

            CompositionLocalProvider(LocalContext provides contextWithLocale) {
                MaterialTheme {
                    MainScreen(
                        contactRepo = contactRepo,
                        messageRepo = messageRepo,
                        onLanguageChange = { lang ->
                            LocaleHelper.saveLanguage(this, lang)
                            getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("language_changed", true)
                                .apply()
                            currentLang = lang
                        }
                    )
                }
            }
        }
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 100)
        }
    }

    @Suppress("Deprecated")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    contactRepo: ContactRepository,
    messageRepo: MessageRepository,
    onLanguageChange: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    var showEdit by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<ContactEntity?>(null) }
    var showConversation by remember { mutableStateOf(false) }
    var selectedContact by remember { mutableStateOf<ContactEntity?>(null) }

    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val defaultColor = Color(0xFF6200EE)
    val savedColorInt = prefs.getInt("top_bar_color", defaultColor.toArgbInt())
    var topBarColor by remember { mutableStateOf(colorFromArgbInt(savedColorInt)) }

    LaunchedEffect(topBarColor) {
        prefs.edit().putInt("top_bar_color", topBarColor.toArgbInt()).apply()
    }

    LaunchedEffect(Unit) { contacts = contactRepo.getAllContacts() }

    val lastTs = context.getSharedPreferences("prefs", 0).getLong("last_background_ts", 0L)
    val wasLanguageChanged = prefs.getBoolean("language_changed", false)

    var shownOnce by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!shownOnce && lastTs != 0L && !wasLanguageChanged) {
            shownOnce = true
            val s = SimpleDateFormat.getDateTimeInstance().format(Date(lastTs))
            Toast.makeText(context, "Last backgrounded at $s", Toast.LENGTH_LONG).show()
        }
        if (wasLanguageChanged) {
            prefs.edit().putBoolean("language_changed", false).apply()
        }
    }

    val textColor = if (topBarColor.red + topBarColor.green + topBarColor.blue > 1.5f)
        Color.Black else Color.White
    val currentLang = LocaleHelper.getSavedLanguage(context)
    val displayLang = if (currentLang == "fr") "FR" else "EN"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("42-hangouts", color = textColor) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = textColor,
                    actionIconContentColor = textColor
                ),
                actions = {
                    IconButton(onClick = {
                        val newLang = if (currentLang == "fr") "en" else "fr"
                        onLanguageChange(newLang)
                    }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null)
                            Text(displayLang, color = textColor, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = {
                        topBarColor = if (topBarColor == Color.Red) Color(0xFF3F51B5) else Color.Red
                    }) {
                        Icon(Icons.Default.ColorLens, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showConversation && !showEdit) {
                FloatingActionButton(
                    onClick = {
                        contactToEdit = null
                        showEdit = true
                    },
                    containerColor = Color(0xFF7E57C2),
                    contentColor = Color.White
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts) { contact ->
                    val colorScheme = MaterialTheme.colorScheme
                    val backgroundColor = colorScheme.surfaceVariant.copy(alpha = 0.4f)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
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
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = colorScheme.surface.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(start = 20.dp, top = 18.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = rememberVectorPainter(Icons.Default.Phone),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(contact.phone, color = Color(0xFF2E7D32))
                                }

                                contact.email?.takeIf { it.isNotBlank() }?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = rememberVectorPainter(Icons.Default.Email),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF1E88E5)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(it, color = Color(0xFF1565C0))
                                    }
                                }

                                contact.address?.takeIf { it.isNotBlank() }?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = rememberVectorPainter(Icons.Default.LocationOn),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFFF57C00)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(it, color = Color(0xFFEF6C00))
                                    }
                                }

                                contact.notes?.takeIf { it.isNotBlank() }?.let {
                                    val preview = if (it.length > 30) "${it.take(30)}…" else it
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = rememberVectorPainter(Icons.Default.Note),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF8E24AA)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(preview, color = Color(0xFF6A1B9A))
                                    }
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