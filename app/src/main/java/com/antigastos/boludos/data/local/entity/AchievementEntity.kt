package com.antigastos.boludos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Logros/trofeos. La key es un id estable (definido en AchievementsCatalog).
 * `unlockedAt` null = bloqueado.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,
    val unlockedAt: Long? = null,
)
