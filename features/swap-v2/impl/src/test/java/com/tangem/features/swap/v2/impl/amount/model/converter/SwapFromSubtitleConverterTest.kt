package com.tangem.features.swap.v2.impl.amount.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SwapFromSubtitleConverterTest {

    @Test
    fun `GIVEN isEntering true WHEN convert THEN subtitleRight is EMPTY and sendSubtitle is null`() {
        val cryptoCurrencyStatus = mockk<CryptoCurrencyStatus>(relaxed = true)
        every { cryptoCurrencyStatus.currency.symbol } returns "ETH"
        every { cryptoCurrencyStatus.currency.decimals } returns 8
        every { cryptoCurrencyStatus.value.amount } returns BigDecimal("1.5")

        val result = SwapFromSubtitleConverter.convert(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            isBalanceHidden = false,
            isEntering = true,
            isAmountEmpty = false,
            displayAmount = BigDecimal("0.5"),
        )

        assertThat(result.subtitleRight).isEqualTo(TextReference.EMPTY)
        assertThat(result.sendSubtitle).isNull()
    }

    @Test
    fun `GIVEN isEntering false and isAmountEmpty true WHEN convert THEN subtitleRight is EMPTY and sendSubtitle is null`() {
        val cryptoCurrencyStatus = mockk<CryptoCurrencyStatus>(relaxed = true)
        every { cryptoCurrencyStatus.currency.symbol } returns "ETH"
        every { cryptoCurrencyStatus.currency.decimals } returns 8
        every { cryptoCurrencyStatus.value.amount } returns BigDecimal("1.5")

        val result = SwapFromSubtitleConverter.convert(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            isBalanceHidden = false,
            isEntering = false,
            isAmountEmpty = true,
            displayAmount = null,
        )

        assertThat(result.subtitleRight).isEqualTo(TextReference.EMPTY)
        assertThat(result.sendSubtitle).isNull()
    }

    @Test
    fun `GIVEN isEntering false and isAmountEmpty false WHEN convert THEN subtitleRight is EMPTY and sendSubtitle is not null`() {
        val cryptoCurrencyStatus = mockk<CryptoCurrencyStatus>(relaxed = true)
        every { cryptoCurrencyStatus.currency.symbol } returns "ETH"
        every { cryptoCurrencyStatus.currency.decimals } returns 8
        every { cryptoCurrencyStatus.value.amount } returns BigDecimal("1.5")

        val result = SwapFromSubtitleConverter.convert(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            isBalanceHidden = false,
            isEntering = false,
            isAmountEmpty = false,
            displayAmount = BigDecimal("0.5"),
        )

        assertThat(result.subtitleRight).isEqualTo(TextReference.EMPTY)
        assertThat(result.sendSubtitle).isNotNull()
    }

    @Test
    fun `GIVEN isBalanceHidden true and has amount WHEN convert THEN sendSubtitle is not null`() {
        val cryptoCurrencyStatus = mockk<CryptoCurrencyStatus>(relaxed = true)
        every { cryptoCurrencyStatus.currency.symbol } returns "ETH"
        every { cryptoCurrencyStatus.currency.decimals } returns 8
        every { cryptoCurrencyStatus.value.amount } returns BigDecimal("1.5")

        val result = SwapFromSubtitleConverter.convert(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            isBalanceHidden = true,
            isEntering = false,
            isAmountEmpty = false,
            displayAmount = BigDecimal("0.5"),
        )

        assertThat(result.subtitleRight).isEqualTo(TextReference.EMPTY)
        assertThat(result.sendSubtitle).isNotNull()
    }
}