package com.tangem.domain.onramp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampPaymentMethodTest {

    @ParameterizedTest
    @ProvideTestModels
    fun imageUrl(model: ImageUrlCase) {
        // Act
        val actual = createMethod(model.light, model.dark).imageUrl(isDark = model.isDark)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN both null WHEN hasThemedImages THEN false`() {
        assertThat(createMethod(light = null, dark = null).hasThemedImages).isFalse()
    }

    @Test
    fun `GIVEN light present WHEN hasThemedImages THEN true`() {
        assertThat(createMethod(light = "l", dark = null).hasThemedImages).isTrue()
    }

    @Test
    fun `GIVEN dark present WHEN hasThemedImages THEN true`() {
        assertThat(createMethod(light = null, dark = "d").hasThemedImages).isTrue()
    }

    private fun createMethod(light: String?, dark: String?) = OnrampPaymentMethod(
        id = "card",
        name = "Card",
        imageUrl = "legacy",
        type = PaymentMethodType.CARD,
        imageUrlLight = light,
        imageUrlDark = dark,
    )

    data class ImageUrlCase(val light: String?, val dark: String?, val isDark: Boolean, val expected: String)

    private fun provideTestModels() = listOf(
        ImageUrlCase(light = "l", dark = "d", isDark = false, expected = "l"),
        ImageUrlCase(light = "l", dark = "d", isDark = true, expected = "d"),
        ImageUrlCase(light = "l", dark = null, isDark = false, expected = "l"),
        ImageUrlCase(light = "l", dark = null, isDark = true, expected = "l"),
        ImageUrlCase(light = null, dark = "d", isDark = false, expected = "d"),
        ImageUrlCase(light = null, dark = "d", isDark = true, expected = "d"),
        ImageUrlCase(light = null, dark = null, isDark = false, expected = "legacy"),
        ImageUrlCase(light = null, dark = null, isDark = true, expected = "legacy"),
    )
}