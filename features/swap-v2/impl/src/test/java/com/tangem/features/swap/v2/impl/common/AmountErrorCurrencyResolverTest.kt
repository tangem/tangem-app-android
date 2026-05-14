package com.tangem.features.swap.v2.impl.common

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapAmountType
import io.mockk.mockk
import org.junit.Test

internal class AmountErrorCurrencyResolverTest {

    private val from = mockk<CryptoCurrency>(relaxed = true)
    private val to = mockk<CryptoCurrency>(relaxed = true)

    @Test
    fun `GIVEN amountType From WHEN resolveAmountErrorCurrency THEN returns fromCryptoCurrency`() {
        // GIVEN, WHEN
        val result = resolveAmountErrorCurrency(
            fromCryptoCurrency = from,
            toCryptoCurrency = to,
            amountType = SwapAmountType.From,
        )

        // THEN
        assertThat(result).isSameInstanceAs(from)
    }

    @Test
    fun `GIVEN amountType To WHEN resolveAmountErrorCurrency THEN returns toCryptoCurrency`() {
        // GIVEN, WHEN
        val result = resolveAmountErrorCurrency(
            fromCryptoCurrency = from,
            toCryptoCurrency = to,
            amountType = SwapAmountType.To,
        )

        // THEN
        assertThat(result).isSameInstanceAs(to)
    }

    @Test
    fun `GIVEN amountType null WHEN resolveAmountErrorCurrency THEN returns fromCryptoCurrency`() {
        // GIVEN, WHEN
        val result = resolveAmountErrorCurrency(
            fromCryptoCurrency = from,
            toCryptoCurrency = to,
            amountType = null,
        )

        // THEN
        assertThat(result).isSameInstanceAs(from)
    }
}