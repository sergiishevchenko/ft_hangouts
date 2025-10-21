package com.example.ft_hangouts_42.ui.contact

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.room.ContactEntity

class ContactEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = ContactRepository(this)
        val contact: ContactEntity? = null

        setContent {
            ContactEditScreen(
                repo = repo,
                contact = contact,
                onClose = { finish() }
            )
        }
    }
}
