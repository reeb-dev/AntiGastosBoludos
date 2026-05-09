package com.antigastos.boludos

import android.app.Application
import com.antigastos.boludos.data.ExpenseRepository
import com.antigastos.boludos.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AntiGastosApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { ExpenseRepository(database) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            repository.ensureCategoriesSeeded()
        }
    }
}
