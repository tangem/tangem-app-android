package com.tangem.data.onramp.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.grow.datasource.onramp.models.response.model.PaymentMethodDTO
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentMethodConverterTest {

    private val converter = PaymentMethodConverter()

    private val dto = PaymentMethodDTO(
        id = "card",
        name = "Card",
        image = "legacy",
        imageLight = "light",
        imageDark = "dark",
    )

    @Test
    fun `GIVEN dto with themed urls WHEN convert THEN themed urls mapped`() {
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