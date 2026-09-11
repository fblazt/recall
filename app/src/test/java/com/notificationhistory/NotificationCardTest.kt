package com.notificationhistory

import com.notificationhistory.ui.components.formatNotificationTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationCardTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")
    private var defaultLocale: Locale? = null

    @Before
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        defaultLocale?.let { Locale.setDefault(it) }
    }

    @Test
    fun formatNotificationTime_whenTimestampIsZeroOrNegative_returnsEmptyString() {
        assertEquals("", formatNotificationTime(0L, zoneId))
        assertEquals("", formatNotificationTime(-1L, zoneId))
        assertEquals("", formatNotificationTime(-1000L, zoneId))
        assertEquals("", formatNotificationTime(Long.MIN_VALUE, zoneId))
        // Verify default zoneId overload behaves identically
        assertEquals("", formatNotificationTime(0L))
        assertEquals("", formatNotificationTime(-1L))
    }

    @Test
    fun formatNotificationTime_whenTimestampIsToday_formatsCorrectlyWithoutDatePrefix() {
        val today = LocalDate.now(zoneId)

        // Afternoon timestamp: 2:30 PM
        val afternoon = ZonedDateTime.of(today, LocalTime.of(14, 30), zoneId)
        val afternoonMillis = afternoon.toInstant().toEpochMilli()
        assertEquals("2:30 PM", formatNotificationTime(afternoonMillis, zoneId))

        // Morning timestamp: 9:05 AM
        val morning = ZonedDateTime.of(today, LocalTime.of(9, 5), zoneId)
        val morningMillis = morning.toInstant().toEpochMilli()
        assertEquals("9:05 AM", formatNotificationTime(morningMillis, zoneId))

        // Noon timestamp: 12:00 PM
        val noon = ZonedDateTime.of(today, LocalTime.of(12, 0), zoneId)
        val noonMillis = noon.toInstant().toEpochMilli()
        assertEquals("12:00 PM", formatNotificationTime(noonMillis, zoneId))

        // Midnight timestamp: 12:00 AM
        val midnight = ZonedDateTime.of(today, LocalTime.of(0, 0), zoneId)
        val midnightMillis = midnight.toInstant().toEpochMilli()
        assertEquals("12:00 AM", formatNotificationTime(midnightMillis, zoneId))
    }

    @Test
    fun formatNotificationTime_whenTimestampIsYesterday_formatsWithYesterdayPrefix() {
        val today = LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)

        val morningYesterday = ZonedDateTime.of(yesterday, LocalTime.of(10, 42), zoneId)
        val morningMillis = morningYesterday.toInstant().toEpochMilli()
        assertEquals("Yesterday, 10:42 AM", formatNotificationTime(morningMillis, zoneId))

        val eveningYesterday = ZonedDateTime.of(yesterday, LocalTime.of(23, 59), zoneId)
        val eveningMillis = eveningYesterday.toInstant().toEpochMilli()
        assertEquals("Yesterday, 11:59 PM", formatNotificationTime(eveningMillis, zoneId))
    }

    @Test
    fun formatNotificationTime_whenTimestampIsEarlierInSameYear_formatsWithMonthAndDay() {
        val today = LocalDate.now(zoneId)

        // Pick a date in the same year that is neither today nor yesterday
        val earlierDate = if (today.dayOfYear > 2) {
            today.minusDays(2)
        } else {
            today.plusDays(5)
        }

        val zonedDateTime = ZonedDateTime.of(earlierDate, LocalTime.of(15, 45), zoneId)
        val millis = zonedDateTime.toInstant().toEpochMilli()

        val expectedMonthDay = earlierDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        val expected = "$expectedMonthDay, 3:45 PM"

        val result = formatNotificationTime(millis, zoneId)
        assertEquals(expected, result)

        // If today is later than Jan 15 of this year, test an explicit early-year date (Jan 15)
        if (today.isAfter(LocalDate.of(today.year, 1, 15))) {
            val janDate = LocalDate.of(today.year, 1, 15)
            val janZonedDateTime = ZonedDateTime.of(janDate, LocalTime.of(8, 15), zoneId)
            val janMillis = janZonedDateTime.toInstant().toEpochMilli()
            assertEquals("Jan 15, 8:15 AM", formatNotificationTime(janMillis, zoneId))
        }
    }

    @Test
    fun formatNotificationTime_whenTimestampIsInDifferentYear_formatsWithMonthDayAndYear() {
        val today = LocalDate.now(zoneId)

        // Previous year timestamp
        val pastYear = today.year - 1
        val pastDate = LocalDate.of(pastYear, 10, 12)
        val pastZonedDateTime = ZonedDateTime.of(pastDate, LocalTime.of(10, 42), zoneId)
        val pastMillis = pastZonedDateTime.toInstant().toEpochMilli()
        assertEquals("Oct 12, $pastYear, 10:42 AM", formatNotificationTime(pastMillis, zoneId))

        // Fixed older year timestamp (2021)
        val olderDate = LocalDate.of(2021, 3, 5)
        val olderZonedDateTime = ZonedDateTime.of(olderDate, LocalTime.of(14, 20), zoneId)
        val olderMillis = olderZonedDateTime.toInstant().toEpochMilli()
        assertEquals("Mar 5, 2021, 2:20 PM", formatNotificationTime(olderMillis, zoneId))

        // Future year timestamp
        val futureYear = today.year + 1
        val futureDate = LocalDate.of(futureYear, 7, 20)
        val futureZonedDateTime = ZonedDateTime.of(futureDate, LocalTime.of(9, 0), zoneId)
        val futureMillis = futureZonedDateTime.toInstant().toEpochMilli()
        assertEquals("Jul 20, $futureYear, 9:00 AM", formatNotificationTime(futureMillis, zoneId))
    }

    @Test
    fun formatNotificationTime_respectsSpecifiedZoneId() {
        val nyZone = ZoneId.of("America/New_York")
        val todayNy = LocalDate.now(nyZone)
        val zonedDateTimeNy = ZonedDateTime.of(todayNy, LocalTime.of(16, 20), nyZone)
        val millisNy = zonedDateTimeNy.toInstant().toEpochMilli()

        assertEquals("4:20 PM", formatNotificationTime(millisNy, nyZone))
    }
}
