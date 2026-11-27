// Global currency, date format, clock, week-start, and IANA timezone infrastructure
package com.example.localization

import androidx.compose.runtime.staticCompositionLocalOf
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToLong

enum class ThemeMode(val wire: String, val label: String) {
  SYSTEM("system", "System Default"),
  LIGHT("light", "Light (Warm Alabaster)"),
  DARK("dark", "Dark (Obsidian Emerald)");

  companion object {
    fun fromWire(value: String?): ThemeMode =
      entries.firstOrNull { it.wire.equals(value, ignoreCase = true) } ?: SYSTEM
  }
}

data class CurrencySpec(
  val code: String,
  val symbol: String,
  val displayName: String,
  val decimalDigits: Int = 2,
)

object GlobalCurrencies {
  val presets: List<CurrencySpec> =
    listOf(
      CurrencySpec("USD", "$", "US Dollar", 2),
      CurrencySpec("INR", "₹", "Indian Rupee", 2),
      CurrencySpec("EUR", "€", "Euro", 2),
      CurrencySpec("GBP", "£", "British Pound", 2),
      CurrencySpec("AED", "د.إ", "UAE Dirham", 2),
      CurrencySpec("SGD", "S$", "Singapore Dollar", 2),
      CurrencySpec("CAD", "C$", "Canadian Dollar", 2),
      CurrencySpec("AUD", "A$", "Australian Dollar", 2),
      CurrencySpec("SAR", "﷼", "Saudi Riyal", 2),
      CurrencySpec("JPY", "¥", "Japanese Yen", 0),
    )

  fun findByCode(code: String?): CurrencySpec {
    if (code.isNullOrBlank()) return presets.first()
    presets.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }?.let { return it }
    val normalized = code.trim().uppercase(Locale.US)
    val jvmSymbol =
      runCatching { Currency.getInstance(normalized).symbol }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: normalized
    val decimals =
      runCatching { Currency.getInstance(normalized).defaultFractionDigits.coerceIn(0, 3) }.getOrNull()
        ?: 2
    return CurrencySpec(code = normalized, symbol = jvmSymbol, displayName = normalized, decimalDigits = decimals)
  }
}

enum class DateFormatPattern(val wire: String, val pattern: String, val sampleLabel: String) {
  ISO_YYYY_MM_DD("iso", "yyyy-MM-dd", "2026-11-27"),
  DMY_SLASH("dmy_slash", "dd/MM/yyyy", "27/11/2026"),
  MDY_SLASH("mdy_slash", "MM/dd/yyyy", "11/27/2026"),
  DD_MMM_YYYY("dd_mmm_yyyy", "dd MMM yyyy", "27 Nov 2026"),
  MMM_DD_YYYY("mmm_dd_yyyy", "MMM dd, yyyy", "Nov 27, 2026");

  companion object {
    fun fromWire(value: String?): DateFormatPattern =
      entries.firstOrNull { it.wire.equals(value, ignoreCase = true) } ?: DD_MMM_YYYY
  }
}

data class GlobalLocaleConfig(
  val currencyCode: String = "INR",
  val currencySymbol: String = "₹",
  val currencyDecimals: Int = 2,
  val dateFormatPattern: DateFormatPattern = DateFormatPattern.DD_MMM_YYYY,
  val use24HourClock: Boolean = false,
  val firstDayOfWeek: Int = Calendar.MONDAY,
  val timeZoneId: String = TimeZone.getDefault().id,
) {
  val timeZone: TimeZone
    get() = TimeZone.getTimeZone(timeZoneId.ifBlank { "UTC" })

  companion object {
    fun defaultFromSystem(locale: Locale = Locale.getDefault()): GlobalLocaleConfig {
      val currency = runCatching { Currency.getInstance(locale) }.getOrNull()
      val code = currency?.currencyCode ?: "USD"
      val spec = GlobalCurrencies.findByCode(code)
      val cal = Calendar.getInstance(locale)
      return GlobalLocaleConfig(
        currencyCode = spec.code,
        currencySymbol = spec.symbol,
        currencyDecimals = spec.decimalDigits,
        dateFormatPattern = DateFormatPattern.DD_MMM_YYYY,
        use24HourClock = false,
        firstDayOfWeek = cal.firstDayOfWeek,
        timeZoneId = TimeZone.getDefault().id,
      )
    }

    val commonTimeZones: List<String> =
      listOf(
        "UTC",
        "Asia/Kolkata",
        "America/New_York",
        "America/Los_Angeles",
        "Europe/London",
        "Europe/Berlin",
        "Asia/Dubai",
        "Asia/Singapore",
        "Asia/Tokyo",
        "Australia/Sydney",
      )
  }
}

