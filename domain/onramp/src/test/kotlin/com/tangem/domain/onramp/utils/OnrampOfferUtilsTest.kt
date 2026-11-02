package com.tangem.domain.onramp.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampProviderInfo
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.PaymentMethodType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

internal class OnrampOfferUtilsTest {

    @Test
    fun `GIVEN no best rate WHEN calculate rate difference THEN returns null`() {
        assertThat(calculateRateDif(BigDecimal("90"), null)).isNull()
    }

    @ParameterizedTest
    @CsvSource("100", "110")
    fun `GIVEN current rate is best WHEN calculate rate difference THEN returns null`(current: String) {
        assertThat(calculateRateDif(BigDecimal(current), BigDecimal("100"))).isNull()
    }

    @ParameterizedTest
    @CsvSource("90,100,0.1", "90.00,100,0.1", "0,1,1", "99.99,100,0.0001", "0.0009,0.001,0.1")
    fun `GIVEN meaningful difference WHEN calculate rate difference THEN returns scale independent fraction`(
        current: String,
        best: String,
        expected: String,
    ) {
        // Act
        val result = calculateRateDif(BigDecimal(current), BigDecimal(best))

        // Assert
        assertThat(result).isNotNull()
        assertThat(result!!.compareTo(BigDecimal(expected))).isEqualTo(0)
    }

    @Test
    fun `GIVEN zero best rate WHEN calculate rate difference THEN returns null`() {
        assertThat(calculateRateDif(BigDecimal.ZERO, BigDecimal.ZERO)).isNull()
    }

    @Test
    fun `GIVEN negligible rate difference WHEN calculate rate difference THEN hides difference`() {
        assertThat(calculateRateDif(BigDecimal("99.99995"), BigDecimal("100"))).isNull()
    }

    @Test
    fun `GIVEN equal rates WHEN compare offers THEN faster payment method wins`() {
        // Arrange
        val card = quote(PaymentMethodType.CARD, BigDecimal("100"))
        val sepa = quote(PaymentMethodType.SEPA, BigDecimal("100"))

        // Act
        val comparator = compareOffersByRateSpeedAndPriority(isGooglePayAvailable = false)

        // Assert
        assertThat(comparator.compare(card, sepa)).isGreaterThan(0)
    }

    @Test
    fun `GIVEN sepa priority enabled WHEN compare offers THEN sepa wins despite lower rate`() {
        // Arrange
        val card = quote(PaymentMethodType.CARD, BigDecimal("100"))
        val sepa = quote(PaymentMethodType.SEPA, BigDecimal("90"))

        // Act
        val comparator = compareOffersByRateSpeedAndPriority(
            isGooglePayAvailable = false,
            isSepaPrioritized = true,
        )

        // Assert
        assertThat(comparator.compare(sepa, card)).isGreaterThan(0)
        assertThat(comparator.compare(card, sepa)).isLessThan(0)
    }

    @ParameterizedTest
    @CsvSource("true,1", "false,-1")
    fun `GIVEN equal instant offers WHEN compare THEN Google Pay availability breaks tie`(
        googlePayAvailable: Boolean,
        expectedSign: Int,
    ) {
        // Arrange
        val googlePay = quote(PaymentMethodType.GOOGLE_PAY, BigDecimal.TEN)
        val revolut = quote(PaymentMethodType.REVOLUT_PAY, BigDecimal.TEN)

        // Act
        val result = compareOffersByRateSpeedAndPriority(googlePayAvailable).compare(googlePay, revolut)

        // Assert
        assertThat(result.compareTo(0)).isEqualTo(expectedSign)
    }

    @Test
    fun `GIVEN different rates WHEN compare without sepa priority THEN rate wins over speed`() {
        // Arrange
        val card = quote(PaymentMethodType.CARD, BigDecimal("90"))
        val sepa = quote(PaymentMethodType.SEPA, BigDecimal("100"))

        // Act
        val comparator = compareOffersByRateSpeedAndPriority(isGooglePayAvailable = false)

        // Assert
        assertThat(comparator.compare(sepa, card)).isGreaterThan(0)
        assertThat(comparator.compare(card, sepa)).isLessThan(0)
        assertThat(comparator.compare(card, card)).isEqualTo(0)
    }

    private fun quote(type: PaymentMethodType, amount: BigDecimal) = OnrampQuote.Data(
        paymentMethod = OnrampPaymentMethod(
            id = type.name,
            name = type.name,
            imageUrl = "",
            type = type,
        ),
        provider = OnrampProvider(
            id = "provider",
            info = OnrampProviderInfo("Provider", "", null, null),
            paymentMethods = emptyList(),
        ),
        fromAmount = amount(10),
        countryCode = "CC",
        toAmount = amount(amount),
        minFromAmount = null,
        maxFromAmount = null,
    )

    private fun amount(value: Number) = OnrampAmount(
        symbol = "USD",
        value = BigDecimal(value.toString()),
        decimals = 2,
    )
}