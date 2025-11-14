package com.example.ft_hangouts_42.data

import android.content.Context
import com.example.ft_hangouts_42.data.room.AppDatabase
import com.example.ft_hangouts_42.data.room.ContactEntity
import kotlinx.coroutines.flow.Flow

class ContactRepository(context: Context) {

    private val contactDao = AppDatabase.getDatabase(context).contactDao()

    suspend fun add(contact: ContactEntity) {
        try {
            contactDao.insert(contact)
        } catch (e: Exception) {
            throw RepositoryException("Failed to add contact", e)
        }
    }

    suspend fun update(contact: ContactEntity) {
        try {
            contactDao.update(contact)
        } catch (e: Exception) {
            throw RepositoryException("Failed to update contact", e)
        }
    }

    suspend fun delete(contact: ContactEntity) {
        try {
            contactDao.delete(contact)
        } catch (e: Exception) {
            throw RepositoryException("Failed to delete contact", e)
        }
    }

    fun getAllContacts(): Flow<List<ContactEntity>> {
        return contactDao.getAllContacts()
    }

    suspend fun getByPhone(phone: String): ContactEntity? {
        return try {
            contactDao.getByPhone(phone)
        } catch (e: Exception) {
            throw RepositoryException("Failed to get contact by phone", e)
        }
    }

    suspend fun getById(id: Long): ContactEntity? {
        return try {
            contactDao.getById(id)
        } catch (e: Exception) {
            throw RepositoryException("Failed to get contact by id", e)
        }
    }
}

class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
