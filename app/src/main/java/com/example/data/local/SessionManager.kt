package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "ride_ledger_session")

class SessionManager(private val context: Context) {

    private val activeDriverIdKey = stringPreferencesKey("active_driver_id")
    private val biometricDriverIdKey = stringPreferencesKey("biometric_driver_id")
    private val storageSetupCompletedKey = booleanPreferencesKey("storage_setup_completed")
    private val grantedFolderUriKey = stringPreferencesKey("granted_folder_uri")

    val activeDriverId: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[activeDriverIdKey]
    }

    val biometricDriverId: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[biometricDriverIdKey]
    }

    val storageSetupCompleted: Flow<Boolean> = context.sessionDataStore.data.map { prefs ->
        prefs[storageSetupCompletedKey] ?: false
    }

    val grantedFolderUri: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[grantedFolderUriKey]
    }

    suspend fun setActiveDriverId(driverId: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[activeDriverIdKey] = driverId
        }
    }

    suspend fun setBiometricDriverId(driverId: String?) {
        context.sessionDataStore.edit { prefs ->
            if (driverId.isNullOrBlank()) {
                prefs.remove(biometricDriverIdKey)
            } else {
                prefs[biometricDriverIdKey] = driverId
            }
        }
    }

    suspend fun setStorageSetupCompleted(completed: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[storageSetupCompletedKey] = completed
        }
    }

    suspend fun setGrantedFolderUri(uriString: String?) {
        context.sessionDataStore.edit { prefs ->
            if (uriString.isNullOrBlank()) {
                prefs.remove(grantedFolderUriKey)
            } else {
                prefs[grantedFolderUriKey] = uriString
            }
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            // Clear only the active session so fingerprint sign-in and folder permissions remain enrolled after logout.
            prefs.remove(activeDriverIdKey)
        }
    }
}
