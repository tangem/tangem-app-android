package com.tangem.feature.swap.ui

import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.SwapState
import com.tangem.feature.swap.domain.models.data.formatToUIRepresentation
import com.tangem.feature.swap.models.FeeState
import com.tangem.feature.swap.models.SwapButton
import com.tangem.feature.swap.models.SwapCardData
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.SwapWarning
import com.tangem.feature.swap.models.TransactionCardType

/**
 * State builder creates a specific states for SwapScreen
 */
class StateBuilder {

    fun createInitialLoadingState(networkCurrency: String, onAmountChanged: (String) -> Unit): SwapStateHolder {
        return SwapStateHolder(
            sendCardData = SwapCardData(
                type = TransactionCardType.SendCard("", true, onAmountChanged),
                amount = null,
                amountEquivalent = null,
                tokenIconUrl = "",
                tokenCurrency = "",
                canSelectAnotherToken = false,
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = null,
                amountEquivalent = null,
                tokenIconUrl = "",
                tokenCurrency = "",
                canSelectAnotherToken = false,
            ),
            fee = FeeState.Loading,
            networkCurrency = networkCurrency,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
            onRefresh = {},
            onBackClicked = {},
            onChangeCardsClicked = {},
        )
    }

    fun createQuotesLoadingState(
        uiStateHolder: SwapStateHolder,
        fromToken: Currency,
        toToken: Currency,
        mainTokenId: String,
    ): SwapStateHolder {
        return uiStateHolder.copy(
            sendCardData = SwapCardData(
                type = (uiStateHolder.sendCardData.type as TransactionCardType.SendCard),
                amount = uiStateHolder.sendCardData.amount,
                amountEquivalent = uiStateHolder.sendCardData.amountEquivalent,
                tokenIconUrl = fromToken.logoUrl,
                tokenCurrency = fromToken.symbol,
                canSelectAnotherToken = mainTokenId != fromToken.id,
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = uiStateHolder.receiveCardData.amount,
                amountEquivalent = uiStateHolder.receiveCardData.amountEquivalent,
                tokenIconUrl = toToken.logoUrl,
                tokenCurrency = toToken.symbol,
                canSelectAnotherToken = mainTokenId != toToken.id,
            ),
            fee = FeeState.Loading,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
        )
    }

    fun createQuotesLoadedState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuoteModel,
        fromToken: Currency,
        onSwapClick: () -> Unit,
    ): SwapStateHolder {
        return uiStateHolder.copy(
            sendCardData = SwapCardData(
                type = (uiStateHolder.sendCardData.type as TransactionCardType.SendCard)
                    .copy(permissionIsGiven = quoteModel.isAllowedToSpend),
                amount = quoteModel.fromTokenAmount.formatToUIRepresentation(),
                amountEquivalent = uiStateHolder.sendCardData.amountEquivalent,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = quoteModel.toTokenAmount.formatToUIRepresentation(),
                amountEquivalent = uiStateHolder.receiveCardData.amountEquivalent,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
            ),
            warnings = if (!quoteModel.isAllowedToSpend) {
                listOf(SwapWarning.PermissionNeeded(fromToken.symbol))
            } else {
                emptyList()
            },
            fee = FeeState.Loaded(quoteModel.estimatedGas.toString()),
            swapButton = SwapButton(
                enabled = quoteModel.isAllowedToSpend,
                loading = false,
                onClick = onSwapClick,
            ),
        )
    }
}