package com.tangem.features.swap.v2.impl.amount.model.converter

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList")
internal class SwapAmountReadyStateConverter(
    private val userWallet: UserWallet,
    private val swapCurrencies: SwapCurrencies,
    private val primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val secondaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val clickIntents: AmountScreenClickIntents,
    private val swapDirection: SwapDirection,
    private val isBalanceHidden: Boolean,
) : Converter<Unit, SwapAmountUM> {

    private val amountFieldConverter = SwapAmountFieldConverter(
        swapDirection = swapDirection,
        isBalanceHidden = isBalanceHidden,
        userWallet = userWallet,
        appCurrency = appCurrency,
        clickIntents = clickIntents,
    )

    override fun convert(value: Unit): SwapAmountUM {
        return SwapAmountUM.Content(
            primaryCryptoCurrencyStatus = primaryCryptoCurrencyStatus,
            secondaryCryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
            primaryAmount = amountFieldConverter.convert(
                selectedType = SwapAmountType.From,
                cryptoCurrencyStatus = primaryCryptoCurrencyStatus,
            ),
            secondaryAmount = amountFieldConverter.convert(
                selectedType = SwapAmountType.To,
                cryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
            ),
            swapCurrencies = swapCurrencies,
            swapDirection = swapDirection,
            swapQuotes = persistentListOf(),
            selectedQuote = SwapQuoteUM.Empty,
            selectedAmountType = SwapAmountType.From,
            appCurrency = appCurrency,
            swapRateType = ExpressRateType.Float,
            isPrimaryButtonEnabled = false,
        )
    }
}