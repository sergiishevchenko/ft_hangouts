package com.example.ft_hangouts_42.data

import android.content.ContentValues
import android.content.Context
import com.example.ft_hangouts_42.model.Contact
import com.example.ft_hangouts_42.model.Message

class ContactRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    fun createContact(contact: Contact): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(DatabaseHelper.C_NAME, contact.name)
            put(DatabaseHelper.C_PHONE, contact.phone)
            put(DatabaseHelper.C_EMAIL, contact.email)
            put(DatabaseHelper.C_ADDRESS, contact.address)
            put(DatabaseHelper.C_NOTES, contact.notes)
            put(DatabaseHelper.C_AVATAR, contact.avatarPath)
        }
        val id = db.insert(DatabaseHelper.T_CONTACTS, null, cv)
        db.close()
        return id
    }

    fun updateContact(contact: Contact): Int {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(DatabaseHelper.C_NAME, contact.name)
            put(DatabaseHelper.C_PHONE, contact.phone)
            put(DatabaseHelper.C_EMAIL, contact.email)
            put(DatabaseHelper.C_ADDRESS, contact.address)
            put(DatabaseHelper.C_NOTES, contact.notes)
            put(DatabaseHelper.C_AVATAR, contact.avatarPath)
        }
        val rows = db.update(
            DatabaseHelper.T_CONTACTS,
            cv,
            "${DatabaseHelper.C_ID}=?",
            arrayOf(contact.id.toString())
        )
        db.close()
        return rows
    }

    fun deleteContact(contactId: Long) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.T_CONTACTS, "${DatabaseHelper.C_ID}=?", arrayOf(contactId.toString()))
        db.close()
    }

    fun getAllContacts(): List<Contact> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.T_CONTACTS, null, null, null, null, null, "${DatabaseHelper.C_NAME} ASC")
        val list = mutableListOf<Contact>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(Contact(
                    id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.C_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_NAME)),
                    phone = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_PHONE)),
                    email = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_EMAIL)),
                    address = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_ADDRESS)),
                    notes = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_NOTES)),
                    avatarPath = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_AVATAR))
                ))
            }
        }
        db.close()
        return list
    }

    fun getContactByPhone(phone: String): Contact? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.T_CONTACTS, null, "${DatabaseHelper.C_PHONE}=?", arrayOf(phone), null, null, null)
        var c: Contact? = null
        cursor.use {
            if (it.moveToFirst()) {
                c = Contact(
                    id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.C_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_NAME)),
                    phone = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_PHONE)),
                    email = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_EMAIL)),
                    address = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_ADDRESS)),
                    notes = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_NOTES)),
                    avatarPath = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.C_AVATAR))
                )
            }
        }
        db.close()
        return c
    }

    fun addMessage(message: Message): Long {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(DatabaseHelper.M_CONTACT_ID, if (message.contactId == 0L) null else message.contactId)
            put(DatabaseHelper.M_PHONE, message.phone)
            put(DatabaseHelper.M_TEXT, message.text)
            put(DatabaseHelper.M_TIMESTAMP, message.timestamp)
            put(DatabaseHelper.M_INCOMING, if (message.isIncoming) 1 else 0)
        }
        val id = db.insert(DatabaseHelper.T_MESSAGES, null, cv)
        db.close()
        return id
    }

    fun getConversationByPhone(phone: String): List<Message> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(DatabaseHelper.T_MESSAGES, null, "${DatabaseHelper.M_PHONE}=?", arrayOf(phone), null, null, "${DatabaseHelper.M_TIMESTAMP} ASC")
        val list = mutableListOf<Message>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(Message(
                    id = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.M_ID)),
                    contactId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.M_CONTACT_ID)),
                    phone = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.M_PHONE)),
                    text = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.M_TEXT)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.M_TIMESTAMP)),
                    isIncoming = it.getInt(it.getColumnIndexOrThrow(DatabaseHelper.M_INCOMING)) == 1
                ))
            }
        }
        db.close()
        return list
    }
}
