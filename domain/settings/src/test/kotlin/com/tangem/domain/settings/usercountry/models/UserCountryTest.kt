package com.tangem.domain.settings.usercountry.models

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.util.Locale

internal class UserCountryTest {

    private val defaultLocale: Locale = Locale.getDefault()

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class From {

        @ParameterizedTest
        @ProvideTestModels
        fun from(model: FromModel) {
            // Act
            val actual = UserCountry.from(code = model.code)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FromModel(code = "ru", expected = UserCountry.Russia),
            FromModel(code = "RU", expected = UserCountry.Russia),
            FromModel(code = "de", expected = UserCountry.Other(code = "de")),
            FromModel(code = "DE", expected = UserCountry.Other(code = "de")),
            FromModel(code = "", expected = UserCountry.Other(code = "")),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FromDeviceLocale {

        @Test
        fun `GIVEN russian device locale WHEN fromDeviceLocale THEN returns Russia`() {
            // Arrange
            Locale.setDefault(Locale("ru", "RU"))

            // Act & Assert
            assertThat(UserCountry.fromDeviceLocale()).isEqualTo(UserCountry.Russia)
        }

        @Test
        fun `GIVEN uk device locale WHEN fromDeviceLocale THEN returns lowercase code`() {
            // Arrange
            Locale.setDefault(Locale.UK)

            // Act & Assert
            assertThat(UserCountry.fromDeviceLocale()).isEqualTo(UserCountry.Other(code = "gb"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NeedApplyFCARestrictions {

        @ParameterizedTest
        @ProvideTestModels
        fun needApplyFCARestrictions(model: FCARestrictionsModel) {
            // Arrange
            Locale.setDefault(model.locale)

            // Act
            val actual = model.userCountry.needApplyFCARestrictions()

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            FCARestrictionsModel(
                locale = Locale.UK,
                userCountry = UserCountry.Other(code = "de"),
                expected = false,
            ),
            FCARestrictionsModel(
                locale = Locale.UK,
                userCountry = UserCountry.Other(code = "gb"),
                expected = true,
            ),
            FCARestrictionsModel(
                locale = Locale.UK,
                userCountry = GB_COUNTRY,
                expected = true,
            ),
            FCARestrictionsModel(
                locale = Locale.UK,
                userCountry = null,
                expected = true,
            ),
            FCARestrictionsModel(
                locale = Locale.GERMANY,
                userCountry = UserCountry.Other(code = "gb"),
                expected = true,
            ),
            FCARestrictionsModel(
                locale = Locale.GERMANY,
                userCountry = GB_COUNTRY,
                expected = true,
            ),
            FCARestrictionsModel(
                locale = Locale.GERMANY,
                userCountry = UserCountry.Russia,
                expected = false,
            ),
            FCARestrictionsModel(
                locale = Locale.GERMANY,
                userCountry = null,
                expected = false,
            ),
        )
    }

    internal data class FromModel(
        val code: String,
        val expected: UserCountry,
    )

    internal data class FCARestrictionsModel(
        val locale: Locale,
        val userCountry: UserCountry?,
        val expected: Boolean,
    )
}