package com.example.ft_hangouts_42.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import com.example.ft_hangouts_42.utils.colorFromArgbInt
import com.example.ft_hangouts_42.utils.toArgbInt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var contactRepo: ContactRepository
    private lateinit var messageRepo: MessageRepository

    private var lastBackgroundTime = 0L
    private var isFirstResume = true

    private lateinit var callPermissionLauncher: ActivityResultLauncher<String>

    @SuppressLint("UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSmsPermission()
        contactRepo = ContactRepository(this)
        messageRepo = MessageRepository(this)

        callPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.call_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

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

            val makePhoneCall: (String) -> Unit = { phoneNumber ->
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "No app to handle call intent", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
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
                            recreate()
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
                        onSelectedImageUriChanged = { selectedImageUri = it },
                        onMakePhoneCall = makePhoneCall
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
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("UseKtx")
    override fun onPause() {
        super.onPause()
        lastBackgroundTime = System.currentTimeMillis()
        getSharedPreferences("prefs", MODE_PRIVATE)
            .edit()
            .putLong("last_background_ts", lastBackgroundTime)
            .apply()
    }

    @SuppressLint("UseKtx")
    override fun onResume() {
        super.onResume()

        if (!isFirstResume && lastBackgroundTime != 0L) {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val wasLanguageChanged = prefs.getBoolean("language_changed", false)

            if (!wasLanguageChanged) {
                val s = SimpleDateFormat.getDateTimeInstance().format(Date(lastBackgroundTime))
                Toast.makeText(this, "Last backgrounded at $s", Toast.LENGTH_LONG).show()
            }

            prefs.edit().putBoolean("language_changed", false).apply()
        }

        isFirstResume = false
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
    onSelectedImageUriChanged: (Uri?) -> Unit,
    onMakePhoneCall: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current

    var contacts by remember { mutableStateOf(listOf<ContactEntity>()) }
    var showEdit by rememberSaveable { mutableStateOf(false) }
    var contactToEdit by rememberSaveable { mutableStateOf<ContactEntity?>(null) }
    var showConversation by rememberSaveable { mutableStateOf(false) }
    var selectedContact by rememberSaveable { mutableStateOf<ContactEntity?>(null) }
    var expandedContactId by remember { mutableStateOf<Long?>(null) }

    val currentContactForMenu = remember(contacts, expandedContactId) {
        val foundContact = contacts.find { it.id == expandedContactId }
        if (expandedContactId != null && foundContact == null) {
            expandedContactId = null
        }
        foundContact
    }

    var cardCoordinates by remember { mutableStateOf<Offset?>(null) }
    var cardSize by remember { mutableStateOf<IntSize?>(null) }

    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val defaultColor = Color(0xFF6200EE)
    val savedColorInt = prefs.getInt("top_bar_color", defaultColor.toArgbInt())
    var topBarColor by remember { mutableStateOf(colorFromArgbInt(savedColorInt)) }

    LaunchedEffect(topBarColor) {
        prefs.edit().putInt("top_bar_color", topBarColor.toArgbInt()).apply()
    }

    LaunchedEffect(Unit) {
        contacts = contactRepo.getAllContacts()
        while (true) {
            kotlinx.coroutines.delay(2000)
            contacts = contactRepo.getAllContacts()
        }
    }

    val textColor =
        if (topBarColor.red + topBarColor.green + topBarColor.blue > 1.5f) Color.Black else Color.White
    val currentLang = LocaleHelper.getSavedLanguage(context)
    val displayLang = if (currentLang == "fr") "FR" else "EN"

    LaunchedEffect(currentLang) {
        expandedContactId = null
    }

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
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
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
                            .pointerInput(contact.id) {
                                detectTapGestures(onLongPress = {
                                    expandedContactId = contact.id
                                }, onTap = { })
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
                                    .padding(
                                        start = 20.dp,
                                        top = 18.dp,
                                        end = 16.dp,
                                        bottom = 16.dp
                                    ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                            tint = Color(0xFF8E24AA)
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

            currentContactForMenu?.let { contact ->
                val currentLang = LocaleHelper.getSavedLanguage(context)
                val localizedContext = remember(currentLang) {
                    LocaleHelper.setLocale(context, currentLang)
                }

                val menuOffset by remember(cardCoordinates, cardSize, density) {
                    derivedStateOf {
                        if (cardCoordinates != null && cardSize != null) {
                            val menuWidthPx = with(density) { 200.dp.toPx() }

                            val cardCenterX = cardCoordinates!!.x + (cardSize!!.width / 2.0)
                            val menuX = cardCenterX - (menuWidthPx / 2.0)

                            val cardCenterY = cardCoordinates!!.y + (cardSize!!.height / 2.0)
                            val estimatedMenuHeightPx = with(density) { 100.dp.toPx() }
                            val menuY = cardCenterY - (estimatedMenuHeightPx / 2.0)

                            DpOffset(
                                with(density) { menuX.toFloat().toDp() },
                                with(density) { menuY.toFloat().toDp() }
                            )
                        } else DpOffset.Zero
                    }
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
                    val callText = localizedContext.getString(R.string.call)
                    val sendMessageText = localizedContext.getString(R.string.send_message)
                    val editText = localizedContext.getString(R.string.edit)

                    DropdownMenuItem(
                        onClick = {
                            onMakePhoneCall(contact.phone)
                            expandedContactId = null
                        },
                        text = {
                            Text(
                                callText,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = callText,
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    )

                    DropdownMenuItem(
                        onClick = {
                            selectedContact = contact
                            showConversation = true
                            expandedContactId = null
                        },
                        text = {
                            Text(
                                sendMessageText,
                                color = Color(0xFF1E88E5),
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = sendMessageText,
                                tint = Color(0xFF1E88E5)
                            )
                        }
                    )

                    DropdownMenuItem(
                        onClick = {
                            contactToEdit = contact
                            showEdit = true
                            expandedContactId = null
                        },
                        text = {
                            Text(
                                editText,
                                color = Color(0xFF8E24AA),
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = editText,
                                tint = Color(0xFF8E24AA)
                            )
                        }
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