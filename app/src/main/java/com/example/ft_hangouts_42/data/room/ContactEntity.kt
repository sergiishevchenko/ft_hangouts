package com.example.ft_hangouts_42.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null
)
