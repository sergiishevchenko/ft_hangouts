package com.example.ft_hangouts_42.data

import android.content.Context
import com.example.ft_hangouts_42.data.room.AppDatabase
import com.example.ft_hangouts_42.data.room.MessageEntity
import kotlinx.coroutines.flow.Flow

class MessageRepository(context: Context) {

    private val messageDao = AppDatabase.getDatabase(context).messageDao()

    suspend fun addMessage(message: MessageEntity) {
        try {
            messageDao.insert(message)
        } catch (e: Exception) {
            throw RepositoryException("Failed to add message", e)
        }
    }

    fun getMessagesForContact(contactId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForContact(contactId)
    }
}
