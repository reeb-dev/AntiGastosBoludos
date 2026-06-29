package com.antigastos.boludos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antigastos.boludos.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE personaKey = :key ORDER BY createdAt ASC")
    fun observe(key: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE personaKey = :key ORDER BY createdAt ASC")
    suspend fun list(key: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE personaKey = :key")
    suspend fun clear(key: String)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
