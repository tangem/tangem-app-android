package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.pay.models.response.CustomerOffersResponse
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.plasticOffer
import com.tangem.domain.pay.model.plasticReissueOffer
import com.tangem.domain.pay.model.virtualOffer
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
        val actual = OfferConverter.convert(createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN"))

        // Assert
        assertThat(actual?.type).isEqualTo(Offer.Type.CARD_ISSUE_PLASTIC_RAIN)
        assertThat(actual?.isPlastic).isTrue()
    }

    @Test
    fun `GIVEN the wire value the plastic offer is matched on WHEN read THEN it is the one the backend sends`() {
        // Assert
        assertThat(Offer.Type.CARD_ISSUE_PLASTIC_RAIN.wireValue).isEqualTo("CARD_ISSUE_PLASTIC_RAIN")
        assertThat(Offer.Type.CARD_ISSUE_VIRTUAL_RAIN.wireValue).isEqualTo("CARD_ISSUE_VIRTUAL_RAIN")
        assertThat(Offer.Type.CARD_REISSUE_PLASTIC_RAIN.wireValue).isEqualTo("CARD_REISSUE_PLASTIC_RAIN")
    }

    @Test
    fun `GIVEN reissue type WHEN convert THEN offer is a plastic reissue and not a plastic issue`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "CARD_REISSUE_PLASTIC_RAIN"))

        // Assert
        assertThat(actual?.type).isEqualTo(Offer.Type.CARD_REISSUE_PLASTIC_RAIN)
        assertThat(actual?.isPlasticReissue).isTrue()
        assertThat(actual?.isPlastic).isFalse()
    }

    @Test
    fun `GIVEN a list with a reissue offer WHEN plasticReissueOffer THEN it is picked`() {
        // Arrange
        val offers = listOf(
            createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN"),
            createResponseOffer(type = "CARD_REISSUE_PLASTIC_RAIN"),
        ).mapNotNull(OfferConverter::convert)

        // Act
        val actual = offers.plasticReissueOffer()

        // Assert
        assertThat(actual?.isPlasticReissue).isTrue()
    }

    @Test
    fun `GIVEN list without a reissue offer WHEN plasticReissueOffer THEN returns null`() {
        // Arrange
        val offers = listOf(createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN"))
            .mapNotNull(OfferConverter::convert)

        // Assert
        assertThat(offers.plasticReissueOffer()).isNull()
    }

    @Test
    fun `GIVEN the superseded plastic wire value WHEN convert THEN offer is unknown and not plastic`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "TANGEM_PAY_PLASTIC_VISA"))

        // Assert
        assertThat(actual?.type).isEqualTo(Offer.Type.UNKNOWN)
        assertThat(actual?.isPlastic).isFalse()
    }

    @Test
    fun `GIVEN a lowercase plastic type WHEN convert THEN offer is plastic`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "card_issue_plastic_rain"))

        // Assert
        assertThat(actual?.isPlastic).isTrue()
    }

    @Test
    fun `GIVEN non-plastic type WHEN convert THEN offer is not plastic`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"))

        // Assert
        assertThat(actual?.isPlastic).isFalse()
    }

    @Test
    fun `GIVEN list with a plastic offer WHEN plasticOffer THEN returns the plastic offer`() {
        // Arrange
        val offers = listOf(
            createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"),
            createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN"),
        ).mapNotNull(OfferConverter::convert)

        // Act
        val actual = offers.plasticOffer()

        // Assert
        assertThat(actual?.isPlastic).isTrue()
    }

    @Test
    fun `GIVEN list without a plastic offer WHEN plasticOffer THEN returns null`() {
        // Arrange
        val offers = listOf(createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"))
            .mapNotNull(OfferConverter::convert)

        // Assert
        assertThat(offers.plasticOffer()).isNull()
    }

    @Test
    fun `GIVEN a placeholder plastic offer next to a virtual one WHEN convert THEN only the virtual one is kept`() {
        // Arrange
        val response = listOf(
            createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"),
            CustomerOffersResponse.Offer(type = "CARD_ISSUE_PLASTIC_RAIN", fee = null, data = null),
        )

        // Act
        val offers = response.mapNotNull(OfferConverter::convert)

        // Assert
        assertThat(offers.single().isVirtual).isTrue()
        assertThat(offers.plasticOffer()).isNull()
    }

    @Test
    fun `GIVEN virtual type WHEN convert THEN offer is virtual`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"))

        // Assert
        assertThat(actual?.isVirtual).isTrue()
    }

    @Test
    fun `GIVEN non-virtual type WHEN convert THEN offer is not virtual`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN"))

        // Assert
        assertThat(actual?.isVirtual).isFalse()
    }

    @Test
    fun `GIVEN list with a virtual offer WHEN virtualOffer THEN returns the virtual offer`() {
        // Arrange
        val offers = listOf(
            createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN"),
            createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"),
        ).mapNotNull(OfferConverter::convert)

        // Act
        val actual = offers.virtualOffer()

        // Assert
        assertThat(actual?.isVirtual).isTrue()
    }

    @Test
    fun `GIVEN list without a virtual offer WHEN virtualOffer THEN returns null`() {
        // Arrange
        val offers = listOf(createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN"))
            .mapNotNull(OfferConverter::convert)

        // Assert
        assertThat(offers.virtualOffer()).isNull()
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
            name = "plastic type -> Type.CARD_ISSUE_PLASTIC_RAIN (spec name stays opaque)",
            response = createResponseOffer(
                type = "CARD_ISSUE_PLASTIC_RAIN",
                specificationName = "SP_000010",
                orderType = "CARD_REISSUE",
            ),
            expected = Offer(
                type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000010", orderType = OrderType.CARD_REISSUE),
            ),
        ),
        ConvertModel(
            name = "plastic offer carries fee and delivery eta from customer/offers",
            response = createResponseOffer(
                type = "CARD_ISSUE_PLASTIC_RAIN",
                amount = "21.69",
                specificationName = "SP_000008",
                orderType = "CARD_ISSUE_PLASTIC_RAIN",
                deliveryEtaMinDays = 2,
                deliveryEtaMaxDays = 4,
            ),
            expected = Offer(
                type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
                fee = Offer.Fee(amount = BigDecimal("21.69"), currency = Currency.getInstance("USD")),
                data = Offer.Data(
                    specificationName = "SP_000008",
                    orderType = OrderType.CARD_ISSUE_PLASTIC_RAIN,
                    deliveryEta = Offer.DeliveryEta(minBusinessDays = 2, maxBusinessDays = 4),
                ),
            ),
        ),
        ConvertModel(
            name = "max eta without min -> eta with a null min",
            response = createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN", deliveryEtaMaxDays = 4),
            expected = Offer(
                type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(
                    specificationName = "SP_000004",
                    orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC,
                    deliveryEta = Offer.DeliveryEta(minBusinessDays = null, maxBusinessDays = 4),
                ),
            ),
        ),
        ConvertModel(
            name = "min eta without max -> no eta",
            response = createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN", deliveryEtaMinDays = 2),
            expected = Offer(
                type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000004", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC),
            ),
        ),
        ConvertModel(
            name = "reissue offer has no spec name and carries fee and delivery eta",
            response = createResponseOffer(
                type = "CARD_REISSUE_PLASTIC_RAIN",
                amount = "10.00",
                specificationName = null,
                orderType = "CARD_REISSUE_PLASTIC_RAIN",
                deliveryEtaMinDays = 3,
                deliveryEtaMaxDays = 20,
            ),
            expected = Offer(
                type = Offer.Type.CARD_REISSUE_PLASTIC_RAIN,
                fee = Offer.Fee(amount = BigDecimal("10.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(
                    specificationName = null,
                    orderType = OrderType.CARD_REISSUE_PLASTIC_RAIN,
                    deliveryEta = Offer.DeliveryEta(minBusinessDays = 3, maxBusinessDays = 20),
                ),
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
        ConvertModel(
            name = "plastic offer announced without fee and data -> dropped",
            response = CustomerOffersResponse.Offer(type = "CARD_ISSUE_PLASTIC_RAIN", fee = null, data = null),
            expected = null,
        ),
        ConvertModel(
            name = "offer without a fee -> dropped",
            response = createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN").copy(fee = null),
            expected = null,
        ),
        ConvertModel(
            name = "offer without data -> dropped",
            response = createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN").copy(data = null),
            expected = null,
        ),
        ConvertModel(
            name = "offer without an order type -> dropped",
            response = createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN", orderType = null),
            expected = null,
        ),
        ConvertModel(
            name = "offer with a blank order type -> dropped",
            response = createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN", orderType = "  "),
            expected = null,
        ),
        ConvertModel(
            name = "offer with a fee object but no amount -> dropped",
            response = createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN", amount = null),
            expected = null,
        ),
        ConvertModel(
            name = "unknown but present order type -> kept as OrderType.UNKNOWN",
            response = createResponseOffer(type = "CARD_ISSUE_PLASTIC_RAIN", orderType = "ORDER_TYPE_FROM_THE_FUTURE"),
            expected = Offer(
                type = Offer.Type.CARD_ISSUE_PLASTIC_RAIN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000004", orderType = OrderType.UNKNOWN),
            ),
        ),
        ConvertModel(
            name = "offer without a type -> kept as Type.UNKNOWN",
            response = createResponseOffer(type = null),
            expected = Offer(
                type = Offer.Type.UNKNOWN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000004", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC),
            ),
        ),
        ConvertModel(
            name = "offer without a fee currency -> falls back to USD",
            response = createResponseOffer(currency = null),
            expected = Offer(
                type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000004", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC),
            ),
        ),
        ConvertModel(
            name = "offer without images -> mapped with no main image",
            response = createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN", images = null),
            expected = Offer(
                type = Offer.Type.CARD_ISSUE_VIRTUAL_RAIN,
                fee = Offer.Fee(amount = BigDecimal("1.00"), currency = Currency.getInstance("USD")),
                data = Offer.Data(specificationName = "SP_000004", orderType = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC),
            ),
        ),
    )

    internal data class ConvertModel(
        val name: String,
        val response: CustomerOffersResponse.Offer,
        val expected: Offer?,
    ) {
        override fun toString(): String = name
    }

    @Test
    fun `GIVEN an offer with a MAIN image WHEN convert THEN mainImageUrl exposes it`() {
        // Act
        val actual = OfferConverter.convert(
            createResponseOffer(
                type = "CARD_ISSUE_PLASTIC_RAIN",
                images = listOf(CustomerOffersResponse.Image(type = "MAIN", url = PLASTIC_IMAGE_URL)),
            ),
        )

        // Assert
        assertThat(actual?.mainImageUrl).isEqualTo(PLASTIC_IMAGE_URL)
    }

    @Test
    fun `GIVEN an offer without images WHEN convert THEN mainImageUrl is null`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(type = "CARD_ISSUE_VIRTUAL_RAIN"))

        // Assert
        assertThat(actual?.mainImageUrl).isNull()
    }

    @Test
    fun `GIVEN a fee currency that is not a valid ISO code WHEN convert THEN falls back to USD instead of throwing`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(currency = "USDC"))

        // Assert
        assertThat(actual?.fee?.currency).isEqualTo(Currency.getInstance("USD"))
    }

    @Test
    fun `GIVEN a valid non-USD fee currency WHEN convert THEN it is preserved`() {
        // Act
        val actual = OfferConverter.convert(createResponseOffer(currency = "EUR"))

        // Assert
        assertThat(actual?.fee?.currency).isEqualTo(Currency.getInstance("EUR"))
    }

    @Test
    fun `GIVEN a MAIN image without a url WHEN convert THEN mainImageUrl is null`() {
        // Act
        val actual = OfferConverter.convert(
            createResponseOffer(images = listOf(CustomerOffersResponse.Image(type = "MAIN", url = null))),
        )

        // Assert
        assertThat(actual?.mainImageUrl).isNull()
    }

    @Test
    fun `GIVEN only a non-MAIN image WHEN convert THEN mainImageUrl stays null`() {
        // Act
        val actual = OfferConverter.convert(
            createResponseOffer(
                images = listOf(CustomerOffersResponse.Image(type = "THUMBNAIL", url = PLASTIC_IMAGE_URL)),
            ),
        )

        // Assert
        assertThat(actual?.mainImageUrl).isNull()
    }

    private companion object {

        const val PLASTIC_IMAGE_URL = "https://images.us.paera.com/Physical-main.png"
        fun createResponseOffer(
            type: String? = "CARD_ISSUE_VIRTUAL_RAIN",
            amount: String? = "1.00",
            currency: String? = "USD",
            specificationName: String? = "SP_000004",
            orderType: String? = "CARD_ISSUE_VIRTUAL_RAIN_KYC",
            deliveryEtaMinDays: Int? = null,
            deliveryEtaMaxDays: Int? = null,
            images: List<CustomerOffersResponse.Image>? = emptyList(),
        ) = CustomerOffersResponse.Offer(
            type = type,
            fee = CustomerOffersResponse.Fee(amount = amount?.let(::BigDecimal), currency = currency),
            data = CustomerOffersResponse.Data(
                specificationName = specificationName,
                orderType = orderType,
                deliveryEtaMinDays = deliveryEtaMinDays,
                deliveryEtaMaxDays = deliveryEtaMaxDays,
            ),
            images = images,
        )
    }
}