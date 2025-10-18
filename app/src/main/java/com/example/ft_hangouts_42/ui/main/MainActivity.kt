package com.example.ft_hangouts_42.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ft_hangouts_42.model.Contact

class MainActivity : ComponentActivity() {

    private val contacts = listOf(
        Contact(id = 1, name = "Alice", phone = "123"),
        Contact(id = 2, name = "Bob", phone = "456")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ContactListScreen(contacts)
            }
        }
    }
}

@Composable
fun ContactListScreen(contacts: List<Contact>) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Add contact */ }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(contacts) { contact ->
                Text("${contact.name} - ${contact.phone}", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
