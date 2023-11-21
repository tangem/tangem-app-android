package com.tangem.feature.swap.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.Provider
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.components.states.Item
import com.tangem.core.ui.components.states.SelectableItemsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.converters.TokensDataConverter
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.domain.NetworkInfo
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * State builder creates a specific states for SwapScreen
 */
@Suppress("LargeClass")
internal class StateBuilder(
    private val actions: UiActions,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) {

    private val tokensDataConverter = TokensDataConverter(
        onSearchEntered = actions.onSearchEntered,
        onTokenSelected = actions.onTokenSelected,
        isBalanceHiddenProvider = isBalanceHiddenProvider,
        appCurrencyProvider = appCurrencyProvider,
    )

    fun createInitialLoadingState(initialCurrency: CryptoCurrency, networkInfo: NetworkInfo): SwapStateHolder {
        return SwapStateHolder(
            networkId = initialCurrency.network.backendId,
            blockchainId = networkInfo.blockchainId,
            sendCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.SendCard(actions.onAmountChanged, actions.onAmountSelected),
                amountEquivalent = null,
                amountTextFieldValue = null,
                tokenIconUrl = initialCurrency.iconUrl,
                tokenCurrency = initialCurrency.symbol,
                coinId = null,
                canSelectAnotherToken = false,
                isNotNativeToken = initialCurrency is CryptoCurrency.Token,
                balance = "",
                isBalanceHidden = true,
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amountEquivalent = null,
                tokenIconUrl = "",
                tokenCurrency = "",
                amountTextFieldValue = null,
                canSelectAnotherToken = false,
                balance = "",
                isNotNativeToken = false,
                coinId = null,
                isBalanceHidden = true,
            ),
            fee = FeeState.Loading,
            networkCurrency = networkInfo.blockchainCurrency,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
            onRefresh = {},
            onBackClicked = actions.onBackClicked,
            onChangeCardsClicked = actions.onChangeCardsClicked,
            onMaxAmountSelected = actions.onMaxAmountSelected,
            updateInProgress = true,
            onShowPermissionBottomSheet = actions.openPermissionBottomSheet,
            onCancelPermissionBottomSheet = actions.hidePermissionBottomSheet,
        )
    }

    fun createNoAvailableTokensToSwapState(
        uiStateHolder: SwapStateHolder,
        fromToken: CryptoCurrencyStatus,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amountTextFieldValue = TextFieldValue(
                    text = "0",
                ),
                amountEquivalent = "0 ${appCurrencyProvider.invoke().symbol}",
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                coinId = uiStateHolder.sendCardData.coinId,
                isNotNativeToken = uiStateHolder.sendCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                balance = fromToken.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.Empty(
                type = TransactionCardType.ReceiveCard(),
                amountEquivalent = "0 ${appCurrencyProvider.invoke().symbol}",
                amountTextFieldValue = TextFieldValue(
                    text = "0",
                ),
                canSelectAnotherToken = true,
            ),
            warnings = listOf(
                SwapWarning.NoAvailableTokensToSwap(
                    notificationConfig = NotificationConfig(
                        title = stringReference("No tokens"),
                        subtitle = stringReference("Swap tokens not available"),
                        iconResId = R.drawable.ic_alert_24,
                    ),
                ),
            ),
            fee = FeeState.Empty,
            swapButton = SwapButton(
                enabled = false,
                loading = false,
                onClick = { },
            ),
            updateInProgress = false,
        )
    }

    fun createQuotesLoadingState(
        uiStateHolder: SwapStateHolder,
        fromToken: CryptoCurrency,
        toToken: CryptoCurrency,
        mainTokenId: String,
    ): SwapStateHolder {
        val canSelectSendToken = mainTokenId != fromToken.id.value // TODO look at id matching
        val canSelectReceiveToken = mainTokenId != toToken.id.value // TODO look at id matching
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = null,
                tokenIconUrl = fromToken.iconUrl,
                tokenCurrency = fromToken.symbol,
                coinId = fromToken.id.value,
                isNotNativeToken = fromToken is CryptoCurrency.Token,
                canSelectAnotherToken = canSelectSendToken,
                balance = if (!canSelectSendToken) uiStateHolder.sendCardData.balance else "",
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amountTextFieldValue = null,
                amountEquivalent = null,
                tokenIconUrl = toToken.iconUrl,
                tokenCurrency = toToken.symbol,
                coinId = toToken.id.value,
                isNotNativeToken = toToken is CryptoCurrency.Token,
                canSelectAnotherToken = canSelectReceiveToken,
                balance = if (!canSelectReceiveToken) uiStateHolder.receiveCardData.balance else "",
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            fee = FeeState.Loading,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
            permissionState = uiStateHolder.permissionState,
            updateInProgress = true,
        )
    }

    /**
     * Create quotes loaded state
     *
     * @param uiStateHolder whole screen state
     * @param quoteModel data model
     * @param fromToken token data to swap
     * @param onFeeSetup callback for reset fee after auto update
     * @return updated whole screen state
     */
    @Suppress("LongMethod")
    fun createQuotesLoadedState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
        onFeeSetup: (TxFee) -> Unit,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val warnings = mutableListOf<SwapWarning>()
        if (!quoteModel.preparedSwapConfigState.isAllowedToSpend &&
            quoteModel.preparedSwapConfigState.isFeeEnough &&
            quoteModel.permissionState is PermissionDataState.PermissionReadyForRequest
        ) {
            warnings.add(
                SwapWarning.PermissionNeeded(
                    createPermissionNotificationConfig(fromToken.symbol),
                ),
            )
        }
        if (!quoteModel.preparedSwapConfigState.isBalanceEnough) {
            warnings.add(SwapWarning.InsufficientFunds)
        }

        if (quoteModel.priceImpact > PRICE_IMPACT_THRESHOLD) {
            warnings.add(
                SwapWarning.HighPriceImpact(
                    priceImpact = (quoteModel.priceImpact * HUNDRED_PERCENTS).toInt(),
                    notificationConfig = highPriceImpactNotificationConfig(),
                ),
            )
        }
        val feeState = createFeeState(quoteModel, uiStateHolder, onFeeSetup)
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = quoteModel.fromTokenInfo.tokenFiatBalance,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                coinId = quoteModel.fromTokenInfo.coinId,
                isNotNativeToken = uiStateHolder.sendCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                balance = quoteModel.fromTokenInfo.tokenWalletBalance,
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amountTextFieldValue = TextFieldValue(quoteModel.toTokenInfo.tokenAmount.formatToUIRepresentation()),
                amountEquivalent = quoteModel.toTokenInfo.tokenFiatBalance,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                coinId = quoteModel.toTokenInfo.coinId,
                isNotNativeToken = uiStateHolder.receiveCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                balance = quoteModel.toTokenInfo.tokenWalletBalance,
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            networkCurrency = quoteModel.networkCurrency,
            warnings = warnings,
            permissionState = convertPermissionState(
                lastPermissionState = uiStateHolder.permissionState,
                permissionDataState = quoteModel.permissionState,
                feeState = feeState,
                onGivePermissionClick = actions.onGivePermissionClick,
                onChangeApproveType = actions.onChangeApproveType,
            ),
            fee = feeState,
            swapButton = SwapButton(
                enabled = quoteModel.preparedSwapConfigState.isAllowedToSpend &&
                    quoteModel.preparedSwapConfigState.isBalanceEnough &&
                    quoteModel.preparedSwapConfigState.isFeeEnough,
                loading = false,
                onClick = actions.onSwapClick,
            ),
            updateInProgress = false,
        )
    }

    fun createQuotesEmptyAmountState(
        uiStateHolder: SwapStateHolder,
        emptyAmountState: SwapState.EmptyAmountState,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                coinId = uiStateHolder.sendCardData.coinId,
                isNotNativeToken = uiStateHolder.sendCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                balance = emptyAmountState.fromTokenWalletBalance,
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amountTextFieldValue = TextFieldValue("0"),
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                coinId = uiStateHolder.receiveCardData.coinId,
                isNotNativeToken = uiStateHolder.receiveCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                balance = emptyAmountState.toTokenWalletBalance,
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            warnings = emptyList(),
            fee = FeeState.Empty,
            swapButton = SwapButton(
                enabled = false,
                loading = false,
                onClick = { },
            ),
            updateInProgress = false,
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

    fun addTokensToState(uiState: SwapStateHolder, tokensDataState: CurrenciesGroup): SwapStateHolder {
        return uiState.copy(
            selectTokenState = tokensDataConverter.convert(
                value = tokensDataState,
            ),
        )
    }

    fun createSilentLoadState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            updateInProgress = true,
        )
    }

    fun updateSwapAmount(uiState: SwapStateHolder, amount: String): SwapStateHolder {
        if (uiState.sendCardData !is SwapCardState.SwapCardData) return uiState
        return uiState.copy(
            sendCardData = uiState.sendCardData.copy(
                amountTextFieldValue = TextFieldValue(
                    text = amount,
                    selection = TextRange(amount.length),
                ),
            ),
        )
    }

    fun updateBalanceHiddenState(uiState: SwapStateHolder, isBalanceHidden: Boolean): SwapStateHolder {
        if (uiState.sendCardData !is SwapCardState.SwapCardData) return uiState
        if (uiState.receiveCardData !is SwapCardState.SwapCardData) return uiState
        val patchedSendCardData = uiState.sendCardData.copy(
            isBalanceHidden = isBalanceHidden,
        )
        val patchedReceiveCardData = uiState.receiveCardData.copy(
            isBalanceHidden = isBalanceHidden,
        )
        val selectTokenState = uiState.selectTokenState?.copy(
            availableTokens = uiState.selectTokenState.availableTokens.map {
                when (it) {
                    is TokenToSelectState.TokenToSelect -> {
                        it.copy(
                            addedTokenBalanceData = it.addedTokenBalanceData?.copy(isBalanceHidden = isBalanceHidden),
                        )
                    }
                    is TokenToSelectState.Title -> {
                        it
                    }
                }
            }.toImmutableList(),
        )

        return uiState.copy(
            sendCardData = patchedSendCardData,
            receiveCardData = patchedReceiveCardData,
            selectTokenState = selectTokenState,
        )
    }

    fun updateApproveType(uiState: SwapStateHolder, approveType: ApproveType): SwapStateHolder {
        return if (uiState.permissionState is SwapPermissionState.ReadyForRequest) {
            uiState.copy(
                permissionState = uiState.permissionState.copy(
                    approveType = approveType,
                ),
            )
        } else {
            uiState
        }
    }

    fun updateFeeSelectedItem(uiState: SwapStateHolder, item: Item<TxFee>, isFeeEnough: Boolean): SwapStateHolder {
        val newSelectedItem = item.copy(
            startText = TextReference.Res(R.string.send_network_fee_title),
        )
        val permissionState = uiState.permissionState
        val newPermissionState = if (permissionState is SwapPermissionState.ReadyForRequest) {
            permissionState.copy(
                fee = newSelectedItem.endText,
            )
        } else {
            permissionState
        }
        val updateState = when (val fee = uiState.fee) {
            is FeeState.Loaded -> {
                getUpdatedFeeStateForEnoughFee(uiState, fee, item, newSelectedItem, newPermissionState, isFeeEnough)
            }
            is FeeState.NotEnoughFundsWarning -> {
                getUpdatedFeeStateForNotEnoughFee(uiState, fee, item, newSelectedItem, newPermissionState, isFeeEnough)
            }
            else -> uiState
        }
        return if (isFeeEnough) {
            updateState.copy(
                warnings = uiState.warnings.filterNot { it is SwapWarning.InsufficientFunds },
            )
        } else {
            updateState.copy(
                warnings = uiState.warnings.plus(SwapWarning.InsufficientFunds),
            )
        }
    }

    @Suppress("LongParameterList")
    private fun getUpdatedFeeStateForEnoughFee(
        uiState: SwapStateHolder,
        fee: FeeState.Loaded,
        itemToSelect: Item<TxFee>,
        newSelectedItem: Item<TxFee>,
        newPermissionState: SwapPermissionState,
        isFeeEnough: Boolean,
    ): SwapStateHolder {
        val newState = fee.state?.copy(
            selectedItem = newSelectedItem,
            items = selectNewItem(fee.state.items, itemToSelect),
        )
        val newFeeState = if (isFeeEnough) {
            fee.copy(state = newState)
        } else {
            FeeState.NotEnoughFundsWarning(
                tangemFee = fee.tangemFee,
                state = newState,
                onSelectItem = fee.onSelectItem,
            )
        }
        return uiState.copy(
            fee = newFeeState,
            permissionState = newPermissionState,
            swapButton = uiState.swapButton.copy(
                enabled = isFeeEnough,
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun getUpdatedFeeStateForNotEnoughFee(
        uiState: SwapStateHolder,
        fee: FeeState.NotEnoughFundsWarning,
        itemToSelect: Item<TxFee>,
        newSelectedItem: Item<TxFee>,
        newPermissionState: SwapPermissionState,
        isFeeEnough: Boolean,
    ): SwapStateHolder {
        val newState = fee.state?.copy(
            selectedItem = newSelectedItem,
            items = selectNewItem(fee.state.items, itemToSelect),
        )
        val newFeeState = if (isFeeEnough) {
            FeeState.Loaded(
                tangemFee = fee.tangemFee,
                state = newState,
                onSelectItem = fee.onSelectItem,
            )
        } else {
            fee.copy(state = newState)
        }
        return uiState.copy(
            fee = newFeeState,
            permissionState = newPermissionState,
            swapButton = uiState.swapButton.copy(
                enabled = isFeeEnough,
            ),
        )
    }

    private fun createFeeState(
        quoteModel: SwapState.QuotesLoadedState,
        uiStateHolder: SwapStateHolder,
        onFeeSetup: (TxFee) -> Unit,
    ): FeeState {
        val previousFeeState = when (val stateFee = uiStateHolder.fee) {
            is FeeState.Loaded -> stateFee.state
            is FeeState.NotEnoughFundsWarning -> stateFee.state
            else -> null
        }
        val permissionState = quoteModel.permissionState
        val feeState = if (permissionState is PermissionDataState.PermissionReadyForRequest) {
            permissionState.requestApproveData.fee
        } else {
            quoteModel.swapDataModel?.fee
        }
        val selectFeeState = createSelectFeeState(
            fee = feeState,
            previousState = previousFeeState,
            onFeeSetup = onFeeSetup,
        )
        return if (quoteModel.preparedSwapConfigState.isFeeEnough) {
            FeeState.Loaded(
                tangemFee = quoteModel.tangemFee,
                state = selectFeeState,
                onSelectItem = actions.onSelectItemFee,
            )
        } else {
            FeeState.NotEnoughFundsWarning(
                tangemFee = quoteModel.tangemFee,
                state = selectFeeState,
                onSelectItem = actions.onSelectItemFee,
            )
        }
    }

    private fun selectNewItem(items: ImmutableList<Item<TxFee>>, selectItem: Item<TxFee>): ImmutableList<Item<TxFee>> {
        return items.map {
            if (it.id == selectItem.id) {
                it.copy(isSelected = true)
            } else {
                it.copy(isSelected = false)
            }
        }.toImmutableList()
    }

    fun loadingPermissionState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            permissionState = SwapPermissionState.InProgress,
            warnings = uiState.warnings.filterNot { it is SwapWarning.PermissionNeeded },
        )
    }

    fun createSuccessState(
        uiState: SwapStateHolder,
        txState: TxState.TxSent,
        onSecondaryBtnClick: () -> Unit,
    ): SwapStateHolder {
        return uiState.copy(
            successState = SwapSuccessStateHolder(
                fromTokenAmount = txState.fromAmount ?: "",
                toTokenAmount = txState.toAmount ?: "",
                onSecondaryButtonClick = onSecondaryBtnClick,
            ),
        )
    }

    fun createErrorTransaction(uiState: SwapStateHolder, txState: TxState, onAlertClick: () -> Unit): SwapStateHolder {
        return uiState.copy(
            alert = SwapWarning.GenericWarning(
                message = null,
                onClick = onAlertClick,
                type = if (txState is TxState.NetworkError) GenericWarningType.NETWORK else GenericWarningType.OTHER,
            ),
            updateInProgress = false,
        )
    }

    fun mapError(uiState: SwapStateHolder, error: DataError, onClick: () -> Unit): SwapStateHolder {
        return when (error) {
            // todo use if needed later
            // DataError.InsufficientLiquidity -> TODO()
            // DataError.NoError -> TODO()
            is DataError.UnknownError -> addWarning(uiState, error.message, true, onClick)
            else -> addWarning(uiState, null, false) {}
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

    fun clearAlert(uiState: SwapStateHolder): SwapStateHolder = uiState.copy(alert = null)

    fun addWarning(
        uiState: SwapStateHolder,
        message: String?,
        shouldWrapMessage: Boolean = false,
        onClick: () -> Unit,
    ): SwapStateHolder {
        val renewWarnings = uiState.warnings.filterNot { it is SwapWarning.GenericWarning }.toMutableList()
        renewWarnings.add(
            SwapWarning.GenericWarning(
                message = message,
                shouldWrapMessage = shouldWrapMessage,
                onClick = onClick,
            ),
        )
        return uiState.copy(
            warnings = renewWarnings,
        )
    }

    private fun convertPermissionState(
        lastPermissionState: SwapPermissionState,
        permissionDataState: PermissionDataState,
        feeState: FeeState,
        onGivePermissionClick: () -> Unit,
        onChangeApproveType: (ApproveType) -> Unit,
    ): SwapPermissionState {
        val approveType = if (lastPermissionState is SwapPermissionState.ReadyForRequest) {
            lastPermissionState.approveType
        } else {
            ApproveType.UNLIMITED
        }
        val fee = when (feeState) {
            is FeeSelectState -> feeState.state?.selectedItem?.endText
            else -> null
        }
        return when (permissionDataState) {
            PermissionDataState.Empty -> SwapPermissionState.Empty
            PermissionDataState.PermissionFailed -> SwapPermissionState.Empty
            PermissionDataState.PermissionLoading -> SwapPermissionState.InProgress
            is PermissionDataState.PermissionReadyForRequest -> SwapPermissionState.ReadyForRequest(
                currency = permissionDataState.currency,
                amount = permissionDataState.amount,
                approveType = approveType,
                walletAddress = getShortAddressValue(permissionDataState.walletAddress),
                spenderAddress = getShortAddressValue(permissionDataState.spenderAddress),
                fee = fee ?: TextReference.Str(""),
                approveButton = ApprovePermissionButton(
                    enabled = true,
                    onClick = onGivePermissionClick,
                ),
                cancelButton = CancelPermissionButton(
                    enabled = true,
                ),
                onChangeApproveType = onChangeApproveType,
            )
        }
    }

    private fun createSelectFeeState(
        fee: TxFeeState?,
        previousState: SelectableItemsState<TxFee>?,
        onFeeSetup: (TxFee) -> Unit,
    ): SelectableItemsState<TxFee>? {
        if (fee == null) return null
        if (previousState == null) {
            onFeeSetup.invoke(fee.normalFee) // if there is no previous state, setup normal fee by default
            val selectedItemId = 0
            // by default preselect normal
            val preselectedItem = Item(
                id = selectedItemId,
                startText = TextReference.Res(R.string.send_network_fee_title),
                endText = TextReference.Str(fee.normalFee.feeCryptoFormatted + fee.normalFee.feeFiatFormatted),
                isSelected = true,
                data = fee.normalFee,
            )
            val feeItems = mutableListOf<Item<TxFee>>()
            val normalFeeItem = Item(
                id = selectedItemId,
                startText = TextReference.Res(R.string.send_fee_picker_normal),
                endText = TextReference.Str(fee.normalFee.feeCryptoFormatted + fee.normalFee.feeFiatFormatted),
                isSelected = true,
                data = fee.normalFee,
            )
            val priorityFeeItem = Item(
                id = 1,
                startText = TextReference.Res(R.string.send_fee_picker_priority),
                endText = TextReference.Str(fee.priorityFee.feeCryptoFormatted + fee.priorityFee.feeFiatFormatted),
                isSelected = false,
                data = fee.priorityFee,
            )
            feeItems.add(normalFeeItem)
            feeItems.add(priorityFeeItem)
            return SelectableItemsState(
                selectedItem = preselectedItem,
                items = feeItems.toImmutableList(),
            )
        } else {
            val normalFeeItem =
                requireNotNull(previousState.items.firstOrNull()) { "in previousState there are 2 items" }
                    .copy(
                        endText = TextReference.Str(fee.normalFee.feeCryptoFormatted + fee.normalFee.feeFiatFormatted),
                    )
            val priorityFeeItem =
                requireNotNull(previousState.items.getOrNull(1)) { "in previousState there are 2 items" }
                    .copy(
                        endText = TextReference.Str(
                            fee.priorityFee.feeCryptoFormatted + fee.priorityFee.feeFiatFormatted,
                        ),
                    )
            val selectedEndText = if (normalFeeItem.isSelected) {
                onFeeSetup.invoke(fee.normalFee)
                normalFeeItem.endText
            } else {
                onFeeSetup.invoke(fee.priorityFee)
                priorityFeeItem.endText
            }
            return previousState.copy(
                selectedItem = previousState.selectedItem.copy(
                    endText = selectedEndText,
                ),
                items = listOf(normalFeeItem, priorityFeeItem).toImmutableList(),
            )
        }
    }

    private fun createPermissionNotificationConfig(fromTokenSymbol: String): NotificationConfig {
        return NotificationConfig(
            title = resourceReference(R.string.swapping_permission_header),
            subtitle = resourceReference(
                id = R.string.swapping_permission_subheader,
                formatArgs = wrappedList(fromTokenSymbol),
            ),
            iconResId = R.drawable.ic_locked_24,
        )
    }

    private fun highPriceImpactNotificationConfig(): NotificationConfig {
        return NotificationConfig(
            title = resourceReference(R.string.swapping_high_price_impact),
            subtitle = resourceReference(R.string.swapping_high_price_impact_description),
            iconResId = R.drawable.ic_locked_24,
        )
    }

    private fun getShortAddressValue(fullAddress: String): String {
        check(fullAddress.length > ADDRESS_MIN_LENGTH) { "Invalid address" }
        val firstAddressPart = fullAddress.substring(startIndex = 0, endIndex = ADDRESS_FIRST_PART_LENGTH)
        val secondAddressPart = fullAddress.substring(
            startIndex = fullAddress.length - ADDRESS_SECOND_PART_LENGTH,
            endIndex = fullAddress.length,
        )
        return "$firstAddressPart...$secondAddressPart"
    }

    private fun CryptoCurrencyStatus.getFormattedAmount(): String {
        val amount = value.amount ?: return UNKNOWN_AMOUNT_SIGN

        return BigDecimalFormatter.formatCryptoAmount(amount, currency.symbol, currency.decimals)
    }

    @Suppress("UnusedPrivateMember")
    private fun CryptoCurrencyStatus.getFormattedFiatAmount(): String {
        val fiatAmount = value.fiatAmount ?: return UNKNOWN_AMOUNT_SIGN
        val appCurrency = appCurrencyProvider()

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
    }

    private companion object {
        const val ADDRESS_MIN_LENGTH = 11
        const val ADDRESS_FIRST_PART_LENGTH = 7
        const val ADDRESS_SECOND_PART_LENGTH = 4
        private const val PRICE_IMPACT_THRESHOLD = 0.1
        private const val HUNDRED_PERCENTS = 100
        private const val UNKNOWN_AMOUNT_SIGN = "—"
    }
}