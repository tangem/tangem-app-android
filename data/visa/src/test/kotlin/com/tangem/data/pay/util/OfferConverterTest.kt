package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.CustomerOffersResponse
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.plasticOffer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OfferConverterTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun convert(model: ConvertModel) {
        // Act
        val actual = OfferConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN plastic type WHEN convert THEN offer is plastic`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "TANGEM_PAY_PLASTIC_VISA"))

        // Assert
        assertThat(actual.type).isEqualTo(Offer.Type.TANGEM_PAY_PLASTIC_VISA)
        assertThat(actual.isPlastic).isTrue()
    }

    @Test
    fun `GIVEN non-plastic type WHEN convert THEN offer is not plastic`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"))

        // Assert
        assertThat(actual.isPlastic).isFalse()
    }

    @Test
    fun `GIVEN list with a plastic offer WHEN plasticOffer THEN returns the plastic offer`() {
        // Arrange
        val offers = OfferConverter.convertList(
            listOf(
                createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"),
                createResponseOffer(type = "TANGEM_PAY_PLASTIC_VISA"),
            ),
        )

        // Act
        val actual = offers.plasticOffer()

        // Assert
        assertThat(actual?.isPlastic).isTrue()
    }

    @Test
    fun `GIVEN list without a plastic offer WHEN plasticOffer THEN returns null`() {
        // Arrange
        val offers = OfferConverter.convertList(listOf(createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN")))

        // Assert
        assertThat(offers.plasticOffer()).isNull()
    }

    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "virtual type + opaque spec -> mapped, not plastic",
            response = createResponseOffer(
                type = "CARD_ISSUE_VIRTUAL_RAIN",
                specificationName = "SP_000004",
                orderType = "CARD_ISSUE_VIRTUAL_RAIN_KYC",
            ),
            expected = Offer(
                type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000004", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC),
            ),
        ),
        ConvertModel(
            name = "plastic type -> Type.TANGEM_PAY_PLASTIC_VISA (spec name stays opaque)",
            response = createResponseOffer(
                type = "TANGEM_PAY_PLASTIC_VISA",
                specificationName = "SP_000010",
                orderType = "CARD_REISSUE",
            ),
            expected = Offer(
                type = Offer.Type.TANGEM_PAY_PLASTIC_VISA,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000010", orderType = OrderType.CARD_REISSUE),
            ),
        ),
        ConvertModel(
            name = "unknown type -> Type.UNKNOWN",
            response = createResponseOffer(type = "SOMETHING_NEW"),
            expected = Offer(
                type = Offer.Type.UNKNOWN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000004", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC),
            ),
        ),
    )

    internal data class ConvertModel(
        val name: String,
        val response: CustomerOffersResponse.Offer,
        val expected: Offer,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        fun createResponseOffer(
            type: String = "CARD_ISSUE_VIRTUAL_RAIN",
            amount: String = "1.00",
            currency: String = "USD",
            specificationName: String = "SP_000004",
            orderType: String = "CARD_ISSUE_VIRTUAL_RAIN_KYC",
        ) = CustomerOffersResponse.Offer(
            type = type,
            fee = CustomerOffersResponse.Fee(amount = BigDecimal(amount), currency = currency),
            data = CustomerOffersResponse.Data(specificationName = specificationName, orderType = orderType),
        )
    }
}