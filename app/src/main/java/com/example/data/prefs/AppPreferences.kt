// OfficeZen DataStore preferences (local-only app preferences + theme & global regional config)
package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.DateFormatPattern
import com.example.localization.GlobalCurrencies
import com.example.localization.GlobalLocaleConfig
import com.example.localization.ThemeMode
import java.util.TimeZone
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

  val localUserNameFlow: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[KEY_LOCAL_USER_NAME] ?: "Office Owner" }

  val preferInitialsAvatarFlow: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[KEY_PREFER_INITIALS_AVATAR] ?: false }

  val lastSyncEpochMsFlow: Flow<Long> =
    context.dataStore.data.map { prefs -> prefs[KEY_LAST_SYNC_EPOCH_MS] ?: 0L }

  val lastSyncMessageFlow: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[KEY_LAST_SYNC_MESSAGE] ?: "" }

  val themeModeFlow: Flow<ThemeMode> =
    context.dataStore.data.map { prefs -> ThemeMode.fromWire(prefs[KEY_THEME_MODE]) }

  val globalLocaleConfigFlow: Flow<GlobalLocaleConfig> =
    context.dataStore.data.map { prefs ->
      val defaults = GlobalLocaleConfig.defaultFromSystem()
      val currencyCode = prefs[KEY_CURRENCY_CODE]
      val currencySpec =
        if (!currencyCode.isNullOrBlank()) GlobalCurrencies.findByCode(currencyCode)
        else GlobalCurrencies.findByCode(defaults.currencyCode)
      val symbol = prefs[KEY_CURRENCY_SYMBOL]?.takeIf { it.isNotBlank() } ?: currencySpec.symbol
      val datePattern =
        prefs[KEY_DATE_FORMAT]?.let { DateFormatPattern.fromWire(it) } ?: defaults.dateFormatPattern
      val use24h = prefs[KEY_USE_24H_CLOCK] ?: defaults.use24HourClock
      val firstDay = prefs[KEY_WEEK_START_DAY] ?: defaults.firstDayOfWeek
      val tz = prefs[KEY_TIME_ZONE_ID]?.takeIf { it.isNotBlank() } ?: TimeZone.getDefault().id
      GlobalLocaleConfig(
        currencyCode = currencySpec.code,
        currencySymbol = symbol,
        currencyDecimals = currencySpec.decimalDigits,
        dateFormatPattern = datePattern,
        use24HourClock = use24h,
        firstDayOfWeek = firstDay,
        timeZoneId = tz,
      )
    }

  suspend fun setLanguageCode(code: String) {
    context.dataStore.edit { prefs -> prefs[KEY_LANGUAGE_CODE] = code }
  }

  suspend fun setRemindersEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_REMINDERS_ENABLED] = enabled }
  }

  suspend fun setLastNotifiedMonth(monthKey: String) {
    context.dataStore.edit { prefs -> prefs[KEY_LAST_NOTIFIED_MONTH] = monthKey }
  }

  suspend fun setLocalUserName(name: String) {
    context.dataStore.edit { prefs -> prefs[KEY_LOCAL_USER_NAME] = name.trim().ifBlank { "Office Owner" } }
  }

  suspend fun setPreferInitialsAvatar(preferInitials: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_PREFER_INITIALS_AVATAR] = preferInitials }
  }

  suspend fun setLastSyncResult(epochMs: Long, message: String) {
    context.dataStore.edit { prefs ->
      prefs[KEY_LAST_SYNC_EPOCH_MS] = epochMs
      prefs[KEY_LAST_SYNC_MESSAGE] = message.take(240)
    }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.wire }
  }

  suspend fun setGlobalLocaleConfig(config: GlobalLocaleConfig) {
    context.dataStore.edit { prefs ->
      prefs[KEY_CURRENCY_CODE] = config.currencyCode
      prefs[KEY_CURRENCY_SYMBOL] = config.currencySymbol
      prefs[KEY_DATE_FORMAT] = config.dateFormatPattern.wire
      prefs[KEY_USE_24H_CLOCK] = config.use24HourClock
      prefs[KEY_WEEK_START_DAY] = config.firstDayOfWeek
      prefs[KEY_TIME_ZONE_ID] = config.timeZoneId
    }
  }

  private companion object {
    val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
    val KEY_REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val KEY_LAST_NOTIFIED_MONTH = stringPreferencesKey("last_notified_month")
    val KEY_LOCAL_USER_NAME = stringPreferencesKey("local_user_name")
    val KEY_PREFER_INITIALS_AVATAR = booleanPreferencesKey("prefer_initials_avatar")
    val KEY_LAST_SYNC_EPOCH_MS = longPreferencesKey("last_sync_epoch_ms")
    val KEY_LAST_SYNC_MESSAGE = stringPreferencesKey("last_sync_message")
    val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
    val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
    val KEY_DATE_FORMAT = stringPreferencesKey("date_format_pattern")
    val KEY_USE_24H_CLOCK = booleanPreferencesKey("use_24h_clock")
    val KEY_WEEK_START_DAY = intPreferencesKey("week_start_day")
    val KEY_TIME_ZONE_ID = stringPreferencesKey("time_zone_id")
  }
}
