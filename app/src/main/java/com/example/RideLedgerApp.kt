package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SessionManager
import com.example.data.repository.RideLedgerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RideLedgerApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val sessionManager: SessionManager by lazy { SessionManager(this) }
    val repository: RideLedgerRepository by lazy {
        RideLedgerRepository(
            driverDao = database.driverDao(),
            settingsDao = database.settingsDao(),
            rideDao = database.rideDao(),
            sessionManager = sessionManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            repository.ensureDefaultsPopulated()
        }
    }
}