val LocalGlobalLocaleConfig =
  staticCompositionLocalOf {
    GlobalLocaleConfig.defaultFromSystem()
  }

object GlobalFormatter {

  fun formatCurrency(
    amount: Double,
    config: GlobalLocaleConfig,
    showDecimalsWhenWhole: Boolean = false,
  ): String {
    val safeAmount = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
    val negative = safeAmount < 0.0
    val absVal = abs(safeAmount)
    val decimals =
      if (config.currencyDecimals == 0) {
        0
      } else if (!showDecimalsWhenWhole && abs(absVal - absVal.roundToLong().toDouble()) < 0.005) {
        0
      } else {
        config.currencyDecimals
      }
    val formatter =
      NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
        isGroupingUsed = true
      }
    val formatted = formatter.format(absVal)
    val prefix = if (negative) "-${config.currencySymbol}" else config.currencySymbol
    return "$prefix$formatted"
  }

  fun formatDate(
    epochMs: Long,
    config: GlobalLocaleConfig,
    locale: Locale = Locale.getDefault(),
  ): String {
    if (epochMs <= 0L) return "—"
    val sdf = SimpleDateFormat(config.dateFormatPattern.pattern, locale)
    sdf.timeZone = config.timeZone
    return sdf.format(Date(epochMs))
  }

  fun formatDateKey(
    dateKey: String,
    config: GlobalLocaleConfig,
    locale: Locale = Locale.getDefault(),
  ): String {
    if (dateKey.isBlank()) return "—"
    return runCatching {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = config.timeZone }
      val parsed = parser.parse(dateKey) ?: return@runCatching dateKey
      val out = SimpleDateFormat(config.dateFormatPattern.pattern, locale).apply { timeZone = config.timeZone }
      out.format(parsed)
    }.getOrDefault(dateKey)
  }

  fun formatTime(
    epochMs: Long,
    config: GlobalLocaleConfig,
    locale: Locale = Locale.getDefault(),
  ): String {
    if (epochMs <= 0L) return "—"
    val pattern = if (config.use24HourClock) "HH:mm" else "hh:mm a"
    val sdf = SimpleDateFormat(pattern, locale)
    sdf.timeZone = config.timeZone
    return sdf.format(Date(epochMs))
  }

  fun formatDateTime(
    epochMs: Long,
    config: GlobalLocaleConfig,
    locale: Locale = Locale.getDefault(),
  ): String {
    if (epochMs <= 0L) return "—"
    val timePart = if (config.use24HourClock) "HH:mm" else "hh:mm a"
    val sdf = SimpleDateFormat("${config.dateFormatPattern.pattern}, $timePart", locale)
    sdf.timeZone = config.timeZone
    return sdf.format(Date(epochMs))
  }

  fun currentDateKey(
    timeZoneId: String = TimeZone.getDefault().id,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone(timeZoneId.ifBlank { "UTC" })
    return sdf.format(Date(nowEpochMs))
  }

  fun currentMonthKey(
    timeZoneId: String = TimeZone.getDefault().id,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): String {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone(timeZoneId.ifBlank { "UTC" })
    return sdf.format(Date(nowEpochMs))
  }
}
