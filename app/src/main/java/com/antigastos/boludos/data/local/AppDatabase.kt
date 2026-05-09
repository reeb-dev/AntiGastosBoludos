package com.antigastos.boludos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antigastos.boludos.data.local.dao.CategoryDao
import com.antigastos.boludos.data.local.dao.ExpenseDao
import com.antigastos.boludos.data.local.dao.GoalDao
import com.antigastos.boludos.data.local.entity.CategoryEntity
import com.antigastos.boludos.data.local.entity.ExpenseEntity
import com.antigastos.boludos.data.local.entity.GoalEntity

@Database(
    entities = [CategoryEntity::class, ExpenseEntity::class, GoalEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "antigastos.db",
            ).build()
    }
}
