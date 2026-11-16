// OfficeZen DataStore preferences
package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "officezen_prefs")

class AppPreferences(private val context: Context) {
  val languageCodeFlow: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[KEY_LANGUAGE_CODE] ?: "en" }

  val remindersEnabledFlow: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[KEY_REMINDERS_ENABLED] ?: true }

  val lastNotifiedMonthFlow: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[KEY_LAST_NOTIFIED_MONTH] ?: "" }

  suspend fun setLanguageCode(code: String) {
    context.dataStore.edit { prefs -> prefs[KEY_LANGUAGE_CODE] = code }
  }

  suspend fun setRemindersEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_REMINDERS_ENABLED] = enabled }
  }

  suspend fun setLastNotifiedMonth(monthKey: String) {
    context.dataStore.edit { prefs -> prefs[KEY_LAST_NOTIFIED_MONTH] = monthKey }
  }

  private companion object {
    val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
    val KEY_REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val KEY_LAST_NOTIFIED_MONTH = stringPreferencesKey("last_notified_month")
  }
}
