package com.example.ft_hangouts_42.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String?): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ContactEntity)

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)
}
