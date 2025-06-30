package com.tangem.features.swap.v2.impl.amount.ui.preview

import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountType
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import kotlinx.collections.immutable.persistentListOf

internal data object SwapAmountContentPreview {

    val emptyState = SwapAmountUM.Content(
        isPrimaryButtonEnabled = false,
        primaryAmount = SwapAmountFieldUM.Empty(
            amountType = SwapAmountType.From,
        ),
        secondaryAmount = SwapAmountFieldUM.Empty(
            amountType = SwapAmountType.To,
        ),
        swapDirection = SwapDirection.Direct,
        selectedAmountType = SwapAmountType.From,
        swapCurrencies = SwapCurrencies.EMPTY,
        swapQuotes = persistentListOf(),
        selectedQuote = SwapQuoteUM.Empty,
        primaryCryptoCurrencyStatus = null,
        secondaryCryptoCurrencyStatus = null,
        swapRateType = ExpressRateType.Float,
        appCurrency = AppCurrency.Default,
    )

    val defaultState = SwapAmountUM.Content(
        primaryAmount = SwapAmountFieldUM.Content(
            amountType = SwapAmountType.From,
            amountField = AmountStatePreviewData.amountState.copy(
                availableBalance = stringReference("Balance: 100 BTC"),
            ),
            title = stringReference("Tether"),
            subtitle = stringReference("Balance: 100 BTC"),
            priceImpact = null,
            isClickEnabled = false,
            subtitleEllipsis = TextEllipsis.OffsetEnd(3),
        ),
        secondaryAmount = SwapAmountFieldUM.Content(
            amountType = SwapAmountType.To,
            amountField = AmountStatePreviewData.amountState.copy(
                title = stringReference("Amount to receive"),
                availableBalance = TextReference.EMPTY,
            ),
            title = stringReference("Shiba Inu"),
            priceImpact = stringReference("(-10%)"),
            subtitle = TextReference.EMPTY,
            isClickEnabled = false,
            subtitleEllipsis = TextEllipsis.OffsetEnd(3),
        ),
        appCurrency = AppCurrency.Default,
        swapDirection = SwapDirection.Direct,
        selectedAmountType = SwapAmountType.From,
        swapCurrencies = SwapCurrencies.EMPTY,
        swapQuotes = persistentListOf(),
        selectedQuote = SwapQuoteUM.Empty,
        primaryCryptoCurrencyStatus = null,
        secondaryCryptoCurrencyStatus = null,
        swapRateType = ExpressRateType.Float,
        isPrimaryButtonEnabled = true,
    )
}
