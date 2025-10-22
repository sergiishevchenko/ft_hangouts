package com.example.ft_hangouts_42.ui.contact

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.room.ContactEntity
import com.example.ft_hangouts_42.R
import kotlinx.coroutines.launch

@Composable
fun ContactEditScreen(
    repo: ContactRepository,
    contact: ContactEntity?,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var email by remember { mutableStateOf(contact?.email ?: "") }
    var address by remember { mutableStateOf(contact?.address ?: "") }
    var notes by remember { mutableStateOf(contact?.notes ?: "") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        if (contact == null) {
                            repo.add(
                                ContactEntity(
                                    name = name,
                                    phone = phone,
                                    email = email.ifBlank { null },
                                    address = address.ifBlank { null },
                                    notes = notes.ifBlank { null }
                                )
                            )
                        } else {
                            repo.update(
                                contact.copy(
                                    name = name,
                                    phone = phone,
                                    email = email.ifBlank { null },
                                    address = address.ifBlank { null },
                                    notes = notes.ifBlank { null }
                                )
                            )
                        }
                        onClose()
                    }
                }) {
                    Text(stringResource(R.string.save))
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (contact != null) {
                    Button(onClick = {
                        scope.launch {
                            repo.delete(contact)
                            onClose()
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
    }
}