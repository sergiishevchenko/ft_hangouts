package com.example.ft_hangouts_42.ui.contact

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.RepositoryException
import com.example.ft_hangouts_42.data.room.ContactEntity
import com.example.ft_hangouts_42.R
import com.example.ft_hangouts_42.utils.ValidationUtils
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import java.io.File

@Composable
fun ContactEditScreen(
    repo: ContactRepository,
    contact: ContactEntity?,
    onClose: () -> Unit,
    imagePickerLauncher: ActivityResultLauncher<String>,
    permissionLauncher: ActivityResultLauncher<String>,
    selectedImageUri: Uri?,
    onSelectedImageUriChanged: (Uri?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf(contact?.name ?: "") }
    var phone by rememberSaveable { mutableStateOf(contact?.phone ?: "") }
    var email by rememberSaveable { mutableStateOf(contact?.email ?: "") }
    var address by rememberSaveable { mutableStateOf(contact?.address ?: "") }
    var notes by rememberSaveable { mutableStateOf(contact?.notes ?: "") }
    var avatarPath by rememberSaveable { mutableStateOf(contact?.avatarPath) }
    
    var showErrorSnackbar by remember { mutableStateOf<String?>(null) }
    var imageLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedImageUri) {
        selectedImageUri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val outputFile = File(context.filesDir, "avatars")
                outputFile.mkdirs()
                val newFile = File(outputFile, "${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    input.copyTo(newFile.outputStream())
                }
                avatarPath = newFile.absolutePath
                onSelectedImageUriChanged(null)
                imageLoadError = null
            } catch (e: Exception) {
                imageLoadError = "Failed to load image: ${e.message}"
                onSelectedImageUriChanged(null)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(showErrorSnackbar) {
        showErrorSnackbar?.let { error ->
            snackbarHostState.showSnackbar(error)
            showErrorSnackbar = null
        }
    }

    LaunchedEffect(imageLoadError) {
        imageLoadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            imageLoadError = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(2.dp, Color.Gray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarPath != null) {
                            Image(
                                painter = rememberAsyncImagePainter(avatarPath),
                                contentDescription = "Contact Avatar",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = name.take(1).uppercase(),
                                fontSize = 36.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.change_avatar))
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(R.string.phone)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.address)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Button(onClick = {
                        scope.launch {
                            try {
                                val formattedPhone = ValidationUtils.formatPhone(phone)
                                if (contact == null) {
                                    repo.add(
                                        ContactEntity(
                                            name = name.trim(),
                                            phone = formattedPhone,
                                            email = email.trim().ifBlank { null },
                                            address = address.trim().ifBlank { null },
                                            notes = notes.trim().ifBlank { null },
                                            avatarPath = avatarPath
                                        )
                                    )
                                } else {
                                    repo.update(
                                        contact.copy(
                                            name = name.trim(),
                                            phone = formattedPhone,
                                            email = email.trim().ifBlank { null },
                                            address = address.trim().ifBlank { null },
                                            notes = notes.trim().ifBlank { null },
                                            avatarPath = avatarPath
                                        )
                                    )
                                }
                                onClose()
                            } catch (e: RepositoryException) {
                                showErrorSnackbar = "Failed to save contact: ${e.message}"
                            } catch (e: Exception) {
                                showErrorSnackbar = "Unexpected error: ${e.message}"
                            }
                        }
                    }) {
                        Text(stringResource(R.string.save))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (contact != null) {
                        Button(onClick = {
                            scope.launch {
                                try {
                                    repo.delete(contact)
                                    onClose()
                                } catch (e: RepositoryException) {
                                    showErrorSnackbar = "Failed to delete contact: ${e.message}"
                                } catch (e: Exception) {
                                    showErrorSnackbar = "Unexpected error: ${e.message}"
                                }
                            }
                        }) {
                            Text(stringResource(R.string.delete))
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(onClick = onClose) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
            
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}