package com.example.ft_hangouts_42.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ft_hangouts_42.data.ContactRepository
import com.example.ft_hangouts_42.data.RepositoryException
import com.example.ft_hangouts_42.data.room.ContactEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val contactRepository: ContactRepository
) : ViewModel() {

    val contacts: StateFlow<List<ContactEntity>> = contactRepository.getAllContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshContacts() {
        viewModelScope.launch {
        }
    }

    fun deleteContact(contact: ContactEntity, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                contactRepository.delete(contact)
            } catch (e: RepositoryException) {
                onError("Failed to delete contact: ${e.message}")
            } catch (e: Exception) {
                onError("Unexpected error: ${e.message}")
            }
        }
    }
}

