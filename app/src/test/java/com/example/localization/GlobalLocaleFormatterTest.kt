// Unit tests for OfficeZen global currency, date, time, and timezone formatting infrastructure
package com.example.localization

import java.util.Calendar
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalLocaleFormatterTest {

  @Test
  fun globalCurrencies_resolvesKnownPresetsAndJvmFallback() {
    val usd = GlobalCurrencies.findByCode("USD")
    assertEquals("$", usd.symbol)
    assertEquals(2, usd.decimalDigits)

    val inr = GlobalCurrencies.findByCode("inr")
    assertEquals("₹", inr.symbol)

    val jpy = GlobalCurrencies.findByCode("JPY")
    assertEquals("¥", jpy.symbol)
    assertEquals(0, jpy.decimalDigits)

    val chf = GlobalCurrencies.findByCode("CHF")
    assertEquals("CHF", chf.code)
    assertTrue(chf.symbol.isNotBlank())
  }

  @Test
  fun formatCurrency_formatsWholeFractionalZeroDecimalAndNegativeValues() {
    val usdConfig =
      GlobalLocaleConfig(
        currencyCode = "USD",
        currencySymbol = "$",
        currencyDecimals = 2,
      )
    assertEquals("$12,500", GlobalFormatter.formatCurrency(12500.0, usdConfig))
    assertEquals("$12,500.00", GlobalFormatter.formatCurrency(12500.0, usdConfig, showDecimalsWhenWhole = true))
    assertEquals("$1,234.50", GlobalFormatter.formatCurrency(1234.5, usdConfig))
    assertEquals("-$250.75", GlobalFormatter.formatCurrency(-250.75, usdConfig))

    val jpyConfig =
      GlobalLocaleConfig(
        currencyCode = "JPY",
        currencySymbol = "¥",
        currencyDecimals = 0,
      )
    assertEquals("¥15,000", GlobalFormatter.formatCurrency(15000.0, jpyConfig, showDecimalsWhenWhole = true))
  }

  @Test
  fun formatDateAndDateKey_respectPatternAndTimeZone() {
    val epochMs = 1795782600000L // 2026-11-27 12:30:00 UTC
    val isoUtc =
      GlobalLocaleConfig(
        dateFormatPattern = DateFormatPattern.ISO_YYYY_MM_DD,
        timeZoneId = "UTC",
      )
    assertEquals("2026-11-27", GlobalFormatter.formatDate(epochMs, isoUtc, Locale.US))
    assertEquals("2026-11-27", GlobalFormatter.currentDateKey("UTC", epochMs))
    assertEquals("2026-11", GlobalFormatter.currentMonthKey("UTC", epochMs))

    val mdyUs =
      GlobalLocaleConfig(
        dateFormatPattern = DateFormatPattern.MDY_SLASH,
        timeZoneId = "America/New_York",
      )
    assertEquals("11/27/2026", GlobalFormatter.formatDateKey("2026-11-27", mdyUs, Locale.US))

    val dmyIst =
      GlobalLocaleConfig(
        dateFormatPattern = DateFormatPattern.DMY_SLASH,
        timeZoneId = "Asia/Kolkata",
      )
    assertEquals("27/11/2026", GlobalFormatter.formatDateKey("2026-11-27", dmyIst, Locale.US))
  }

  @Test
  fun formatTime_handles12HourAnd24HourAcrossTimeZones() {
    val utcMidnightPlusHalfHour = 1795739400000L // 2026-11-27 00:30:00 UTC? Let's verify exact UTC offset-independent
    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"), Locale.US).apply {
      set(2026, Calendar.NOVEMBER, 27, 14, 45, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val epochMs = cal.timeInMillis

    val utc24 = GlobalLocaleConfig(use24HourClock = true, timeZoneId = "UTC")
    assertEquals("14:45", GlobalFormatter.formatTime(epochMs, utc24, Locale.US))

    val utc12 = GlobalLocaleConfig(use24HourClock = false, timeZoneId = "UTC")
    assertEquals("02:45 PM", GlobalFormatter.formatTime(epochMs, utc12, Locale.US))

    val ist24 = GlobalLocaleConfig(use24HourClock = true, timeZoneId = "Asia/Kolkata")
    assertEquals("20:15", GlobalFormatter.formatTime(epochMs, ist24, Locale.US))

    assertEquals("—", GlobalFormatter.formatTime(0L, utc24, Locale.US))
  }
}
