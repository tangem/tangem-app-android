package com.tangem.features.tangempay.cashback.impl.model

import android.text.format.DateFormat
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.joda.time.DateTime
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCashbackDateFormatterTest {

    private val defaultLocale = Locale.getDefault()

    private val formatter = TangemPayCashbackDateFormatter()

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.US)
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN year and month WHEN formatMonth THEN full month name`() {
        // Act
        val actual = formatter.formatMonth(year = 2026, month = 6)

        // Assert
        assertThat(actual).isEqualTo("June")
    }

    @Test
    fun `GIVEN date WHEN formatMonthDay THEN month name then day`() {
        // Act
        val actual = formatter.formatMonthDay(DateTime.parse("2026-07-05"))

        // Assert
        assertThat(actual).isEqualTo("July 5")
    }

    @Test
    fun `GIVEN window within one month WHEN formatWindow THEN single month with day range`() {
        // Act
        val actual = formatter.formatWindow(DateTime.parse("2026-07-01"), DateTime.parse("2026-07-05"))

        // Assert
        assertThat(actual).isEqualTo("July 1–5")
    }

    @Test
    fun `GIVEN window spanning two months WHEN formatWindow THEN month name on both sides`() {
        // Act
        val actual = formatter.formatWindow(DateTime.parse("2026-07-30"), DateTime.parse("2026-08-02"))

        // Assert
        assertThat(actual).isEqualTo("July 30 – August 2")
    }

    @Test
    fun `GIVEN date WHEN formatNumericDate THEN numeric day month year`() {
        // Act
        val actual = formatter.formatNumericDate(DateTime.parse("2026-09-26"))

        // Assert
        assertThat(actual).isEqualTo("26.09.2026")
    }
}