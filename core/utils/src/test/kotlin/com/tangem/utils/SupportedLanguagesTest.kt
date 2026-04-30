package com.tangem.utils

import com.google.common.truth.Truth
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SupportedLanguagesTest {

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `getCurrentSupportedLanguageCode returns primary language when locale is supported`() {
        // Arrange
        Locale.setDefault(Locale("en", "US"))

        // Act
        val actual = SupportedLanguages.getCurrentSupportedLanguageCode()

        // Assert
        Truth.assertThat(actual).isEqualTo("en")
    }

    @Test
    fun `getCurrentSupportedLanguageCode drops region for supported language`() {
        // Arrange
        Locale.setDefault(Locale("zh", "CN"))

        // Act
        val actual = SupportedLanguages.getCurrentSupportedLanguageCode()

        // Assert
        Truth.assertThat(actual).isEqualTo("zh")
    }

    @Test
    fun `getCurrentSupportedLanguageCode returns ENGLISH when locale is not supported`() {
        // Arrange — pt (Portuguese) is not in supportedLanguageCodes
        Locale.setDefault(Locale("pt", "BR"))

        // Act
        val actual = SupportedLanguages.getCurrentSupportedLanguageCode()

        // Assert
        Truth.assertThat(actual).isEqualTo(SupportedLanguages.ENGLISH)
    }

    @Test
    fun `getCurrentSupportedLanguageCode returns ENGLISH for empty language`() {
        // Arrange
        Locale.setDefault(Locale("", ""))

        // Act
        val actual = SupportedLanguages.getCurrentSupportedLanguageCode()

        // Assert
        Truth.assertThat(actual).isEqualTo(SupportedLanguages.ENGLISH)
    }

    @Test
    fun `getCurrentSupportedLanguageCode supports every code in supportedLanguageCodes`() {
        SupportedLanguages.supportedLanguageCodes.forEach { code ->
            // Arrange
            Locale.setDefault(Locale(code))

            // Act
            val actual = SupportedLanguages.getCurrentSupportedLanguageCode()

            // Assert
            Truth.assertThat(actual).isEqualTo(code)
        }
    }

    @Test
    fun `supportedLanguageCodes contains the expected nine ISO 639-1 codes`() {
        // Assert
        Truth.assertThat(SupportedLanguages.supportedLanguageCodes)
            .containsExactly("en", "ru", "de", "fr", "it", "ja", "uk", "zh", "es")
            .inOrder()
    }
}