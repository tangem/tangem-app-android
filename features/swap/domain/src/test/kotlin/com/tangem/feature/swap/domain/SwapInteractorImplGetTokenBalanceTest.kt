package com.tangem.feature.swap.domain

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
 * Tests for [SwapInteractorImpl.getTokenBalance].
 *
 * Trivial conversion: `SwapAmount(value.amount ?: ZERO, currency.decimals)`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplGetTokenBalanceTest : SwapInteractorImplTestBase() {

    @Test
    fun `should return SwapAmount with the reported balance and decimals when value amount is non-null`() {
        // Given
        val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { decimals } returns 18
        }
        val value = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { amount } returns BigDecimal("5.75")
        }
        val status = CryptoCurrencyStatus(currency = currency, value = value)

        // When
        val result = sut.getTokenBalance(status)

        // Then
        assertThat(result.value).isEqualTo(BigDecimal("5.75"))
        assertThat(result.decimals).isEqualTo(18)
    }

    @Test
    fun `should return SwapAmount with ZERO when value amount is null`() {
        // Given — a non-Loaded value with null amount (e.g. Loading state)
        val currency = mockk<CryptoCurrency.Coin>(relaxed = true) {
            every { decimals } returns 8
        }
        val value = mockk<CryptoCurrencyStatus.Loading>(relaxed = true) {
            every { amount } returns null
        }
        val status = CryptoCurrencyStatus(currency = currency, value = value)

        // When
        val result = sut.getTokenBalance(status)

        // Then
        assertThat(result.value).isEqualTo(BigDecimal.ZERO)
        assertThat(result.decimals).isEqualTo(8)
    }

    @Test
    fun `should preserve decimals from the underlying currency`() {
        // Given — Token with custom decimals
        val currency = mockk<CryptoCurrency.Token>(relaxed = true) {
            every { decimals } returns 6
        }
        val value = mockk<CryptoCurrencyStatus.Loaded>(relaxed = true) {
            every { amount } returns BigDecimal("100")
        }
        val status = CryptoCurrencyStatus(currency = currency, value = value)

        // When
        val result = sut.getTokenBalance(status)

        // Then
        assertThat(result.decimals).isEqualTo(6)
        assertThat(result.value).isEqualTo(BigDecimal("100"))
    }
}