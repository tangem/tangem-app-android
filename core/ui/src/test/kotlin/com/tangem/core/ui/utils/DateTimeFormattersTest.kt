package com.tangem.core.ui.utils

import android.text.format.DateFormat
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Locale

/**
 * Unit tests for [DateTimeFormatters], in particular for conversion of ICU date/time patterns
 * to Joda-Time compatible patterns.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DateTimeFormattersTest {

    private val defaultLocale = Locale.getDefault()

    @BeforeEach
    fun setUp() {
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `converts LLLL to MMMM - full standalone month pattern that crashes on Chinese locale`() {
        // Arrange
        val icuPattern = "d LLLL"

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPattern)

        // Assert
        Truth.assertThat(actual).isEqualTo("d MMMM")
    }

    @Test
    fun `converts LLL to MMM - short standalone month`() {
        // Arrange
        val icuPattern = "dd LLL yyyy"

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPattern)

        // Assert
        Truth.assertThat(actual).isEqualTo("dd MMM yyyy")
    }

    @Test
    fun `converts LL to MM - numeric standalone month`() {
        // Arrange
        val icuPattern = "yyyy-MM-LL"

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPattern)

        // Assert
        Truth.assertThat(actual).isEqualTo("yyyy-MM-MM")
    }

    @Test
    fun `converts single L to M`() {
        // Arrange
        val icuPattern = "d/L/yyyy"

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPattern)

        // Assert
        Truth.assertThat(actual).isEqualTo("d/M/yyyy")
    }

    @Test
    fun `leaves pattern without L unchanged`() {
        // Arrange
        val icuPattern = "dd.MM.yyyy HH:mm"

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPattern)

        // Assert
        Truth.assertThat(actual).isEqualTo("dd.MM.yyyy HH:mm")
    }

    @Test
    fun `leaves pattern with M unchanged`() {
        // Arrange
        val icuPattern = "d MMMM yyyy"

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPattern)

        // Assert
        Truth.assertThat(actual).isEqualTo("d MMMM yyyy")
    }

    @Test
    fun `handles mixed ICU pattern as returned for Chinese locale - d MMMM`() {
        // Arrange
        val icuPatternWithStandaloneMonth = "d LLLL"

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPatternWithStandaloneMonth)

        // Assert — Joda-Time can parse and format this without IllegalArgumentException
        Truth.assertThat(actual).isEqualTo("d MMMM")
    }

    @Test
    fun `handles empty string`() {
        // Arrange
        val icuPattern = ""

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPattern)

        // Assert
        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `handles pattern with only literal characters`() {
        // Arrange
        val icuPattern = " 'at' "

        // Act
        val actual = DateTimeFormatters.icuPatternToJodaPattern(icuPattern)

        // Assert
        Truth.assertThat(actual).isEqualTo(" 'at' ")
    }

    @Test
    fun `getBestFormatterBySkeleton with d MMMM skeleton produces formatter that does not throw on format`() {
        // Arrange
        val formatter = DateTimeFormatters.getBestFormatterBySkeleton("d MMMM")
        val date = org.joda.time.DateTime(2025, 2, 13, 12, 0, 0, 0)

        // Act & Assert
        val formatted = formatter.print(date)
        Truth.assertThat(formatted).isNotEmpty()
    }

    @Test
    fun `GIVEN russian locale WHEN formatStandaloneShortMonth THEN nominative month name`() {
        // Arrange
        Locale.setDefault(Locale("ru"))

        // Act
        val april = DateTimeFormatters.formatStandaloneShortMonth(org.joda.time.DateTime(2026, 4, 1, 0, 0))
        val may = DateTimeFormatters.formatStandaloneShortMonth(org.joda.time.DateTime(2026, 5, 1, 0, 0))
        val june = DateTimeFormatters.formatStandaloneShortMonth(org.joda.time.DateTime(2026, 6, 1, 0, 0))

        // Assert
        Truth.assertThat(april).isEqualTo("апр.")
        Truth.assertThat(may).isEqualTo("май")
        Truth.assertThat(june).isEqualTo("июнь")
    }

    @Test
    fun `GIVEN english locale WHEN formatStandaloneShortMonth THEN short month name`() {
        // Arrange
        Locale.setDefault(Locale.US)

        // Act
        val actual = DateTimeFormatters.formatStandaloneShortMonth(org.joda.time.DateTime(2026, 6, 1, 0, 0))

        // Assert
        Truth.assertThat(actual).isEqualTo("Jun")
    }

    @Test
    fun `getBestFormatterBySkeleton with dd MMMM skeleton produces formatter that does not throw on format`() {
        // Arrange
        val formatter = DateTimeFormatters.getBestFormatterBySkeleton("dd MMMM")
        val date = org.joda.time.DateTime(2025, 2, 13, 12, 0, 0, 0)

        // Act & Assert
        val formatted = formatter.print(date)
        Truth.assertThat(formatted).isNotEmpty()
    }
}