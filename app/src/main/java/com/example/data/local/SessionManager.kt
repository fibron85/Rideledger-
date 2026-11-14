package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "ride_ledger_session")

class SessionManager(private val context: Context) {

    private val activeDriverIdKey = stringPreferencesKey("active_driver_id")

    val activeDriverId: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[activeDriverIdKey]
    }

    suspend fun setActiveDriverId(driverId: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[activeDriverIdKey] = driverId
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(activeDriverIdKey)
        }
    }
}
