package com.tangem.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.Locale

private const val KNOWN_CODE = "AF"
private const val UNASSIGNED_CODE = "QQ"
private const val UNKNOWN_REGION = "ZZ"
private const val PSEUDO_LOCALE_CODE = "XA"
private const val NON_COUNTRY_CODE = "EU"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CountryNamesTest {

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @ParameterizedTest
    @MethodSource("provideDisplayLocales")
    fun `GIVEN known code WHEN getDisplayName THEN returns name localized for locale`(locale: Locale) {
        // Arrange
        val expected = Locale.Builder().setRegion(KNOWN_CODE).build().getDisplayCountry(locale)

        // Act
        val actual = CountryNames.getDisplayName(KNOWN_CODE, locale)

        // Assert
        assertThat(actual).isEqualTo(expected)
        assertThat(actual).isNotEqualTo(KNOWN_CODE)
    }

    @Test
    fun `GIVEN locales of different scripts WHEN getDisplayName THEN names differ`() {
        // Act
        val english = CountryNames.getDisplayName(KNOWN_CODE, Locale.ENGLISH)
        val russian = CountryNames.getDisplayName(KNOWN_CODE, Locale.forLanguageTag("ru"))

        // Assert
        assertThat(english).isNotEqualTo(russian)
    }

    @ParameterizedTest
    @MethodSource("provideDisplayLocales")
    fun `GIVEN padded lowercase code WHEN getDisplayName THEN resolved case-insensitively`(locale: Locale) {
        // Arrange
        val expected = CountryNames.getDisplayName(KNOWN_CODE, locale)

        // Act
        val actual = CountryNames.getDisplayName("  ${KNOWN_CODE.lowercase(Locale.ROOT)}  ", locale)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @MethodSource("provideUnmappablePayloads")
    fun `GIVEN unmappable payload WHEN getDisplayName THEN returns it unchanged`(payload: String) {
        // Act
        val actual = CountryNames.getDisplayName(payload)

        // Assert
        assertThat(actual).isEqualTo(payload)
    }

    @Test
    fun `GIVEN padded unmappable payload WHEN getDisplayName THEN returns trimmed payload`() {
        // Act
        val actual = CountryNames.getDisplayName("  $UNASSIGNED_CODE  ")

        // Assert
        assertThat(actual).isEqualTo(UNASSIGNED_CODE)
    }

    @ParameterizedTest
    @MethodSource("provideDisplayLocales")
    fun `GIVEN unknown-region code WHEN getDisplayName THEN returns code as-is`(locale: Locale) {
        // Act
        val actual = CountryNames.getDisplayName(UNKNOWN_REGION, locale)

        // Assert
        assertThat(actual).isEqualTo(UNKNOWN_REGION)
    }

    @ParameterizedTest
    @MethodSource("provideBlankPayloads")
    fun `GIVEN blank or null payload WHEN getDisplayName THEN returns empty string`(payload: String?) {
        // Act
        val actual = CountryNames.getDisplayName(payload)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN no explicit locale WHEN getDisplayName THEN falls back to default locale`() {
        // Arrange
        val locale = Locale.forLanguageTag("ru")
        Locale.setDefault(locale)

        // Act
        val actual = CountryNames.getDisplayName(KNOWN_CODE)

        // Assert
        assertThat(actual).isEqualTo(CountryNames.getDisplayName(KNOWN_CODE, locale))
        assertThat(actual).isNotEqualTo(KNOWN_CODE)
    }

    private fun provideDisplayLocales() = listOf(
        Locale.ENGLISH,
        Locale.GERMAN,
        Locale.JAPANESE,
        Locale.forLanguageTag("ru"),
    )

    private fun provideUnmappablePayloads() = listOf(
        UNASSIGNED_CODE,
        UNASSIGNED_CODE.lowercase(Locale.ROOT),
        UNKNOWN_REGION,
        UNKNOWN_REGION.lowercase(Locale.ROOT),
        "AFG",
        "643",
        "United States",
        "A",
        "РФ",
    )

    private fun provideBlankPayloads() = listOf<String?>(null, "", "   ")
    @Test
    fun `GIVEN a real country WHEN resolveDisplayName THEN returns its name`() {
        // Act
        val actual = CountryNames.resolveDisplayName(KNOWN_CODE, Locale.ENGLISH)

        // Assert
        assertThat(actual).isEqualTo(
            Locale.Builder().setRegion(KNOWN_CODE).build().getDisplayCountry(Locale.ENGLISH),
        )
    }

    @Test
    fun `GIVEN an unassigned code WHEN resolveDisplayName THEN returns null`() {
        // Act
        val actual = CountryNames.resolveDisplayName(UNASSIGNED_CODE, Locale.ENGLISH)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a pseudo-locale region WHEN resolveDisplayName THEN returns null`() {
        // Act — CLDR names it ("Pseudo-Accents"), so a name-versus-code check would let it through
        val actual = CountryNames.resolveDisplayName(PSEUDO_LOCALE_CODE, Locale.ENGLISH)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a non-country region WHEN resolveDisplayName THEN returns null`() {
        // Act — "European Union" is a name, but not a country
        val actual = CountryNames.resolveDisplayName(NON_COUNTRY_CODE, Locale.ENGLISH)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a lower case code WHEN resolveDisplayName THEN it is still resolved`() {
        // Act
        val actual = CountryNames.resolveDisplayName(KNOWN_CODE.lowercase(Locale.ROOT), Locale.ENGLISH)

        // Assert
        assertThat(actual).isNotNull()
    }

    @Test
    fun `GIVEN no code WHEN resolveDisplayName THEN returns null`() {
        // Act
        val actual = CountryNames.resolveDisplayName(countryCode = null, locale = Locale.ENGLISH)

        // Assert
        assertThat(actual).isNull()
    }

}