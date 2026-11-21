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
    private val remindersEnabledKey = booleanPreferencesKey("daily_reminders_enabled")
    private val dismissedReminderSlotKey = stringPreferencesKey("dismissed_reminder_slot_key")
    private val dismissedFinanceReminderSlotKey = stringPreferencesKey("dismissed_finance_reminder_slot_key")

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

    val remindersEnabled: Flow<Boolean> = context.sessionDataStore.data.map { prefs ->
        prefs[remindersEnabledKey] ?: true
    }

    val dismissedReminderSlot: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[dismissedReminderSlotKey]
    }

    val dismissedFinanceReminderSlot: Flow<String?> = context.sessionDataStore.data.map { prefs ->
        prefs[dismissedFinanceReminderSlotKey]
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

    suspend fun setRemindersEnabled(enabled: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[remindersEnabledKey] = enabled
        }
    }

    suspend fun setDismissedReminderSlot(slotKey: String?) {
        context.sessionDataStore.edit { prefs ->
            if (slotKey.isNullOrBlank()) {
                prefs.remove(dismissedReminderSlotKey)
            } else {
                prefs[dismissedReminderSlotKey] = slotKey
            }
        }
    }

    suspend fun setDismissedFinanceReminderSlot(slotKey: String?) {
        context.sessionDataStore.edit { prefs ->
            if (slotKey.isNullOrBlank()) {
                prefs.remove(dismissedFinanceReminderSlotKey)
            } else {
                prefs[dismissedFinanceReminderSlotKey] = slotKey
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
