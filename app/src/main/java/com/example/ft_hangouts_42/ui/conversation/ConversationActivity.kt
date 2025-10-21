package com.example.ft_hangouts_42.ui.conversation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ft_hangouts_42.data.MessageRepository

class ConversationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contactName = intent.getStringExtra("contactName") ?: ""
        val contactId = intent.getLongExtra("contactId", -1)
        val repo = MessageRepository(this)

        setContent {
            ConversationScreen(
                contactName = contactName,
                contactId = contactId,
                repo = repo,
                onNavigateToContacts = { finish() }
            )
        }
    }
}