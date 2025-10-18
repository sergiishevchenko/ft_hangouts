package com.example.ft_hangouts_42.model

data class Contact(
    var id: Long = 0L,
    var name: String,
    var phone: String,
    var email: String?,
    var address: String?,
    var notes: String?,
    var avatarPath: String? = null
)
