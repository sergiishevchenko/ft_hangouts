package com.example.ft_hangouts_42.ui.main

import android.Manifest
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
import com.example.ft_hangouts_42.utils.colorFromArgbInt
import com.example.ft_hangouts_42.utils.toArgbInt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var contactRepo: ContactRepository
    private lateinit var messageRepo: MessageRepository

    private lateinit var callPermissionLauncher: ActivityResultLauncher<String>
    
    companion object {
        private const val PREFS_NAME = "prefs"
        private const val APP_PREFS_NAME = "app_prefs"
        private const val KEY_LAST_BACKGROUND_TS = "last_background_ts"
        private const val KEY_WAS_STOPPED = "was_stopped"
        private const val KEY_IS_FIRST_START = "is_first_start"
        private const val KEY_LANGUAGE_CHANGED = "language_changed"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSmsPermission()
        contactRepo = ContactRepository(this)
        messageRepo = MessageRepository(this)

        callPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.call_permission_denied), Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "No app to handle call intent", Toast.LENGTH_SHORT).show()
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
                            getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean(KEY_LANGUAGE_CHANGED, true)
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            val backgroundTime = System.currentTimeMillis()
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_BACKGROUND_TS, backgroundTime)
                .putBoolean(KEY_WAS_STOPPED, true)
                .apply()
        }
    }

    override fun onStart() {
        super.onStart()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appPrefs = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        
        val wasStopped = prefs.getBoolean(KEY_WAS_STOPPED, false)
        val lastBackgroundTime = prefs.getLong(KEY_LAST_BACKGROUND_TS, 0L)
        val wasLanguageChanged = appPrefs.getBoolean(KEY_LANGUAGE_CHANGED, false)
        
        val isFirstStart = !appPrefs.contains(KEY_IS_FIRST_START)
        if (isFirstStart) {
            appPrefs.edit().putBoolean(KEY_IS_FIRST_START, false).apply()
        }

        if (wasStopped && !isFirstStart && lastBackgroundTime != 0L && !wasLanguageChanged) {
            val s = SimpleDateFormat.getDateTimeInstance().format(Date(lastBackgroundTime))
            Toast.makeText(this, "Last backgrounded at $s", Toast.LENGTH_LONG).show()
            
            prefs.edit().putBoolean(KEY_WAS_STOPPED, false).apply()
        }
        
        if (wasLanguageChanged) {
            appPrefs.edit().putBoolean(KEY_LANGUAGE_CHANGED, false).apply()
            prefs.edit().putBoolean(KEY_WAS_STOPPED, false).apply()
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
                items(contacts, key = { it.id }) { contact ->
                    ElevatedCard(
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
                                detectTapGestures(
                                    onLongPress = { expandedContactId = contact.id },
                                    onTap = { }
                                )
                            },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (contact.avatarPath != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(contact.avatarPath),
                                    contentDescription = "Avatar of ${contact.name}",
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Text(contact.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                contact.email?.takeIf { it.isNotBlank() }?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(Modifier.width(6.dp))

                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                contact.address?.takeIf { it.isNotBlank() }?.let {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(Modifier.width(6.dp))

                                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                contact.notes?.takeIf { it.isNotBlank() }?.let {
                                    val preview = if (it.length > 40) "${it.take(40)}…" else it
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Note,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )

                                        Spacer(Modifier.width(6.dp))

                                        Text(preview, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            IconButton(onClick = { expandedContactId = contact.id }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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