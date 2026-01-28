package com.tangem.core.ui.utils

import com.google.common.truth.Truth
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DateUtilsTest {

    private lateinit var defaultTimeZone: DateTimeZone

    private val now = createDateTime(day = 14, hour = 12)

    @BeforeEach
    fun setUp() {
        defaultTimeZone = DateTimeZone.getDefault()
        DateTimeZone.setDefault(DateTimeZone.forID("Europe/Moscow"))
    }

    @AfterEach
    fun tearDown() {
        DateTimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `should return MinutesAgo when difference is less than 1 minute`() {
        val pastDate = now.minusSeconds(30)
        val createdAt = pastDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.MinutesAgo::class.java)
        Truth.assertThat((result as FormattedDate.MinutesAgo).minutes).isEqualTo(1)
    }

    @Test
    fun `should return MinutesAgo when difference is 1 minute`() {
        val pastDate = now.minusMinutes(1)
        val createdAt = pastDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.MinutesAgo::class.java)
        Truth.assertThat((result as FormattedDate.MinutesAgo).minutes).isEqualTo(1)
    }

    @Test
    fun `should return MinutesAgo when difference is 30 minutes`() {
        val pastDate = now.minusMinutes(30)
        val createdAt = pastDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.MinutesAgo::class.java)
        Truth.assertThat((result as FormattedDate.MinutesAgo).minutes).isEqualTo(30)
    }

    @Test
    fun `should return MinutesAgo when difference is 59 minutes`() {
        val pastDate = now.minusMinutes(59).minusSeconds(59)
        val createdAt = pastDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.MinutesAgo::class.java)
        Truth.assertThat((result as FormattedDate.MinutesAgo).minutes).isEqualTo(59)
    }

    @Test
    fun `should return HoursAgo when difference is exactly 1 hour`() {
        val pastDate = now.minusHours(1)
        val createdAt = pastDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.HoursAgo::class.java)
        Truth.assertThat((result as FormattedDate.HoursAgo).hours).isEqualTo(1)
    }

    @Test
    fun `should return HoursAgo when difference is 11 hours`() {
        val pastDate = now.minusHours(11)
        val createdAt = pastDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.HoursAgo::class.java)
        Truth.assertThat((result as FormattedDate.HoursAgo).hours).isEqualTo(11)
    }

    @Test
    fun `should return Today when difference is exactly 12 hours`() {
        val pastDate = createDateTime(day = 14, hour = 0)
        val createdAt = pastDate.toString()
        val nowInTest = createDateTime(day = 14, hour = 12)
        val result = getFormattedDate(createdAt, nowInTest)
        Truth.assertThat(result).isInstanceOf(FormattedDate.Today::class.java)
        Truth.assertThat((result as FormattedDate.Today).time).isEqualTo("03:00")
    }

    @Test
    fun `should return FullDate when difference is 18 hours and past date is another day`() {
        val pastDate = createDateTime(day = 13, hour = 18)
        val createdAt = pastDate.toString()
        val nowInTest = createDateTime(day = 14, hour = 12)
        val result = getFormattedDate(createdAt, nowInTest)
        Truth.assertThat(result).isInstanceOf(FormattedDate.FullDate::class.java)
    }

    @Test
    fun `should return FullDate when difference is exactly 24 hours`() {
        val pastDate = now.minusDays(1)
        val createdAt = pastDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.FullDate::class.java)
    }

    @Test
    fun `should return FullDate when difference is 2 days`() {
        val pastDate = now.minusDays(2)
        val createdAt = pastDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.FullDate::class.java)
    }

    @Test
    fun `should return FullDate with original string when format has wrong date separator`() {
        val wrongFormat = "2025/10/14T12:00:00.000Z"
        val result = getFormattedDate(wrongFormat, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.FullDate::class.java)
        Truth.assertThat((result as FormattedDate.FullDate).date).isEqualTo(wrongFormat)
    }

    @Test
    fun `should handle future dates correctly`() {
        val futureDate = now.plusHours(1)
        val createdAt = futureDate.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.MinutesAgo::class.java)
        Truth.assertThat((result as FormattedDate.MinutesAgo).minutes).isEqualTo(1)
    }

    @Test
    fun `should handle edge case of exactly 0 milliseconds difference`() {
        val createdAt = now.toString()
        val result = getFormattedDate(createdAt, now)
        Truth.assertThat(result).isInstanceOf(FormattedDate.MinutesAgo::class.java)
        Truth.assertThat((result as FormattedDate.MinutesAgo).minutes).isEqualTo(1)
    }

    private fun createDateTime(day: Int, hour: Int): DateTime {
        return DateTime(
            /* year = */ 2025,
            /* monthOfYear = */ 10,
            /* dayOfMonth = */ day,
            /* hourOfDay = */ hour,
            /* minuteOfHour = */ 0,
            /* secondOfMinute = */ 0,
            /* millisOfSecond = */ 0,
            /* zone = */ DateTimeZone.UTC,
        )
    }
}