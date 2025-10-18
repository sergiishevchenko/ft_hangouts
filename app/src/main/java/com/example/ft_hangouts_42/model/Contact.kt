package com.example.ft_hangouts_42.model

data class Contact(
    var id: Long = 0L,
    var name: String,
    var phone: String,
    var email: String? = null,
    var address: String? = null,
    var notes: String? = null,
    var avatarPath: String? = null
)
