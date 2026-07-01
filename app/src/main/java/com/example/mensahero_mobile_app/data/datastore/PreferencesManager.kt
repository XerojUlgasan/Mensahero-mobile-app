package com.example.mensahero_mobile_app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mensahero_prefs")

class PreferencesManager(private val context: Context) {
    
    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val CHOSEN_SIM_ID = intPreferencesKey("chosen_sim_id")
        private val AGENT_ACTIVE = booleanPreferencesKey("agent_active")
        private val TOTAL_MESSAGES_FETCHED = intPreferencesKey("total_messages_fetched")
        private val TOTAL_DELIVERED = intPreferencesKey("total_delivered")
        private val TOTAL_FAILED = intPreferencesKey("total_failed")
        private val LAST_ACTIVITY_TIMESTAMP = longPreferencesKey("last_activity_timestamp")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val FCM_TOKEN = stringPreferencesKey("fcm_token")
        private val DEVICE_NAME = stringPreferencesKey("device_name")
    }

    val apiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[API_KEY]
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = key
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }

    val chosenSimId: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[CHOSEN_SIM_ID]
    }

    suspend fun saveChosenSimId(simId: Int) {
        context.dataStore.edit { preferences ->
            preferences[CHOSEN_SIM_ID] = simId
        }
    }

    val agentActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AGENT_ACTIVE] ?: true
    }

    suspend fun setAgentActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AGENT_ACTIVE] = active
        }
    }

    val totalMessagesFetched: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TOTAL_MESSAGES_FETCHED] ?: 0
    }

    val totalDelivered: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TOTAL_DELIVERED] ?: 0
    }

    val totalFailed: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TOTAL_FAILED] ?: 0
    }

    val lastActivityTimestamp: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[LAST_ACTIVITY_TIMESTAMP]
    }

    suspend fun incrementMessagesFetched() {
        context.dataStore.edit { preferences ->
            preferences[TOTAL_MESSAGES_FETCHED] = (preferences[TOTAL_MESSAGES_FETCHED] ?: 0) + 1
        }
    }

    suspend fun incrementDelivered() {
        context.dataStore.edit { preferences ->
            preferences[TOTAL_DELIVERED] = (preferences[TOTAL_DELIVERED] ?: 0) + 1
        }
    }

    suspend fun incrementFailed() {
        context.dataStore.edit { preferences ->
            preferences[TOTAL_FAILED] = (preferences[TOTAL_FAILED] ?: 0) + 1
        }
    }

    suspend fun updateLastActivity() {
        context.dataStore.edit { preferences ->
            preferences[LAST_ACTIVITY_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    val deviceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID]
    }

    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID] = deviceId
        }
    }

    val fcmToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[FCM_TOKEN]
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[FCM_TOKEN] = token
        }
    }

    val deviceName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_NAME]
    }

    suspend fun saveDeviceName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_NAME] = name
        }
    }

    suspend fun clearDeviceRegistration() {
        context.dataStore.edit { preferences ->
            preferences.remove(DEVICE_ID)
            preferences.remove(FCM_TOKEN)
            preferences.remove(DEVICE_NAME)
        }
    }
}
