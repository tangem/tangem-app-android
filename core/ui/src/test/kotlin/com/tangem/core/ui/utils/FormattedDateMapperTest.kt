package com.tangem.core.ui.utils

import android.text.format.DateFormat
import com.google.common.truth.Truth
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.utils.StringsSigns
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormatterBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FormattedDateMapperTest {

    private lateinit var defaultTimeZone: DateTimeZone

    private val now = createDateTime(day = 14, hour = 12)

    @BeforeEach
    fun setUp() {
        defaultTimeZone = DateTimeZone.getDefault()
        DateTimeZone.setDefault(DateTimeZone.forID("Europe/Moscow"))

        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }

        mockkObject(DateTimeFormatters)
        every { DateTimeFormatters.timeFormatter } returns DateTimeFormatterBuilder()
            .appendHourOfDay(2)
            .appendLiteral(':')
            .appendMinuteOfHour(2)
            .toFormatter()
    }

    @AfterEach
    fun tearDown() {
        DateTimeZone.setDefault(defaultTimeZone)
        unmockkObject(DateTimeFormatters)
        unmockkStatic(DateFormat::class)
    }

    // region String overload

    @Test
    fun `GIVEN iso string less than a minute ago WHEN mapFormattedDate THEN return minutes PluralRes with count 1`() {
        val createdAt = now.minusSeconds(30).toString()

        val result = mapFormattedDate(createdAt = createdAt, now = now)

        Truth.assertThat(result).isEqualTo(
            TextReference.PluralRes(
                id = R.plurals.common_minutes_time_ago,
                count = 1,
                formatArgs = wrappedList(1),
            ),
        )
    }

    @Test
    fun `GIVEN iso string 30 minutes ago WHEN mapFormattedDate THEN return minutes PluralRes with count 30`() {
        val createdAt = now.minusMinutes(30).toString()

        val result = mapFormattedDate(createdAt = createdAt, now = now)

        Truth.assertThat(result).isEqualTo(
            TextReference.PluralRes(
                id = R.plurals.common_minutes_time_ago,
                count = 30,
                formatArgs = wrappedList(30),
            ),
        )
    }

    @Test
    fun `GIVEN iso string 3 hours ago today WHEN mapFormattedDate THEN return hours PluralRes with count 3`() {
        val createdAt = now.minusHours(3).toString()

        val result = mapFormattedDate(createdAt = createdAt, now = now)

        Truth.assertThat(result).isEqualTo(
            TextReference.PluralRes(
                id = R.plurals.common_hours_time_ago,
                count = 3,
                formatArgs = wrappedList(3),
            ),
        )
    }

    @Test
    fun `GIVEN iso string today but past 12 hours WHEN mapFormattedDate THEN return Combined with today and local time`() {
        val pastDate = createDateTime(day = 14, hour = 0)
        val createdAt = pastDate.toString()
        val nowInTest = createDateTime(day = 14, hour = 12)

        val result = mapFormattedDate(createdAt = createdAt, now = nowInTest)

        Truth.assertThat(result).isEqualTo(
            TextReference.Combined(
                refs = WrappedList(
                    data = listOf(
                        TextReference.Res(R.string.common_today),
                        TextReference.Str(StringsSigns.COMA_SIGN),
                        TextReference.Str(StringsSigns.WHITE_SPACE),
                        TextReference.Str("03:00"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN iso string from a previous day WHEN mapFormattedDate THEN return Str FullDate`() {
        val pastDate = createDateTime(day = 10, hour = 9)
        val createdAt = pastDate.toString()

        val result = mapFormattedDate(createdAt = createdAt, now = now)

        Truth.assertThat(result).isInstanceOf(TextReference.Str::class.java)
    }

    @Test
    fun `GIVEN malformed iso string WHEN mapFormattedDate THEN return Str with original value`() {
        val malformed = "2025/10/14T12:00:00.000Z"

        val result = mapFormattedDate(createdAt = malformed, now = now)

        Truth.assertThat(result).isEqualTo(TextReference.Str(value = malformed))
    }

    // endregion

    // region Long overload

    @Test
    fun `GIVEN timestamp less than a minute ago WHEN mapFormattedDate THEN return minutes PluralRes with count 1`() {
        val timestamp = now.minusSeconds(30).millis

        val result = mapFormattedDate(timestamp = timestamp, now = now)

        Truth.assertThat(result).isEqualTo(
            TextReference.PluralRes(
                id = R.plurals.common_minutes_time_ago,
                count = 1,
                formatArgs = wrappedList(1),
            ),
        )
    }

    @Test
    fun `GIVEN timestamp 45 minutes ago WHEN mapFormattedDate THEN return minutes PluralRes with count 45`() {
        val timestamp = now.minusMinutes(45).millis

        val result = mapFormattedDate(timestamp = timestamp, now = now)

        Truth.assertThat(result).isEqualTo(
            TextReference.PluralRes(
                id = R.plurals.common_minutes_time_ago,
                count = 45,
                formatArgs = wrappedList(45),
            ),
        )
    }

    @Test
    fun `GIVEN timestamp 5 hours ago today WHEN mapFormattedDate THEN return hours PluralRes with count 5`() {
        val timestamp = now.minusHours(5).millis

        val result = mapFormattedDate(timestamp = timestamp, now = now)

        Truth.assertThat(result).isEqualTo(
            TextReference.PluralRes(
                id = R.plurals.common_hours_time_ago,
                count = 5,
                formatArgs = wrappedList(5),
            ),
        )
    }

    @Test
    fun `GIVEN timestamp today but past 12 hours WHEN mapFormattedDate THEN return Combined with today and local time`() {
        val pastDate = createDateTime(day = 14, hour = 0)
        val nowInTest = createDateTime(day = 14, hour = 12)

        val result = mapFormattedDate(timestamp = pastDate.millis, now = nowInTest)

        Truth.assertThat(result).isEqualTo(
            TextReference.Combined(
                refs = WrappedList(
                    data = listOf(
                        TextReference.Res(R.string.common_today),
                        TextReference.Str(StringsSigns.COMA_SIGN),
                        TextReference.Str(StringsSigns.WHITE_SPACE),
                        TextReference.Str("03:00"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN timestamp from a previous day WHEN mapFormattedDate THEN return Str FullDate`() {
        val pastDate = createDateTime(day = 10, hour = 9)

        val result = mapFormattedDate(timestamp = pastDate.millis, now = now)

        Truth.assertThat(result).isInstanceOf(TextReference.Str::class.java)
    }

    // endregion

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