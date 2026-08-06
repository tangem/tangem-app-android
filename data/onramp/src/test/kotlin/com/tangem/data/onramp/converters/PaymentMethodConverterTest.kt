package com.tangem.data.onramp.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.repositories.OnrampFeatureToggles
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentMethodConverterTest {

    private val toggles: OnrampFeatureToggles = mockk()
    private val converter = PaymentMethodConverter(onrampFeatureToggles = toggles)

    private val dto = PaymentMethodDTO(
        id = "card",
        name = "Card",
        image = "legacy",
        imageLight = "light",
        imageDark = "dark",
    )

    @Test
    fun `GIVEN toggle enabled WHEN convert THEN themed urls mapped`() {
        // Arrange
        every { toggles.isThemedPaymentMethodImagesEnabled } returns true

        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual).isEqualTo(
            OnrampPaymentMethod(
                id = "card",
                name = "Card",
                imageUrl = "legacy",
                type = PaymentMethodType.CARD,
                imageUrlLight = "light",
                imageUrlDark = "dark",
            ),
        )
    }

    @Test
    fun `GIVEN toggle disabled WHEN convert THEN themed urls null`() {
        // Arrange
        every { toggles.isThemedPaymentMethodImagesEnabled } returns false

        // Act
        val actual = converter.convert(dto)

        // Assert
        assertThat(actual).isEqualTo(
            OnrampPaymentMethod(
                id = "card",
                name = "Card",
                imageUrl = "legacy",
                type = PaymentMethodType.CARD,
                imageUrlLight = null,
                imageUrlDark = null,
            ),
        )
    }

    @Test
    fun `GIVEN model with themed urls WHEN convertBack THEN dto carries both`() {
        // Arrange
        val model = OnrampPaymentMethod(
            id = "card", name = "Card", imageUrl = "legacy", type = PaymentMethodType.CARD,
            imageUrlLight = "light", imageUrlDark = "dark",
        )

        // Act
        val actual = converter.convertBack(model)

        // Assert
        assertThat(actual).isEqualTo(
            PaymentMethodDTO(id = "card", name = "Card", image = "legacy", imageLight = "light", imageDark = "dark"),
        )
    }
}