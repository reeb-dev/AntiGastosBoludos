package com.antigastos.boludos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antigastos.boludos.data.local.dao.AchievementDao
import com.antigastos.boludos.data.local.dao.BudgetDao
import com.antigastos.boludos.data.local.dao.CategoryDao
import com.antigastos.boludos.data.local.dao.ChatMessageDao
import com.antigastos.boludos.data.local.dao.CopyVoteDao
import com.antigastos.boludos.data.local.dao.ExpenseDao
import com.antigastos.boludos.data.local.dao.GoalDao
import com.antigastos.boludos.data.local.dao.PactDao
import com.antigastos.boludos.data.local.dao.PendingCrushDao
import com.antigastos.boludos.data.local.dao.ScoreDao
import com.antigastos.boludos.data.local.dao.ScoreEventDao
import com.antigastos.boludos.data.local.dao.SettingsDao
import com.antigastos.boludos.data.local.dao.SubscriptionDao
import com.antigastos.boludos.data.local.entity.AchievementEntity
import com.antigastos.boludos.data.local.entity.BudgetEntity
import com.antigastos.boludos.data.local.entity.CategoryEntity
import com.antigastos.boludos.data.local.entity.ChatMessageEntity
import com.antigastos.boludos.data.local.entity.CopyVoteEntity
import com.antigastos.boludos.data.local.entity.ExpenseEntity
import com.antigastos.boludos.data.local.entity.GoalEntity
import com.antigastos.boludos.data.local.entity.PactEntity
import com.antigastos.boludos.data.local.entity.PendingCrushEntity
import com.antigastos.boludos.data.local.entity.ScoreEntity
import com.antigastos.boludos.data.local.entity.ScoreEventEntity
import com.antigastos.boludos.data.local.entity.SettingsEntity
import com.antigastos.boludos.data.local.entity.SubscriptionEntity

@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        GoalEntity::class,
        CopyVoteEntity::class,
        BudgetEntity::class,
        SubscriptionEntity::class,
        AchievementEntity::class,
        PactEntity::class,
        SettingsEntity::class,
        ChatMessageEntity::class,
        ScoreEntity::class,
        ScoreEventEntity::class,
        PendingCrushEntity::class,
    ],
    version = 29,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun copyVoteDao(): CopyVoteDao
    abstract fun budgetDao(): BudgetDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun pactDao(): PactDao
    abstract fun settingsDao(): SettingsDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun scoreDao(): ScoreDao
    abstract fun scoreEventDao(): ScoreEventDao
    abstract fun pendingCrushDao(): PendingCrushDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "antigastos.db",
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
