package com.example.ft_hangouts_42.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: Long,
    val text: String,
    val timestamp: Long,
    val isSent: Boolean
)
