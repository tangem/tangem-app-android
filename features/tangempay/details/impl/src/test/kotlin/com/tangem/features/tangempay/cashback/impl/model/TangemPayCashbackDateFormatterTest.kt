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
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers {
            val skeleton = secondArg<String>()
            if (skeleton == "d MMMM") "MMMM d" else skeleton
        }
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
    fun `GIVEN year and month WHEN formatShortMonth THEN standalone short month name`() {
        // Act
        val actual = formatter.formatShortMonth(year = 2026, month = 6)

        // Assert
        assertThat(actual).isEqualTo("Jun")
    }

    @Test
    fun `GIVEN russian locale WHEN formatShortMonth THEN nominative month name`() {
        // Arrange
        Locale.setDefault(Locale("ru"))

        // Act
        val actual = formatter.formatShortMonth(year = 2026, month = 5)

        // Assert
        assertThat(actual).isEqualTo("май")
    }

    @Test
    fun `GIVEN date WHEN formatMonthDay THEN month name then day`() {
        // Act
        val actual = formatter.formatMonthDay(DateTime.parse("2026-07-05"))

        // Assert
        assertThat(actual).isEqualTo("July 5")
    }

    @Test
    fun `GIVEN date WHEN formatNumericDate THEN numeric day month year`() {
        // Act
        val actual = formatter.formatNumericDate(DateTime.parse("2026-09-26"))

        // Assert
        assertThat(actual).isEqualTo("26.09.2026")
    }

    @Test
    fun `GIVEN locale with slash date pattern WHEN formatNumericDate THEN dot-separated date`() {
        // Arrange
        every { DateFormat.getBestDateTimePattern(any(), "dd.MM.yyyy") } returns "MM/dd/yyyy"

        // Act
        val actual = formatter.formatNumericDate(DateTime.parse("2026-10-19"))

        // Assert
        assertThat(actual).isEqualTo("19.10.2026")
    }
}