package com.example.ft_hangouts_42.data

import android.content.Context
import com.example.ft_hangouts_42.data.room.AppDatabase
import com.example.ft_hangouts_42.data.room.MessageEntity

class MessageRepository(context: Context) {

    private val messageDao = AppDatabase.getDatabase(context).messageDao()

    suspend fun addMessage(message: MessageEntity) {
        messageDao.insert(message)
    }

    suspend fun getMessagesForContact(contactId: Long): List<MessageEntity> {
        return messageDao.getMessagesForContact(contactId)
    }
}
