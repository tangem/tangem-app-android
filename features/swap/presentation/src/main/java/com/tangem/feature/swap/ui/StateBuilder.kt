package com.tangem.feature.swap.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.Provider
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.notifications.NotificationConfig
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
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.*
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.viewmodels.SwapProcessDataState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * State builder creates a specific states for SwapScreen
 */
@Suppress("LargeClass", "TooManyFunctions")
internal class StateBuilder(
    private val actions: UiActions,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) {

    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

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
                token = null,
                tokenIconUrl = initialCurrency.iconUrl,
                tokenCurrency = initialCurrency.symbol,
                coinId = initialCurrency.network.backendId,
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
                token = null,
                amountTextFieldValue = null,
                canSelectAnotherToken = false,
                balance = "",
                isNotNativeToken = false,
                coinId = null,
                isBalanceHidden = true,
            ),
            fee = FeeItemState.Empty,
            networkCurrency = networkInfo.blockchainCurrency,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
            onRefresh = {},
            onBackClicked = actions.onBackClicked,
            onChangeCardsClicked = actions.onChangeCardsClicked,
            onMaxAmountSelected = actions.onMaxAmountSelected,
            updateInProgress = true,
            onShowPermissionBottomSheet = actions.openPermissionBottomSheet,
            providerState = ProviderState.Empty(),
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
                token = fromToken,
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
            fee = FeeItemState.Empty,
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
        val canSelectSendToken = mainTokenId != fromToken.id.value
        val canSelectReceiveToken = mainTokenId != toToken.id.value
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = null,
                token = uiStateHolder.sendCardData.token,
                tokenIconUrl = fromToken.iconUrl,
                tokenCurrency = fromToken.symbol,
                coinId = fromToken.network.backendId,
                isNotNativeToken = fromToken is CryptoCurrency.Token,
                canSelectAnotherToken = canSelectSendToken,
                balance = if (!canSelectSendToken) uiStateHolder.sendCardData.balance else "",
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amountTextFieldValue = null,
                amountEquivalent = null,
                token = uiStateHolder.receiveCardData.token,
                tokenIconUrl = toToken.iconUrl,
                tokenCurrency = toToken.symbol,
                coinId = toToken.network.backendId,
                isNotNativeToken = toToken is CryptoCurrency.Token,
                canSelectAnotherToken = canSelectReceiveToken,
                balance = if (!canSelectReceiveToken) uiStateHolder.receiveCardData.balance else "",
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            fee = FeeItemState.Empty,
            swapButton = SwapButton(enabled = false, loading = true, onClick = {}),
            providerState = ProviderState.Loading(),
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
     * @return updated whole screen state
     */
    @Suppress("LongMethod")
    fun createQuotesLoadedState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
        swapProvider: SwapProvider,
        selectedFeeType: FeeType,
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
        val feeState = createFeeState(quoteModel.txFee, selectedFeeType)
        val fromCurrencyStatus = quoteModel.fromTokenInfo.cryptoCurrencyStatus
        val toCurrencyStatus = quoteModel.toTokenInfo.cryptoCurrencyStatus
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.SendCard),
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = getFormattedFiatAmount(quoteModel.fromTokenInfo.amountFiat),
                token = fromCurrencyStatus,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                coinId = fromCurrencyStatus.currency.network.backendId,
                isNotNativeToken = uiStateHolder.sendCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                balance = fromCurrencyStatus.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amountTextFieldValue = TextFieldValue(quoteModel.toTokenInfo.tokenAmount.formatToUIRepresentation()),
                amountEquivalent = getFormattedFiatAmount(quoteModel.toTokenInfo.amountFiat),
                token = toCurrencyStatus,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                coinId = toCurrencyStatus.currency.network.backendId,
                isNotNativeToken = uiStateHolder.receiveCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                balance = toCurrencyStatus.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            networkCurrency = quoteModel.networkCurrency,
            warnings = warnings,
            permissionState = convertPermissionState(
                lastPermissionState = uiStateHolder.permissionState,
                permissionDataState = quoteModel.permissionState,
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
            providerState = swapProvider.convertToContentClickableProviderState(
                fromTokenInfo = quoteModel.fromTokenInfo,
                toTokenInfo = quoteModel.toTokenInfo,
                selectionType = ProviderState.SelectionType.CLICK,
                onProviderClick = actions.onProviderClick,
            ),
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
                token = uiStateHolder.sendCardData.token,
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
                token = uiStateHolder.receiveCardData.token,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                coinId = uiStateHolder.receiveCardData.coinId,
                isNotNativeToken = uiStateHolder.receiveCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                balance = emptyAmountState.toTokenWalletBalance,
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            warnings = emptyList(),
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                enabled = false,
                loading = false,
                onClick = { },
            ),
            updateInProgress = false,
            providerState = ProviderState.Empty(),
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
        val config = uiState.bottomSheetConfig?.content as? GivePermissionBottomSheetConfig
        return if (config != null) {
            uiState.copy(
                bottomSheetConfig = uiState.bottomSheetConfig.copy(
                    content = config.copy(
                        data = config.data.copy(approveType = approveType),
                    ),
                ),
            )
        } else {
            uiState
        }
    }

    private fun createFeeState(txFeeState: TxFeeState, feeType: FeeType): FeeItemState {
        val isClickable: Boolean
        val fee = when (txFeeState) {
            TxFeeState.Empty -> return FeeItemState.Empty
            is TxFeeState.SingleFeeState -> {
                isClickable = false
                txFeeState.fee
            }
            is TxFeeState.MultipleFeeState -> {
                isClickable = true
                when (feeType) {
                    FeeType.NORMAL -> {
                        txFeeState.normalFee
                    }
                    FeeType.PRIORITY -> {
                        txFeeState.priorityFee
                    }
                }
            }
        }

        return FeeItemState.Content(
            feeType = feeType,
            title = stringReference("Fee"), // todo replace with string
            amountCrypto = fee.feeCryptoFormatted,
            symbolCrypto = fee.cryptoSymbol,
            amountFiatFormatted = fee.feeFiatFormatted,
            isClickable = isClickable,
            onClick = actions.onClickFee,
        )
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
        dataState: SwapProcessDataState,
        txUrl: String,
        onSecondaryBtnClick: () -> Unit,
    ): SwapStateHolder {
        val fromToken = requireNotNull(uiState.sendCardData as? SwapCardState.SwapCardData)
        val toToken = requireNotNull(uiState.receiveCardData as? SwapCardState.SwapCardData)
        val fromTokenIconState = fromToken.token?.let(iconStateConverter::convert)
        val toTokenIconState = toToken.token?.let(iconStateConverter::convert)
        val fee = uiState.fee as? FeeItemState.Content ?: return uiState
        val fromCryptoCurrencyStatus = requireNotNull(fromToken.token)
        val toCryptoCurrencyStatus = requireNotNull(toToken.token)
        val rate = txState.toAmount?.toBigDecimal()?.divide(
            txState.fromAmount?.toBigDecimal(),
            toCryptoCurrencyStatus.currency.decimals,
            RoundingMode.HALF_UP,
        )
        val fromCurrencySymbol = fromCryptoCurrencyStatus.currency.symbol
        val toCurrencySymbol = toCryptoCurrencyStatus.currency.symbol

        return uiState.copy(
            successState = SwapSuccessStateHolder(
                timestamp = System.currentTimeMillis(),
                txUrl = txUrl,
                selectedProvider = requireNotNull(dataState.selectedProvider),
                fee = TextReference.Str("${fee.amountCrypto} ${fee.symbolCrypto} (${fee.amountFiatFormatted})"),
                rate = TextReference.Str("1 $fromCurrencySymbol ≈ $rate $toCurrencySymbol"),
                fromTokenAmount = TextReference.Str("${txState.fromAmount.orEmpty()} ${fromToken.tokenCurrency}}"),
                toTokenAmount = TextReference.Str("${txState.toAmount.orEmpty()} ${toToken.tokenCurrency}}"),
                fromTokenFiatAmount = TextReference.Str(fromToken.amountEquivalent.orEmpty()),
                toTokenFiatAmount = TextReference.Str(toToken.amountEquivalent.orEmpty()),
                fromTokenIconState = fromTokenIconState,
                toTokenIconState = toTokenIconState,
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
// [REDACTED_TODO_COMMENT]
            // DataError.InsufficientLiquidity -> TODO()
            // DataError.NoError -> TODO()
            is DataError.ExchangeTooSmallAmountError -> addWarning(uiState, error.amount.toString(), true, onClick)
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
        onGivePermissionClick: () -> Unit,
        onChangeApproveType: (ApproveType) -> Unit,
    ): SwapPermissionState {
        val approveType = if (lastPermissionState is SwapPermissionState.ReadyForRequest) {
            lastPermissionState.approveType
        } else {
            ApproveType.UNLIMITED
        }
        return when (permissionDataState) {
            PermissionDataState.Empty -> SwapPermissionState.Empty
            PermissionDataState.PermissionFailed -> SwapPermissionState.Empty
            PermissionDataState.PermissionLoading -> SwapPermissionState.InProgress
            is PermissionDataState.PermissionReadyForRequest -> {
                val permissionFee = when (val fee = permissionDataState.requestApproveData.fee) {
                    TxFeeState.Empty -> error("Fee shouldn't be empty")
                    is TxFeeState.MultipleFeeState -> fee.priorityFee
                    is TxFeeState.SingleFeeState -> fee.fee
                }
                SwapPermissionState.ReadyForRequest(
                    currency = permissionDataState.currency,
                    amount = permissionDataState.amount,
                    approveType = approveType,
                    walletAddress = getShortAddressValue(permissionDataState.walletAddress),
                    spenderAddress = getShortAddressValue(permissionDataState.spenderAddress),
                    fee = TextReference.Str("${permissionFee.feeCryptoFormatted} (${permissionFee.feeFiatFormatted})"),
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
    }

    fun showPermissionBottomSheet(uiState: SwapStateHolder, onDismiss: () -> Unit): SwapStateHolder {
        val permissionState = uiState.permissionState
        if (permissionState is SwapPermissionState.ReadyForRequest) {
            val config = GivePermissionBottomSheetConfig(
                data = permissionState,
                onCancel = onDismiss,
            )
            return uiState.copy(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShow = true,
                    onDismissRequest = onDismiss,
                    content = config,
                ),
            )
        }
        return uiState
    }

    fun dismissBottomSheet(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            bottomSheetConfig = uiState.bottomSheetConfig?.copy(isShow = false),
        )
    }

    fun showSelectProviderBottomSheet(
        uiState: SwapStateHolder,
        selectedProviderId: String,
        providersStates: Map<SwapProvider, SwapState>,
        onDismiss: () -> Unit,
    ): SwapStateHolder {
        val config = ChooseProviderBottomSheetConfig(
            selectedProviderId = selectedProviderId,
            providers = providersStates.entries
                .mapNotNull { it.convertToProviderState(actions.onProviderSelect) }
                .toImmutableList(),
        )
        return uiState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = onDismiss,
                content = config,
            ),
        )
    }

    fun updateSelectedProvider(uiState: SwapStateHolder, selectedProviderId: String): SwapStateHolder {
        val config = uiState.bottomSheetConfig?.content as? ChooseProviderBottomSheetConfig
        return if (config != null) {
            uiState.copy(
                bottomSheetConfig = uiState.bottomSheetConfig.copy(
                    content = config.copy(
                        selectedProviderId = selectedProviderId,
                    ),
                ),
            )
        } else {
            uiState
        }
    }

    fun showSelectFeeBottomSheet(
        uiState: SwapStateHolder,
        selectedFee: FeeType,
        txFeeState: TxFeeState.MultipleFeeState,
        onDismiss: () -> Unit,
    ): SwapStateHolder {
        val config = ChooseFeeBottomSheetConfig(
            selectedFee = selectedFee,
            onSelectFeeType = {
                val selectedItem = when (it) {
                    FeeType.NORMAL -> txFeeState.normalFee
                    FeeType.PRIORITY -> txFeeState.priorityFee
                }
                actions.onSelectFeeType.invoke(selectedItem)
            },
            feeItems = txFeeState.toFeeItemState(),
        )
        return uiState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = onDismiss,
                content = config,
            ),
        )
    }

    fun updateSelectedFee(uiState: SwapStateHolder, selectedFee: FeeType): SwapStateHolder {
        val config = uiState.bottomSheetConfig?.content as? ChooseFeeBottomSheetConfig
        return if (config != null) {
            uiState.copy(
                bottomSheetConfig = uiState.bottomSheetConfig.copy(
                    content = config.copy(
                        selectedFee = selectedFee,
                    ),
                ),
            )
        } else {
            uiState
        }
    }

    private fun TxFeeState.MultipleFeeState.toFeeItemState(): ImmutableList<FeeItemState.Content> {
        return listOf(
            FeeItemState.Content(
                feeType = this.normalFee.feeType,
                title = stringReference("Fee"), // todo replace with string
                amountCrypto = this.normalFee.feeCryptoFormatted,
                symbolCrypto = this.normalFee.cryptoSymbol,
                amountFiatFormatted = this.normalFee.feeFiatFormatted,
                isClickable = true,
                onClick = {},
            ),
            FeeItemState.Content(
                feeType = this.priorityFee.feeType,
                title = stringReference("Fee"), // todo replace with string
                amountCrypto = this.priorityFee.feeCryptoFormatted,
                symbolCrypto = this.priorityFee.cryptoSymbol,
                amountFiatFormatted = this.priorityFee.feeFiatFormatted,
                isClickable = true,
                onClick = {},
            ),
        ).toImmutableList()
    }

    private fun Map.Entry<SwapProvider, SwapState>.convertToProviderState(
        onProviderSelect: (String) -> Unit,
    ): ProviderState? {
        val provider = this.key
        return when (val state = this.value) {
            is SwapState.EmptyAmountState -> null
            is SwapState.QuotesLoadedState -> provider.convertToContentClickableProviderState(
                fromTokenInfo = state.fromTokenInfo,
                toTokenInfo = state.toTokenInfo,
                onProviderClick = onProviderSelect,
                selectionType = ProviderState.SelectionType.SELECT,
            )
            is SwapState.SwapError -> null
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
            iconResId = R.drawable.ic_alert_circle_24,
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

    private fun SwapProvider.convertToContentClickableProviderState(
        fromTokenInfo: TokenSwapInfo,
        toTokenInfo: TokenSwapInfo,
        selectionType: ProviderState.SelectionType,
        onProviderClick: (String) -> Unit,
    ): ProviderState {
        val rate = toTokenInfo.tokenAmount.value.divide(
            fromTokenInfo.tokenAmount.value,
            toTokenInfo.cryptoCurrencyStatus.currency.decimals,
            RoundingMode.HALF_UP,
        )
        val fromCurrencySymbol = fromTokenInfo.cryptoCurrencyStatus.currency.symbol
        val toCurrencySymbol = toTokenInfo.cryptoCurrencyStatus.currency.symbol
        val rateString = "1 $fromCurrencySymbol ≈ $rate $toCurrencySymbol"
        return ProviderState.Content(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.toString(),
            rate = rateString,
            additionalBadge = ProviderState.AdditionalBadge.BestTrade,
            selectionType = selectionType,
            percentLowerThenBest = null,
            onProviderClick = onProviderClick,
        )
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

    private fun getFormattedFiatAmount(amount: BigDecimal): String {
        val appCurrency = appCurrencyProvider()

        return BigDecimalFormatter.formatFiatAmount(amount, appCurrency.code, appCurrency.symbol)
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
