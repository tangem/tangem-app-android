package com.tangem.common.ui.amountScreen.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountParameters
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class AmountStateConverterTest {

    private val currency = mockk<CryptoCurrency.Coin> {
        every { symbol } returns "ETH"
        every { decimals } returns 18
        every { name } returns "Ethereum"
    }
    private val status = CryptoCurrencyStatus(currency = currency, value = mockk(relaxed = true))
    private val iconStateConverter = mockk<CryptoCurrencyToIconStateConverter> {
        every { convert(any<CryptoCurrency>()) } returns CurrencyIconState.Loading
    }
    private val clickIntents = mockk<AmountScreenClickIntents>(relaxed = true)
    private val accountTitleUM = mockk<AccountTitleUM>()

    private fun convert(isMaxButtonVisible: Boolean = true): AmountState = AmountStateConverter(
        clickIntents = clickIntents,
        appCurrency = AppCurrency.Default,
        cryptoCurrencyStatus = status,
        maxEnterAmount = EnterAmountBoundary(
            amount = BigDecimal.ONE,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
        ),
        iconStateConverter = iconStateConverter,
        isBalanceHidden = false,
        accountTitleUM = accountTitleUM,
        isMaxButtonVisible = isMaxButtonVisible,
    ).convert(AmountParameters(title = stringReference("Wallet"), value = ""))

    @Test
    fun `GIVEN no isMaxButtonVisible param WHEN convert THEN Data isMaxButtonVisible is true`() {
        val result = convert()

        assertThat((result as AmountState.Data).isMaxButtonVisible).isTrue()
    }

    @Test
    fun `GIVEN isMaxButtonVisible false WHEN convert THEN Data isMaxButtonVisible is false`() {
        val result = convert(isMaxButtonVisible = false)

        assertThat((result as AmountState.Data).isMaxButtonVisible).isFalse()
    }
}