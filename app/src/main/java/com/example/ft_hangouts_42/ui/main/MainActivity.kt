package com.example.ft_hangouts_42.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.ft_hangouts_42.R
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.MessageRepository
import com.example.ft_hangouts_42.data.room.ContactEntity
import com.example.ft_hangouts_42.ui.contact.ContactEditScreen
import com.example.ft_hangouts_42.ui.conversation.ConversationScreen
import com.example.ft_hangouts_42.utils.LocaleHelper
import com.example.ft_hangouts_42.utils.collectIsHoveredAsState
import com.example.ft_hangouts_42.utils.colorFromArgbInt
import com.example.ft_hangouts_42.utils.toArgbInt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var contactRepo: ContactRepository
    private lateinit var messageRepo: MessageRepository

    private var wasInBackground = false
    private var lastBackgroundTime = 0L

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

            var pickedImageUri by remember { mutableStateOf<Uri?>(null) }

            val imagePickerLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    pickedImageUri = uri
                }

            val permissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        imagePickerLauncher.launch("image/*")
                    }
                }

            var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

            LaunchedEffect(pickedImageUri) {
                if (pickedImageUri != null) {
                    selectedImageUri = pickedImageUri
                    pickedImageUri = null
                }
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
                        },
                        onShowToastRequested = { timestamp ->
                            if (timestamp != 0L) {
                                val s =
                                    SimpleDateFormat.getDateTimeInstance().format(Date(timestamp))
                                Toast.makeText(this, "Last backgrounded at $s", Toast.LENGTH_LONG)
                                    .show()
                            }
                        },
                        imagePickerLauncher = imagePickerLauncher,
                        permissionLauncher = permissionLauncher,
                        selectedImageUri = selectedImageUri,
                        onSelectedImageUriChanged = { selectedImageUri = it }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    contactRepo: ContactRepository,
    messageRepo: MessageRepository,
    onLanguageChange: (String) -> Unit,
    onShowToastRequested: (Long) -> Unit,
    imagePickerLauncher: ActivityResultLauncher<String>,
    permissionLauncher: ActivityResultLauncher<String>,
    selectedImageUri: Uri?,
    onSelectedImageUriChanged: (Uri?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current

    var contacts by rememberSaveable { mutableStateOf(listOf<ContactEntity>()) }
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var contactToEdit by rememberSaveable { mutableStateOf<ContactEntity?>(null) }
    var showConversation by rememberSaveable { mutableStateOf(false) }
    var selectedContact by rememberSaveable { mutableStateOf<ContactEntity?>(null) }

    var expandedContactId by remember { mutableStateOf<Long?>(null) }
    val currentContactForMenu = contacts.find { it.id == expandedContactId }

    var cardCoordinates by remember { mutableStateOf<Offset?>(null) }
    var cardSize by remember { mutableStateOf<IntSize?>(null) }

    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val defaultColor = Color(0xFF6200EE)
    val savedColorInt = prefs.getInt("top_bar_color", defaultColor.toArgbInt())
    var topBarColor by remember { mutableStateOf(colorFromArgbInt(savedColorInt)) }

    LaunchedEffect(topBarColor) {
        prefs.edit().putInt("top_bar_color", topBarColor.toArgbInt()).apply()
    }

    LaunchedEffect(Unit) { contacts = contactRepo.getAllContacts() }

    val textColor =
        if (topBarColor.red + topBarColor.green + topBarColor.blue > 1.5f) Color.Black else Color.White
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
                            Text(
                                displayLang,
                                color = textColor,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    IconButton(onClick = {
                        topBarColor =
                            if (topBarColor == Color.Red) Color(0xFF3F51B5) else Color.Red
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
                            .onGloballyPositioned { layoutCoordinates ->
                                if (expandedContactId == contact.id) {
                                    cardCoordinates = layoutCoordinates.positionInWindow()
                                    cardSize = layoutCoordinates.size
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { expandedContactId = contact.id },
                                    onTap = {}
                                )
                            },
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (contact.avatarPath != null) {
                                        Image(
                                            painter = rememberAsyncImagePainter(contact.avatarPath),
                                            contentDescription = "Contact Avatar",
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.LightGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = contact.name.take(1).uppercase(),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                    )
                                }

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
                                    val preview =
                                        if (it.length > 30) "${it.take(30)}…" else it
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

            currentContactForMenu?.let { contact ->
                val menuOffset = remember(cardCoordinates, cardSize) {
                    if (cardCoordinates != null && cardSize != null) {
                        val x = with(density) { cardCoordinates!!.x.toDp() + cardSize!!.width.toDp() - 200.dp }
                        val y = with(density) { cardCoordinates!!.y.toDp() + 10.dp }
                        DpOffset(x, y)
                    } else DpOffset.Zero
                }

                DropdownMenu(
                    expanded = expandedContactId != null,
                    onDismissRequest = { expandedContactId = null },
                    modifier = Modifier.width(200.dp),
                    offset = menuOffset,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val emailInteraction = remember { MutableInteractionSource() }
                    val emailHovered by emailInteraction.collectIsHoveredAsState()
                    DropdownMenuItem(
                        onClick = {
                            selectedContact = contact
                            showConversation = true
                            expandedContactId = null
                        },
                        text = {
                            Text(
                                stringResource(R.string.send_message),
                                color = if (emailHovered) Color.White else Color(0xFF1E88E5),
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                tint = if (emailHovered) Color.White else Color(0xFF1E88E5)
                            )
                        },
                        modifier = Modifier
                            .background(if (emailHovered) Color(0xFF1E88E5) else Color.Transparent)
                            .hoverable(emailInteraction)
                    )

                    val editInteraction = remember { MutableInteractionSource() }
                    val editHovered by editInteraction.collectIsHoveredAsState()
                    DropdownMenuItem(
                        onClick = {
                            contactToEdit = contact
                            showEdit = true
                            expandedContactId = null
                        },
                        text = {
                            Text(
                                stringResource(R.string.edit),
                                color = if (editHovered) Color.White else Color(0xFF8E24AA),
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (editHovered) Color.White else Color(0xFF8E24AA)
                            )
                        },
                        modifier = Modifier
                            .background(if (editHovered) Color(0xFF8E24AA) else Color.Transparent)
                            .hoverable(editInteraction)
                    )
                }
            }

            if (showEdit) {
                ContactEditScreen(
                    repo = contactRepo,
                    contact = contactToEdit,
                    onClose = {
                        scope.launch { contacts = contactRepo.getAllContacts() }
                        showEdit = false
                    },
                    imagePickerLauncher = imagePickerLauncher,
                    permissionLauncher = permissionLauncher,
                    selectedImageUri = selectedImageUri,
                    onSelectedImageUriChanged = onSelectedImageUriChanged
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
