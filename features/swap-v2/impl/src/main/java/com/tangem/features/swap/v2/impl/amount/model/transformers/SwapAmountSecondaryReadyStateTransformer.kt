package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.model.converter.SwapAmountFieldConverter
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongParameterList")
internal class SwapAmountSecondaryReadyStateTransformer(
    private val userWallet: UserWallet,
    private val primaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val secondaryCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val swapCurrencies: SwapCurrencies,
    private val clickIntents: AmountScreenClickIntents,
    private val swapDirection: SwapDirection,
    private val isBalanceHidden: Boolean,
    private val showBestRateAnimation: Boolean,
    private val isSingleWallet: Boolean,
) : Transformer<SwapAmountUM> {

    private val amountFieldConverter = SwapAmountFieldConverter(
        swapDirection = swapDirection,
        isBalanceHidden = isBalanceHidden,
        userWallet = userWallet,
        appCurrency = appCurrency,
        clickIntents = clickIntents,
        isSingleWallet = isSingleWallet,
    )

    override fun transform(prevState: SwapAmountUM): SwapAmountUM {
        return SwapAmountUM.Content(
            isPrimaryButtonEnabled = false,
            primaryAmount = prevState.primaryAmount,
            primaryCryptoCurrencyStatus = primaryCryptoCurrencyStatus,
            secondaryAmount = amountFieldConverter.convert(
                selectedType = SwapAmountType.To,
                cryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
            ),
            secondaryCryptoCurrencyStatus = secondaryCryptoCurrencyStatus,
            swapCurrencies = swapCurrencies,
            selectedAmountType = prevState.selectedAmountType,
            swapDirection = swapDirection,
            swapRateType = ExpressRateType.Float,
            swapQuotes = persistentListOf(),
            selectedQuote = SwapQuoteUM.Empty,
            appCurrency = appCurrency,
            showBestRateAnimation = showBestRateAnimation,
            showFCAWarning = false,
        )
    }
}