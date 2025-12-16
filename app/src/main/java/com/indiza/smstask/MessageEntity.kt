package com.indiza.smstask


data class MessageEntity(
    val id: Int,
    val content: String,
    val destinataire: String,
    var sent: Boolean,
    val statut: Int // 0 = pending, 1 = envoyé, 2 = échec
)
