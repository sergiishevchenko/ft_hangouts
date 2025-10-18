package com.example.ft_hangouts_42.model

data class Message(
    var id: Long = 0L,
    var contactId: Long,
    var phone: String,
    var text: String,
    var timestamp: Long,
    var isIncoming: Boolean
)
