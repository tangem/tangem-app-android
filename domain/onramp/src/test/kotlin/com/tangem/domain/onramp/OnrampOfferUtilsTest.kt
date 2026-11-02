package com.tangem.domain.onramp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampProviderInfo
import com.tangem.domain.onramp.model.OnrampQuote
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.utils.calculateRateDif
import com.tangem.domain.onramp.utils.compareOffersByRateSpeedAndPriority
import java.math.BigDecimal
import org.junit.jupiter.api.Test

internal class OnrampOfferUtilsTest {

    @Test
    fun `GIVEN no best rate WHEN calculate rate difference THEN returns null`() {
        assertThat(calculateRateDif(BigDecimal("90"), null)).isNull()
    }

    @Test
    fun `GIVEN current rate is best WHEN calculate rate difference THEN returns null`() {
        assertThat(calculateRateDif(BigDecimal("100"), BigDecimal("100"))).isNull()
        assertThat(calculateRateDif(BigDecimal("110"), BigDecimal("100"))).isNull()
    }

    @Test
    fun `GIVEN meaningful rate difference WHEN calculate rate difference THEN returns percentage`() {
        assertThat(calculateRateDif(BigDecimal.ZERO, BigDecimal.ONE))
            .isEqualTo(BigDecimal.ONE)
    }

    @Test
    fun `GIVEN negligible rate difference WHEN calculate rate difference THEN hides difference`() {
        assertThat(calculateRateDif(BigDecimal("99.99995"), BigDecimal("100"))).isNull()
    }

    @Test
    fun `GIVEN equal rates WHEN compare offers THEN faster payment method wins`() {
        val card = quote(PaymentMethodType.CARD, BigDecimal("100"))
        val sepa = quote(PaymentMethodType.SEPA, BigDecimal("100"))

        val comparator = compareOffersByRateSpeedAndPriority(isGooglePayAvailable = false)

        assertThat(comparator.compare(card, sepa)).isGreaterThan(0)
    }

    @Test
    fun `GIVEN sepa priority enabled WHEN compare offers THEN sepa wins despite lower rate`() {
        val card = quote(PaymentMethodType.CARD, BigDecimal("100"))
        val sepa = quote(PaymentMethodType.SEPA, BigDecimal("90"))

        val comparator = compareOffersByRateSpeedAndPriority(
            isGooglePayAvailable = false,
            isSepaPrioritized = true,
        )

        assertThat(comparator.compare(sepa, card)).isGreaterThan(0)
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