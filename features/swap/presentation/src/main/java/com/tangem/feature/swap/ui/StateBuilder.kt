package com.tangem.feature.swap.ui

import com.tangem.feature.swap.domain.models.Currency
import com.tangem.feature.swap.domain.models.PermissionDataState
import com.tangem.feature.swap.domain.models.SwapState
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.models.ApprovePermissionButton
import com.tangem.feature.swap.models.CancelPermissionButton
import com.tangem.feature.swap.models.FeeState
import com.tangem.feature.swap.models.SwapButton
import com.tangem.feature.swap.models.SwapCardData
import com.tangem.feature.swap.models.SwapPermissionState
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
                type = TransactionCardType.SendCard(onAmountChanged),
                amount = null,
                amountEquivalent = null,
                tokenIconUrl = "",
                tokenCurrency = "",
                canSelectAnotherToken = false,
                balance = "",
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = null,
                amountEquivalent = null,
                tokenIconUrl = "",
                tokenCurrency = "",
                canSelectAnotherToken = false,
                balance = "",
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
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amount = uiStateHolder.sendCardData.amount,
                amountEquivalent = uiStateHolder.sendCardData.amountEquivalent,
                tokenIconUrl = fromToken.logoUrl,
                tokenCurrency = fromToken.symbol,
                canSelectAnotherToken = mainTokenId != fromToken.id,
                balance = "",
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = uiStateHolder.receiveCardData.amount,
                amountEquivalent = uiStateHolder.receiveCardData.amountEquivalent,
                tokenIconUrl = toToken.logoUrl,
                tokenCurrency = toToken.symbol,
                canSelectAnotherToken = mainTokenId != toToken.id,
                balance = "",
            ),
            fee = FeeState.Loading,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
        )
    }

    fun createQuotesLoadedState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: Currency,
        onSwapClick: () -> Unit,
        onGivePermissionClick: () -> Unit,
    ): SwapStateHolder {
        return uiStateHolder.copy(
            sendCardData = SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amount = quoteModel.fromTokenAmount.formatToUIRepresentation(),
                amountEquivalent = quoteModel.fromTokenFiatBalance,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                balance = quoteModel.fromTokenWalletBalance,
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = quoteModel.toTokenAmount.formatToUIRepresentation(),
                amountEquivalent = quoteModel.toTokenFiatBalance,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                balance = quoteModel.toTokenWalletBalance,
            ),
            warnings = if (!quoteModel.isAllowedToSpend) {
                listOf(SwapWarning.PermissionNeeded(fromToken.symbol))
            } else {
                emptyList()
            },
            permissionState = convertPermissionState(quoteModel.permissionState, onGivePermissionClick),
            fee = FeeState.Loaded(quoteModel.fee),
            swapButton = SwapButton(
                enabled = quoteModel.isAllowedToSpend,
                loading = false,
                onClick = onSwapClick,
            ),
        )
    }

    private fun convertPermissionState(
        permissionDataState: PermissionDataState,
        onGivePermissionClick: () -> Unit,
    ): SwapPermissionState {
        return when (permissionDataState) {
            PermissionDataState.Empty -> SwapPermissionState.Empty
            PermissionDataState.PermissionFailed -> SwapPermissionState.Empty
            PermissionDataState.PermissionLoading -> SwapPermissionState.InProgress
            is PermissionDataState.PermissionReadyForRequest -> SwapPermissionState.ReadyForRequest(
                currency = permissionDataState.currency,
                amount = permissionDataState.amount,
                walletAddress = permissionDataState.walletAddress,
                spenderAddress = permissionDataState.spenderAddress,
                fee = permissionDataState.fee,
                approveButton = ApprovePermissionButton(
                    enabled = true,
                    onClick = onGivePermissionClick,
                ),
                cancelButton = CancelPermissionButton(
                    enabled = true,
                    onClick = {},
                ),
            )
        }
    }
}
