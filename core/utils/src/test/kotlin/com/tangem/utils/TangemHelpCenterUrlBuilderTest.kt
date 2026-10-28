package com.tangem.utils

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemHelpCenterUrlBuilderTest {

    @ParameterizedTest
    @MethodSource("provideSiteLanguageModels")
    fun `GIVEN a device locale WHEN siteLanguage THEN the matching site language or English`(model: SiteLanguageModel) {
        // Act
        val actual = TangemHelpCenterUrlBuilder.siteLanguage(model.locale)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class SiteLanguageModel(val locale: Locale, val expected: String)

    private fun provideSiteLanguageModels() = listOf(
        SiteLanguageModel(locale = Locale("en", "US"), expected = "en"),
        SiteLanguageModel(locale = Locale("de", "DE"), expected = "de"),
        SiteLanguageModel(locale = Locale("es", "ES"), expected = "es"),
        SiteLanguageModel(locale = Locale("fr", "FR"), expected = "fr"),
        SiteLanguageModel(locale = Locale("ja", "JP"), expected = "ja"),
        SiteLanguageModel(locale = Locale("ko", "KR"), expected = "ko"),
        SiteLanguageModel(locale = Locale("pt", "BR"), expected = "pt"),
        SiteLanguageModel(locale = Locale("uk", "UA"), expected = "uk"),
        SiteLanguageModel(locale = Locale("zh", "CN"), expected = "zh-Hans"),
        SiteLanguageModel(locale = Locale("ru", "RU"), expected = "en"),
        SiteLanguageModel(locale = Locale("it", "IT"), expected = "en"),
        SiteLanguageModel(locale = Locale("pl", "PL"), expected = "en"),
    )

    @Test
    fun `GIVEN an article with an anchor WHEN helpCenterUrl THEN the localized article url is built`() {
        // Act
        val actual = TangemHelpCenterUrlBuilder.helpCenterUrl(
            articlePath = "/getting-started/how-to/",
            anchor = "section",
            locale = Locale("zh", "CN"),
        )

        // Assert
        assertThat(actual).isEqualTo("https://tangem.com/zh-Hans/help-center/getting-started/how-to/#section")
    }

    @Test
    fun `GIVEN an article without an anchor WHEN helpCenterUrl THEN no fragment is added`() {
        // Act
        val actual = TangemHelpCenterUrlBuilder.helpCenterUrl(
            articlePath = "getting-started/how-to",
            locale = Locale("ru", "RU"),
        )

        // Assert
        assertThat(actual).isEqualTo("https://tangem.com/en/help-center/getting-started/how-to/")
    }
}