package com.tangem.domain.onramp.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * The `OnrampQuote.isRestricted` extension shadows `OnrampQuote.Data.isRestricted`, so the two read
 * identically at call sites while behaving differently for non-`Data` quotes. Consumers rely on the
 * extension answering `false` for error quotes — `OnrampMainComponentModel` uses it to tell an
 * all-restricted payload apart from an all-failed one.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampQuoteTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isRestricted(model: RestrictedCase) {
        // Act
        val actual = model.quote.isRestricted

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        RestrictedCase(
            name = "restricted data quote",
            quote = createDataQuote(isRestricted = true),
            expected = true,
        ),
        RestrictedCase(
            name = "purchasable data quote",
            quote = createDataQuote(isRestricted = false),
            expected = false,
        ),
        RestrictedCase(
            name = "amount error quote is never restricted",
            quote = mockk<OnrampQuote.AmountError>(),
            expected = false,
        ),
        RestrictedCase(
            name = "generic error quote is never restricted",
            quote = mockk<OnrampQuote.Error>(),
            expected = false,
        ),
    )

    private fun createDataQuote(isRestricted: Boolean) = OnrampQuote.Data(
        paymentMethod = METHOD,
        provider = mockk(relaxed = true),
        fromAmount = AMOUNT,
        countryCode = "US",
        toAmount = AMOUNT,
        minFromAmount = null,
        maxFromAmount = null,
        isRestricted = isRestricted,
    )

    internal data class RestrictedCase(val name: String, val quote: OnrampQuote, val expected: Boolean) {
        override fun toString(): String = name
    }

    private companion object {
        val METHOD = OnrampPaymentMethod(id = "card", name = "Card", imageUrl = "", type = PaymentMethodType.CARD)

        val AMOUNT = OnrampAmount(value = BigDecimal.ONE, decimals = 2, symbol = "USD")
    }
}