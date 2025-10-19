package com.example.ft_hangouts_42.data.room

import androidx.room.*

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    suspend fun getMessagesForContact(contactId: Long): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)
}
