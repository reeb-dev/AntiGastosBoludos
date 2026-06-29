package com.antigastos.boludos.data

import com.antigastos.boludos.data.local.AppDatabase
import com.antigastos.boludos.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Persistencia del chat con cada persona. Es local: lo borrás cuando querés y
 * el historial sirve también de contexto al pedirle a Gemini la próxima respuesta.
 */
class ChatRepository(private val db: AppDatabase) {

    fun observe(personaKey: String): Flow<List<ChatMessageEntity>> =
        db.chatMessageDao().observe(personaKey)

    suspend fun history(personaKey: String): List<ChatMessageEntity> =
        db.chatMessageDao().list(personaKey)

    suspend fun appendUser(personaKey: String, text: String): Long =
        db.chatMessageDao().insert(
            ChatMessageEntity(personaKey = personaKey, role = "user", text = text),
        )

    suspend fun appendPersona(personaKey: String, text: String): Long =
        db.chatMessageDao().insert(
            ChatMessageEntity(personaKey = personaKey, role = "persona", text = text),
        )

    suspend fun clear(personaKey: String) {
        db.chatMessageDao().clear(personaKey)
    }

    /**
     * Borra un mensaje puntual. Lo usamos cuando el usuario pide
     * "regenerar" la última respuesta del personaje.
     */
    suspend fun deleteById(id: Long) {
        db.chatMessageDao().deleteById(id)
    }

    suspend fun clearAll() {
        db.chatMessageDao().clearAll()
    }
}
