package com.tangem.feature.swap.ui

import com.tangem.feature.swap.converters.TokensDataConverter
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TxState
import com.tangem.feature.swap.models.ApprovePermissionButton
import com.tangem.feature.swap.models.CancelPermissionButton
import com.tangem.feature.swap.models.FeeState
import com.tangem.feature.swap.models.SwapButton
import com.tangem.feature.swap.models.SwapCardData
import com.tangem.feature.swap.models.SwapPermissionState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.SwapSuccessStateHolder
import com.tangem.feature.swap.models.SwapWarning
import com.tangem.feature.swap.models.TransactionCardType
import com.tangem.feature.swap.models.UiActions

/**
 * State builder creates a specific states for SwapScreen
 */
class StateBuilder(val actions: UiActions) {

    private val tokensDataConverter = TokensDataConverter(actions.onSearchEntered, actions.onTokenSelected)

    fun createInitialLoadingState(initialCurrency: Currency): SwapStateHolder {
        return SwapStateHolder(
            sendCardData = SwapCardData(
                type = TransactionCardType.SendCard(actions.onAmountChanged),
                amount = null,
                amountEquivalent = null,
                tokenIconUrl = initialCurrency.logoUrl,
                tokenCurrency = initialCurrency.symbol,
                tokenId = initialCurrency.id,
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
                tokenId = "",
            ),
            fee = FeeState.Loading,
            networkCurrency = initialCurrency.symbol,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
            onRefresh = {},
            onBackClicked = actions.onBackClicked,
            onChangeCardsClicked = actions.onChangeCardsClicked,
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
                tokenId = fromToken.id,
                canSelectAnotherToken = mainTokenId != fromToken.id,
                balance = "",
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = null,
                amountEquivalent = null,
                tokenIconUrl = toToken.logoUrl,
                tokenCurrency = toToken.symbol,
                tokenId = toToken.id,
                canSelectAnotherToken = mainTokenId != toToken.id,
                balance = "",
            ),
            fee = FeeState.Loading,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
            permissionState = uiStateHolder.permissionState,
        )
    }

    fun createQuotesLoadedState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: Currency,
    ): SwapStateHolder {
        val warnings = mutableListOf<SwapWarning>()
        if (!quoteModel.preparedSwapConfigState.isAllowedToSpend && quoteModel.preparedSwapConfigState.isFeeEnough) {
            warnings.add(SwapWarning.PermissionNeeded(fromToken.symbol))
        }
        if (!quoteModel.preparedSwapConfigState.isBalanceEnough) {
            warnings.add(SwapWarning.InsufficientFunds)
        }
        val feeState = if (quoteModel.preparedSwapConfigState.isFeeEnough) {
            FeeState.Loaded(quoteModel.fee)
        } else {
            FeeState.NotEnoughFundsWarning(quoteModel.fee)
        }
        return uiStateHolder.copy(
            sendCardData = SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amount = quoteModel.fromTokenInfo.tokenAmount.formatToUIRepresentation(),
                amountEquivalent = quoteModel.fromTokenInfo.tokenFiatBalance,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                tokenId = quoteModel.fromTokenInfo.tokenId,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                balance = quoteModel.fromTokenInfo.tokenWalletBalance,
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = quoteModel.toTokenInfo.tokenAmount.formatToUIRepresentation(),
                amountEquivalent = quoteModel.toTokenInfo.tokenFiatBalance,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                tokenId = quoteModel.toTokenInfo.tokenId,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                balance = quoteModel.toTokenInfo.tokenWalletBalance,
            ),
            networkCurrency = quoteModel.networkCurrency,
            warnings = warnings,
            permissionState = convertPermissionState(quoteModel.permissionState, actions.onGivePermissionClick),
            fee = feeState,
            swapButton = SwapButton(
                enabled = quoteModel.preparedSwapConfigState.isAllowedToSpend
                    && quoteModel.preparedSwapConfigState.isBalanceEnough
                    && quoteModel.preparedSwapConfigState.isFeeEnough,
                loading = false,
                onClick = actions.onSwapClick,
            ),
        )
    }

    fun createQuotesEmptyAmountState(
        uiStateHolder: SwapStateHolder,
        emptyAmountState: SwapState.EmptyAmountState,
    ): SwapStateHolder {
        return uiStateHolder.copy(
            sendCardData = SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amount = uiStateHolder.sendCardData.amount,
                amountEquivalent = uiStateHolder.sendCardData.amountEquivalent,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                tokenId = uiStateHolder.sendCardData.tokenId,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                balance = emptyAmountState.fromTokenWalletBalance,
            ),
            receiveCardData = SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amount = "0",
                amountEquivalent = uiStateHolder.receiveCardData.amountEquivalent,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                tokenId = uiStateHolder.receiveCardData.tokenId,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                balance = emptyAmountState.toTokenWalletBalance,
            ),
            fee = FeeState.Empty,
            swapButton = SwapButton(
                enabled = false,
                loading = false,
                onClick = { },
            ),
        )
    }

    fun createSwapInProgressState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            swapButton = uiState.swapButton.copy(
                loading = true,
                enabled = false,
            ),
        )
    }

    fun addTokensToState(uiState: SwapStateHolder, dataState: FoundTokensState): SwapStateHolder {
        return uiState.copy(
            selectTokenState = tokensDataConverter.convert(dataState),
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

    fun updateSwapAmount(uiState: SwapStateHolder, amount: String): SwapStateHolder {
        return uiState.copy(
            sendCardData = uiState.sendCardData.copy(
                amount = amount,
            ),
        )
    }

    fun loadingPermissionState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            permissionState = SwapPermissionState.InProgress,
            warnings = uiState.warnings.filterNot { it is SwapWarning.PermissionNeeded },
        )
    }

    fun createSuccessState(uiState: SwapStateHolder, txState: TxState.TxSent): SwapStateHolder {
        return uiState.copy(
            successState = SwapSuccessStateHolder(
                fromTokenAmount = txState.fromAmount ?: "",
                toTokenAmount = txState.toAmount ?: "",
                onSecondaryButtonClick = {},
            ),
        )
    }

    fun createSwapErrorTransaction(uiState: SwapStateHolder, onAlertClick: () -> Unit): SwapStateHolder {
        return uiState.copy(
            swapButton = uiState.swapButton.copy(
                enabled = true,
                loading = false,
            ),
            alert = SwapWarning.GenericWarning(
                message = null,
                onClick = onAlertClick,
            ),
        )
    }

    fun addWarning(uiState: SwapStateHolder, message: String?): SwapStateHolder {
        return if (message != null) {
            val renewWarnings = uiState.warnings.filterNot { it is SwapWarning.GenericWarning }.toMutableList()
            renewWarnings.add(SwapWarning.GenericWarning(message) {})
            uiState.copy(
                warnings = renewWarnings,
            )
        } else {
            uiState
        }
    }

    fun mapError(uiState: SwapStateHolder, error: DataError): SwapStateHolder {
        return when (error) {
// [REDACTED_TODO_COMMENT]
            // DataError.InsufficientLiquidity -> TODO()
            // DataError.NoError -> TODO()
            is DataError.UnknownError -> addWarning(uiState, error.message)
            else -> addWarning(uiState, null)
        }
    }

    fun addAlert(uiState: SwapStateHolder, onClick: () -> Unit): SwapStateHolder {
        return uiState.copy(
            alert = SwapWarning.GenericWarning(
                message = null,
                onClick = onClick,
            ),
        )
    }

    fun clearAlert(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            alert = null,
        )
    }
}
