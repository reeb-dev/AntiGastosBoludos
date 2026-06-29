package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Mensaje del chat con una persona. role = "user" | "persona".
 * Se guarda histórico para mantener contexto en la conversación.
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val personaKey: String,
    val role: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)
