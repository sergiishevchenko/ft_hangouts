package com.example.ft_hangouts_42.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "fthangouts.db"
        const val DB_VERSION = 1

        const val T_CONTACTS = "contacts"
        const val C_ID = "_id"
        const val C_NAME = "name"
        const val C_PHONE = "phone"
        const val C_EMAIL = "email"
        const val C_ADDRESS = "address"
        const val C_NOTES = "notes"
        const val C_AVATAR = "avatar"

        const val T_MESSAGES = "messages"
        const val M_ID = "_id"
        const val M_CONTACT_ID = "contact_id"
        const val M_PHONE = "phone"
        const val M_TEXT = "text"
        const val M_TIMESTAMP = "timestamp"
        const val M_INCOMING = "incoming"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createContacts = """
            CREATE TABLE $T_CONTACTS (
                $C_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $C_NAME TEXT NOT NULL,
                $C_PHONE TEXT NOT NULL,
                $C_EMAIL TEXT,
                $C_ADDRESS TEXT,
                $C_NOTES TEXT,
                $C_AVATAR TEXT
            );
        """.trimIndent()

        val createMessages = """
            CREATE TABLE $T_MESSAGES (
                $M_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $M_CONTACT_ID INTEGER,
                $M_PHONE TEXT NOT NULL,
                $M_TEXT TEXT NOT NULL,
                $M_TIMESTAMP INTEGER NOT NULL,
                $M_INCOMING INTEGER NOT NULL,
                FOREIGN KEY($M_CONTACT_ID) REFERENCES $T_CONTACTS($C_ID) ON DELETE SET NULL
            );
        """.trimIndent()

        db.execSQL(createContacts)
        db.execSQL(createMessages)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $T_CONTACTS")
        onCreate(db)
    }
}
