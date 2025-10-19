package com.example.ft_hangouts_42.data

import android.content.Context
import com.example.ft_hangouts_42.data.room.AppDatabase
import com.example.ft_hangouts_42.data.room.ContactEntity

class ContactRepository(context: Context) {

    private val contactDao = AppDatabase.getDatabase(context).contactDao()

    suspend fun add(contact: ContactEntity) {
        contactDao.insert(contact)
    }

    suspend fun update(contact: ContactEntity) {
        contactDao.update(contact)
    }

    suspend fun delete(contact: ContactEntity) {
        contactDao.delete(contact)
    }

    suspend fun getAllContacts(): List<ContactEntity> {
        return contactDao.getAllContacts()
    }

    suspend fun getByPhone(phone: String): ContactEntity? {
        return contactDao.getByPhone(phone)
    }

    suspend fun getById(id: Long): ContactEntity? {
        return contactDao.getById(id)
    }
}
