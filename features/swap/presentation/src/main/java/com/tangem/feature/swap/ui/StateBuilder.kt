package com.tangem.feature.swap.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.tokenicon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.converters.TokensDataConverter
import com.tangem.feature.swap.domain.models.DataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.formatToUIRepresentation
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.*
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.viewmodels.SwapProcessDataState
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

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
                networkIconRes = getActiveIconRes(initialCurrency.network.id.value),
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
                networkIconRes = null,
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
                networkIconRes = getActiveIconRes(fromToken.currency.network.id.value),
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
                        title = resourceReference(R.string.warning_express_no_exchangeable_coins_title),
                        subtitle = resourceReference(
                            id = R.string.warning_express_no_exchangeable_coins_description,
                            formatArgs = wrappedList(fromToken.currency.name),
                        ),
                        iconResId = R.drawable.img_attention_20,
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
                networkIconRes = getActiveIconRes(fromToken.network.id.value),
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
                networkIconRes = getActiveIconRes(toToken.network.id.value),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            warnings = emptyList(),
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
    @Suppress("LongMethod", "LongParameterList")
    fun createQuotesLoadedState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
        swapProvider: SwapProvider,
        bestRatedProviderId: String,
        isManyProviders: Boolean,
        selectedFeeType: FeeType,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val warnings = getWarningsForSuccessState(quoteModel, fromToken)
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
                networkIconRes = uiStateHolder.sendCardData.networkIconRes,
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
                networkIconRes = uiStateHolder.receiveCardData.networkIconRes,
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
                enabled = getSwapButtonEnabled(quoteModel.preparedSwapConfigState),
                loading = false,
                onClick = actions.onSwapClick,
            ),
            updateInProgress = false,
            providerState = swapProvider.convertToContentClickableProviderState(
                isBestRate = bestRatedProviderId == swapProvider.providerId,
                fromTokenInfo = quoteModel.fromTokenInfo,
                toTokenInfo = quoteModel.toTokenInfo,
                isNeedBadge = isManyProviders,
                selectionType = ProviderState.SelectionType.CLICK,
                onProviderClick = actions.onProviderClick,
            ),
        )
    }

    private fun getWarningsForSuccessState(
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
    ): List<SwapWarning> {
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
        when (quoteModel.preparedSwapConfigState.includeFeeInAmount) {
            is IncludeFeeInAmount.Included ->
                warnings.add(
                    SwapWarning.GeneralWarning(
                        createNetworkFeeCoverageNotificationConfig(),
                    ),
                )
            else -> Unit
        }
        if (!quoteModel.preparedSwapConfigState.isFeeEnough &&
            quoteModel.preparedSwapConfigState.isBalanceEnough
        ) {
            warnings.add(
                SwapWarning.UnableToCoverFeeWarning(
                    createUnableToCoverFeeNotificationConfig(
                        fromToken = fromToken,
                        onBuyClick = actions.onBuyClick,
                    ),
                ),
            )
        }
        // check isBalanceEnough, but for dex includeFeeInAmount always Excluded
        if (!quoteModel.preparedSwapConfigState.isBalanceEnough &&
            quoteModel.preparedSwapConfigState.includeFeeInAmount !is IncludeFeeInAmount.Included
        ) {
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
        return warnings
    }

    private fun getSwapButtonEnabled(preparedSwapConfigState: PreparedSwapConfigState): Boolean {
        return when (preparedSwapConfigState.includeFeeInAmount) {
            IncludeFeeInAmount.BalanceNotEnough -> false
            IncludeFeeInAmount.Excluded ->
                preparedSwapConfigState.isAllowedToSpend &&
                    preparedSwapConfigState.isBalanceEnough &&
                    preparedSwapConfigState.isFeeEnough
            is IncludeFeeInAmount.Included -> true
        }
    }

    fun createQuotesErrorState(
        uiStateHolder: SwapStateHolder,
        swapProvider: SwapProvider,
        fromToken: TokenSwapInfo,
        toToken: CryptoCurrencyStatus?,
        dataError: DataError,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val warning = getWarningForError(dataError, fromToken.cryptoCurrencyStatus.currency)
        val providerState = getProviderStateForError(
            swapProvider = swapProvider,
            fromToken = fromToken.cryptoCurrencyStatus.currency,
            dataError = dataError,
            onProviderClick = actions.onProviderClick,
            selectionType = ProviderState.SelectionType.CLICK,
        )
        val receiveCardData = toToken?.let {
            SwapCardState.SwapCardData(
                type = TransactionCardType.ReceiveCard(),
                amountTextFieldValue = TextFieldValue(
                    text = "0",
                ),
                amountEquivalent = "0 ${appCurrencyProvider.invoke().symbol}",
                token = toToken,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                coinId = toToken.currency.network.backendId,
                isNotNativeToken = uiStateHolder.receiveCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                networkIconRes = uiStateHolder.receiveCardData.networkIconRes,
                balance = toToken.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
            )
        } ?: SwapCardState.Empty(
            type = TransactionCardType.ReceiveCard(),
            amountEquivalent = "0 ${appCurrencyProvider.invoke().symbol}",
            amountTextFieldValue = TextFieldValue(
                text = "0",
            ),
            canSelectAnotherToken = true,
        )
        return uiStateHolder.copy(
            sendCardData = uiStateHolder.sendCardData.copy(
                amountEquivalent = getFormattedFiatAmount(fromToken.amountFiat),
            ),
            receiveCardData = receiveCardData,
            warnings = listOf(warning),
            permissionState = SwapPermissionState.Empty,
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                enabled = false,
                loading = false,
                onClick = actions.onSwapClick,
            ),
            updateInProgress = false,
            providerState = providerState,
        )
    }

    private fun getProviderStateForError(
        swapProvider: SwapProvider,
        fromToken: CryptoCurrency,
        dataError: DataError,
        onProviderClick: (String) -> Unit,
        selectionType: ProviderState.SelectionType,
    ): ProviderState {
        return when (dataError) {
            is DataError.ExchangeTooSmallAmountError -> {
                swapProvider.convertToAvailableFromProviderState(
                    alertText = resourceReference(
                        R.string.express_provider_min_amount,
                        wrappedList(dataError.amount.getFormattedCryptoAmount(fromToken)),
                    ),
                    selectionType = selectionType,
                    onProviderClick = onProviderClick,
                )
            }
            else -> {
                ProviderState.Empty()
            }
        }
    }

    private fun getWarningForError(dataError: DataError, fromToken: CryptoCurrency): SwapWarning {
        return when (dataError) {
            is DataError.ExchangeTooSmallAmountError -> SwapWarning.TooSmallAmountWarning(
                notificationConfig = NotificationConfig(
                    title = resourceReference(
                        id = R.string.warning_express_too_minimal_amount_title,
                        formatArgs = wrappedList(dataError.amount.getFormattedCryptoAmount(fromToken)),
                    ),
                    subtitle = resourceReference(R.string.warning_express_too_minimal_amount_description),
                    iconResId = R.drawable.ic_alert_circle_24,
                ),
            )
            is DataError.UnknownError -> SwapWarning.GeneralWarning(
                notificationConfig = NotificationConfig(
                    title = resourceReference(R.string.common_error),
                    subtitle = resourceReference(R.string.swapping_generic_error),
                    iconResId = R.drawable.img_attention_20,
                    buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                        text = resourceReference(R.string.warning_button_refresh),
                        onClick = actions.onRetryClick,
                    ),
                ),
            )
            else -> SwapWarning.GeneralWarning(
                notificationConfig = NotificationConfig(
                    title = resourceReference(R.string.common_error),
                    subtitle = resourceReference(R.string.generic_error_code, wrappedList(dataError.code.toString())),
                    iconResId = R.drawable.img_attention_20,
                ),
            )
        }
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
                networkIconRes = uiStateHolder.sendCardData.networkIconRes,
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
                networkIconRes = uiStateHolder.receiveCardData.networkIconRes,
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

    fun createInitialErrorState(uiState: SwapStateHolder, onRefreshClick: () -> Unit): SwapStateHolder {
        return uiState.copy(
            warnings = listOf(
                SwapWarning.GeneralWarning(
                    notificationConfig = NotificationConfig(
                        title = TextReference.Res(R.string.warning_express_refresh_required_title),
                        subtitle = TextReference.EMPTY,
                        iconResId = R.drawable.ic_alert_circle_24,
                        buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                            text = TextReference.Res(R.string.warning_button_refresh),
                            onClick = onRefreshClick,
                        ),
                    ),
                ),
            ),
        )
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
            title = resourceReference(R.string.common_fee_label),
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

    @Suppress("LongParameterList")
    fun createSuccessState(
        uiState: SwapStateHolder,
        timeStamp: Long,
        txUrl: String,
        dataState: SwapProcessDataState,
        onExploreClick: () -> Unit,
        onStatusClick: () -> Unit,
    ): SwapStateHolder {
        val fee = requireNotNull(dataState.selectedFee)
        val fromCryptoCurrency = requireNotNull(dataState.fromCryptoCurrency)
        val toCryptoCurrency = requireNotNull(dataState.toCryptoCurrency)
        val fromAmount = requireNotNull(dataState.amount?.toBigDecimal())
        val toAmount = requireNotNull(dataState.swapDataModel?.toTokenAmount?.value)
        val providerState = uiState.providerState as ProviderState.Content

        val fromCryptoAmount = BigDecimalFormatter.formatCryptoAmount(fromAmount, fromCryptoCurrency.currency)
        val toCryptoAmount = BigDecimalFormatter.formatCryptoAmount(toAmount, toCryptoCurrency.currency)
        val fromFiatAmount = getFormattedFiatAmount(fromCryptoCurrency.value.fiatRate?.multiply(fromAmount))
        val toFiatAmount = getFormattedFiatAmount(toCryptoCurrency.value.fiatRate?.multiply(toAmount))

        return uiState.copy(
            successState = SwapSuccessStateHolder(
                timestamp = timeStamp,
                txUrl = txUrl,
                providerName = stringReference(providerState.name),
                providerType = stringReference(providerState.type),
                showStatusButton = providerState.type == ExchangeProviderType.CEX.name,
                providerIcon = providerState.iconUrl,
                rate = providerState.subtitle,
                fee = stringReference("${fee.feeCryptoFormatted} (${fee.feeFiatFormatted})"),
                fromTokenAmount = stringReference(fromCryptoAmount),
                toTokenAmount = stringReference(toCryptoAmount),
                fromTokenFiatAmount = stringReference(fromFiatAmount),
                toTokenFiatAmount = stringReference(toFiatAmount),
                fromTokenIconState = iconStateConverter.convert(fromCryptoCurrency),
                toTokenIconState = iconStateConverter.convert(toCryptoCurrency),
                onExploreButtonClick = onExploreClick,
                onStatusButtonClick = onStatusClick,
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

    @Suppress("LongParameterList")
    fun showSelectProviderBottomSheet(
        uiState: SwapStateHolder,
        selectedProviderId: String,
        pricesLowerBest: Map<SwapProvider, Float>,
        providersStates: Map<SwapProvider, SwapState>,
        unavailableProviders: List<SwapProvider>,
        onDismiss: () -> Unit,
    ): SwapStateHolder {
        val availableProvidersStates = providersStates.entries
            .mapNotNull {
                it.convertToProviderBottomSheetState(pricesLowerBest, actions.onProviderSelect)
            }
            .sortedWith(ProviderPercentDiffComparator)
        val unavailableProviderStates = unavailableProviders.map {
            it.convertToUnavailableProviderState(
                alertText = resourceReference(R.string.express_provider_not_available),
                selectionType = ProviderState.SelectionType.NONE,
            )
        }
        val config = ChooseProviderBottomSheetConfig(
            selectedProviderId = selectedProviderId,
            providers = (availableProvidersStates + unavailableProviderStates).toImmutableList(),
        )
        return uiState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = onDismiss,
                content = config,
            ),
        )
    }

    fun updateProvidersBottomSheetContent(
        uiState: SwapStateHolder,
        tokenSwapInfoForProviders: Map<String, TokenSwapInfo>,
    ): SwapStateHolder {
        val config = uiState.bottomSheetConfig?.content as? ChooseProviderBottomSheetConfig
        return if (config != null) {
            val providers = config.providers
            uiState.copy(
                bottomSheetConfig = uiState.bottomSheetConfig.copy(
                    content = config.copy(
                        providers = providers.map {
                            val tokenInfo = tokenSwapInfoForProviders[it.id]
                            if (it is ProviderState.Content && tokenInfo != null) {
                                val rateString = tokenInfo.tokenAmount
                                    .getFormattedCryptoAmount(tokenInfo.cryptoCurrencyStatus.currency)
                                it.copy(
                                    subtitle = stringReference(rateString),
                                )
                            } else {
                                it
                            }
                        }.toImmutableList(),
                    ),
                ),
            )
        } else {
            uiState
        }
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

    fun updateSelectedFeeBottomSheet(uiState: SwapStateHolder, selectedFee: FeeType): SwapStateHolder {
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
                title = resourceReference(R.string.common_fee_label),
                amountCrypto = this.normalFee.feeCryptoFormatted,
                symbolCrypto = this.normalFee.cryptoSymbol,
                amountFiatFormatted = this.normalFee.feeFiatFormatted,
                isClickable = true,
                onClick = {},
            ),
            FeeItemState.Content(
                feeType = this.priorityFee.feeType,
                title = resourceReference(R.string.common_fee_label),
                amountCrypto = this.priorityFee.feeCryptoFormatted,
                symbolCrypto = this.priorityFee.cryptoSymbol,
                amountFiatFormatted = this.priorityFee.feeFiatFormatted,
                isClickable = true,
                onClick = {},
            ),
        ).toImmutableList()
    }

    private fun Map.Entry<SwapProvider, SwapState>.convertToProviderBottomSheetState(
        pricesLowerBest: Map<SwapProvider, Float>,
        onProviderSelect: (String) -> Unit,
    ): ProviderState? {
        val provider = this.key
        return when (val state = this.value) {
            is SwapState.EmptyAmountState -> null
            is SwapState.QuotesLoadedState -> provider.convertToContentSelectableProviderState(
                isBestRate = false, // not show best rate in bottom sheet
                state = state,
                onProviderClick = onProviderSelect,
                pricesLowerBest = pricesLowerBest,
                selectionType = ProviderState.SelectionType.SELECT,
            )
            is SwapState.SwapError -> getProviderStateForError(
                swapProvider = provider,
                fromToken = state.fromTokenInfo.cryptoCurrencyStatus.currency,
                dataError = state.error,
                onProviderClick = onProviderSelect,
                selectionType = ProviderState.SelectionType.SELECT,
            )
        }
    }

    // region warnings
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

    private fun createUnableToCoverFeeNotificationConfig(
        fromToken: CryptoCurrency,
        onBuyClick: () -> Unit,
    ): NotificationConfig {
        return NotificationConfig(
            title = resourceReference(
                R.string.warning_express_not_enough_fee_for_token_tx_title,
                wrappedList(fromToken.network.name),
            ),
            subtitle = resourceReference(
                R.string.warning_express_not_enough_fee_for_token_tx_description,
                wrappedList(fromToken.network.name, fromToken.network.currencySymbol),
            ),
            iconResId = fromToken.networkIconResId,
            buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.common_buy_currency, wrappedList(fromToken.network.currencySymbol)),
                onClick = onBuyClick,
            ),
        )
    }

    private fun createNetworkFeeCoverageNotificationConfig(): NotificationConfig {
        return NotificationConfig(
            title = resourceReference(R.string.send_network_fee_warning_title),
            subtitle = resourceReference(R.string.send_network_fee_warning_content),
            iconResId = R.drawable.img_attention_20,
        )
    }
    // end region

    private fun getShortAddressValue(fullAddress: String): String {
        check(fullAddress.length > ADDRESS_MIN_LENGTH) { "Invalid address" }
        val firstAddressPart = fullAddress.substring(startIndex = 0, endIndex = ADDRESS_FIRST_PART_LENGTH)
        val secondAddressPart = fullAddress.substring(
            startIndex = fullAddress.length - ADDRESS_SECOND_PART_LENGTH,
            endIndex = fullAddress.length,
        )
        return "$firstAddressPart...$secondAddressPart"
    }

    @Suppress("LongParameterList")
    private fun SwapProvider.convertToContentClickableProviderState(
        isBestRate: Boolean,
        fromTokenInfo: TokenSwapInfo,
        toTokenInfo: TokenSwapInfo,
        selectionType: ProviderState.SelectionType,
        isNeedBadge: Boolean,
        onProviderClick: (String) -> Unit,
    ): ProviderState {
        val rate = toTokenInfo.tokenAmount.value.calculateRate(
            fromTokenInfo.tokenAmount.value,
            toTokenInfo.cryptoCurrencyStatus.currency.decimals,
        )
        val fromCurrencySymbol = fromTokenInfo.cryptoCurrencyStatus.currency.symbol
        val toCurrencySymbol = toTokenInfo.cryptoCurrencyStatus.currency.symbol
        val rateString = "1 $fromCurrencySymbol ≈ $rate $toCurrencySymbol"
        val badge = if (isNeedBadge && isBestRate) {
            ProviderState.AdditionalBadge.BestTrade
        } else {
            ProviderState.AdditionalBadge.Empty
        }
        return ProviderState.Content(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.toString(),
            subtitle = stringReference(rateString),
            additionalBadge = badge,
            selectionType = selectionType,
            percentLowerThenBest = ZERO_PERCENT,
            onProviderClick = onProviderClick,
        )
    }

    private fun SwapProvider.convertToContentSelectableProviderState(
        isBestRate: Boolean,
        state: SwapState.QuotesLoadedState,
        selectionType: ProviderState.SelectionType,
        pricesLowerBest: Map<SwapProvider, Float>,
        onProviderClick: (String) -> Unit,
    ): ProviderState {
        val toTokenInfo = state.toTokenInfo
        val rateString = toTokenInfo.tokenAmount.getFormattedCryptoAmount(toTokenInfo.cryptoCurrencyStatus.currency)
        val additionalBadge = if (state.permissionState is PermissionDataState.PermissionReadyForRequest) {
            ProviderState.AdditionalBadge.PermissionRequired
        } else if (isBestRate) {
            ProviderState.AdditionalBadge.BestTrade
        } else {
            ProviderState.AdditionalBadge.Empty
        }
        return ProviderState.Content(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.toString(),
            subtitle = stringReference(rateString),
            additionalBadge = additionalBadge,
            selectionType = selectionType,
            percentLowerThenBest = pricesLowerBest[this] ?: ZERO_PERCENT,
            onProviderClick = onProviderClick,
        )
    }

    private fun SwapProvider.convertToUnavailableProviderState(
        alertText: TextReference,
        selectionType: ProviderState.SelectionType,
        onProviderClick: ((String) -> Unit)? = null,
    ): ProviderState {
        return ProviderState.Unavailable(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.toString(),
            selectionType = selectionType,
            alertText = alertText,
            onProviderClick = onProviderClick,
        )
    }

    private fun SwapProvider.convertToAvailableFromProviderState(
        alertText: TextReference,
        selectionType: ProviderState.SelectionType,
        onProviderClick: (String) -> Unit,
    ): ProviderState {
        return ProviderState.Content(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.toString(),
            selectionType = selectionType,
            subtitle = alertText,
            additionalBadge = ProviderState.AdditionalBadge.Empty,
            percentLowerThenBest = ZERO_PERCENT,
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

    private fun getFormattedFiatAmount(amount: BigDecimal?): String {
        val appCurrency = appCurrencyProvider()

        return BigDecimalFormatter.formatFiatAmount(amount, appCurrency.code, appCurrency.symbol)
    }

    private fun SwapAmount.getFormattedCryptoAmount(token: CryptoCurrency): String {
        return "${this.formatToUIRepresentation()} ${token.network.currencySymbol}"
    }

    private fun BigDecimal.calculateRate(to: BigDecimal, decimals: Int): BigDecimal {
        return this.divide(to, min(decimals, MAX_DECIMALS_TO_SHOW), RoundingMode.HALF_UP)
    }

    private companion object {
        const val ADDRESS_MIN_LENGTH = 11
        const val ADDRESS_FIRST_PART_LENGTH = 7
        const val ADDRESS_SECOND_PART_LENGTH = 4
        private const val PRICE_IMPACT_THRESHOLD = 0.1
        private const val HUNDRED_PERCENTS = 100
        private const val UNKNOWN_AMOUNT_SIGN = "—"
        private const val MAX_DECIMALS_TO_SHOW = 8
        private const val ZERO_PERCENT = 0f
    }
}