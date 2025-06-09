package com.tangem.feature.swap.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.common.ui.bottomsheet.permission.state.*
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.swapStoriesScreen.SwapStoriesFactory
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.promo.models.StoryContent
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.converters.SwapTransactionErrorStateConverter
import com.tangem.feature.swap.converters.TokensDataConverter
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.NetworkInfo
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.model.SwapNotificationsFactory
import com.tangem.feature.swap.model.SwapProcessDataState
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.*
import com.tangem.feature.swap.models.states.events.SwapEvent
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.PERCENT
import com.tangem.utils.StringsSigns.TILDE_SIGN
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
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

    private val notificationsFactory by lazy(LazyThreadSafetyMode.NONE) {
        SwapNotificationsFactory(actions)
    }

    fun createInitialLoadingState(
        initialCurrencyFrom: CryptoCurrency,
        initialCurrencyTo: CryptoCurrency?,
        fromNetworkInfo: NetworkInfo,
    ): SwapStateHolder {
        return SwapStateHolder(
            blockchainId = fromNetworkInfo.blockchainId,
            sendCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.Inputtable(
                    onAmountChanged = actions.onAmountChanged,
                    onFocusChanged = actions.onAmountSelected,
                    inputError = TransactionCardType.InputError.Empty,
                ),
                amountEquivalent = null,
                amountTextFieldValue = null,
                token = null,
                tokenIconUrl = initialCurrencyFrom.iconUrl,
                tokenCurrency = initialCurrencyFrom.symbol,
                coinId = initialCurrencyFrom.network.backendId,
                canSelectAnotherToken = false,
                isNotNativeToken = initialCurrencyFrom is CryptoCurrency.Token,
                balance = "",
                networkIconRes = getActiveIconRes(initialCurrencyFrom.network.rawId),
                isBalanceHidden = true,
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(),
                amountEquivalent = null,
                tokenIconUrl = initialCurrencyTo?.iconUrl,
                tokenCurrency = initialCurrencyTo?.symbol ?: "",
                token = null,
                amountTextFieldValue = null,
                canSelectAnotherToken = false,
                balance = "",
                isNotNativeToken = initialCurrencyTo is CryptoCurrency.Token,
                networkIconRes = initialCurrencyTo?.let { getActiveIconRes(it.network.rawId) },
                coinId = initialCurrencyTo?.network?.backendId,
                isBalanceHidden = true,
            ),
            fee = FeeItemState.Empty,
            swapButton = SwapButton(enabled = false, onClick = {}),
            onRefresh = {},
            onBackClicked = actions.onBackClicked,
            onChangeCardsClicked = actions.onChangeCardsClicked,
            onMaxAmountSelected = actions.onMaxAmountSelected,
            changeCardsButtonState = ChangeCardsButtonState.UPDATE_IN_PROGRESS,
            onShowPermissionBottomSheet = actions.openPermissionBottomSheet,
            onSelectTokenClick = actions.onSelectTokenClick,
            onSuccess = actions.onSuccess,
            providerState = ProviderState.Empty(),
            shouldShowMaxAmount = false,
            priceImpact = PriceImpact.Empty(),
            isInsufficientFunds = false,
        )
    }

    fun createStoriesState(uiStateHolder: SwapStateHolder, swapStory: StoryContent): SwapStateHolder {
        return uiStateHolder.copy(
            storiesConfig = SwapStoriesFactory.createStoriesState(
                swapStory = swapStory,
                onStoriesClose = actions.onStoriesClose,
            ),
        )
    }

    fun createNoAvailableTokensToSwapState(
        uiStateHolder: SwapStateHolder,
        fromToken: CryptoCurrencyStatus,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.Inputtable),
                amountTextFieldValue = null,
                amountEquivalent = "0 ${appCurrencyProvider.invoke().symbol}",
                token = fromToken,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                coinId = uiStateHolder.sendCardData.coinId,
                isNotNativeToken = uiStateHolder.sendCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                balance = fromToken.getFormattedAmount(isNeedSymbol = false),
                networkIconRes = getActiveIconRes(fromToken.currency.network.rawId),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.Empty(
                type = TransactionCardType.ReadOnly(),
                amountEquivalent = "0 ${appCurrencyProvider.invoke().symbol}",
                amountTextFieldValue = TextFieldValue(
                    text = "0",
                ),
                canSelectAnotherToken = true,
            ),
            notifications = notificationsFactory.getNotAvailableStateNotifications(fromToken.currency.name),
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                enabled = false,
                onClick = { },
            ),
            changeCardsButtonState = ChangeCardsButtonState.DISABLED,
            priceImpact = PriceImpact.Empty(),
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
        val sendInputType = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.Inputtable)
        val sendInput = if (sendInputType.inputError !is TransactionCardType.InputError.Empty) {
            sendInputType
        } else {
            sendInputType.copy(
                inputError = TransactionCardType.InputError.Empty,
                header = TextReference.Res(R.string.swapping_from_title),
            )
        }
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = sendInput,
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = null,
                token = uiStateHolder.sendCardData.token,
                tokenIconUrl = fromToken.iconUrl,
                tokenCurrency = fromToken.symbol,
                coinId = fromToken.network.backendId,
                isNotNativeToken = fromToken is CryptoCurrency.Token,
                canSelectAnotherToken = canSelectSendToken,
                balance = if (!canSelectSendToken) uiStateHolder.sendCardData.balance else "",
                networkIconRes = getActiveIconRes(fromToken.network.rawId),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(),
                amountTextFieldValue = null,
                amountEquivalent = null,
                token = uiStateHolder.receiveCardData.token,
                tokenIconUrl = toToken.iconUrl,
                tokenCurrency = toToken.symbol,
                coinId = toToken.network.backendId,
                isNotNativeToken = toToken is CryptoCurrency.Token,
                canSelectAnotherToken = canSelectReceiveToken,
                balance = if (!canSelectReceiveToken) uiStateHolder.receiveCardData.balance else "",
                networkIconRes = getActiveIconRes(toToken.network.rawId),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            notifications = persistentListOf(),
            fee = FeeItemState.Empty,
            swapButton = SwapButton(enabled = false, onClick = {}),
            providerState = ProviderState.Loading(),
            permissionState = uiStateHolder.permissionState,
            changeCardsButtonState = ChangeCardsButtonState.UPDATE_IN_PROGRESS,
            priceImpact = PriceImpact.Empty(),
            shouldShowMaxAmount = shouldShowMaxAmount(fromToken, toToken),
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
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        swapProvider: SwapProvider,
        bestRatedProviderId: String,
        isNeedBestRateBadge: Boolean,
        selectedFeeType: FeeType,
        isReverseSwapPossible: Boolean,
        needApplyFCARestrictions: Boolean,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val notifications = notificationsFactory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            fromToken = fromToken,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            selectedFeeType = selectedFeeType,
            providerName = swapProvider.name,
        )
        val feeState = createFeeState(quoteModel.txFee, selectedFeeType)
        val fromCurrencyStatus = quoteModel.fromTokenInfo.cryptoCurrencyStatus
        val toCurrencyStatus = quoteModel.toTokenInfo.cryptoCurrencyStatus
        val isInsufficientFunds = isInsufficientFundsCondition(quoteModel)
        val insufficientFundsHeader = if (isInsufficientFunds) {
            TextReference.Res(R.string.swapping_insufficient_funds)
        } else {
            TextReference.Res(R.string.swapping_from_title)
        }
        val sendCardType = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.Inputtable)
        val sendInput = when (sendCardType.inputError) {
            is TransactionCardType.InputError.WrongAmount,
            -> sendCardType
            is TransactionCardType.InputError.Empty,
            is TransactionCardType.InputError.InsufficientFunds,
            -> {
                val error = if (isInsufficientFunds) {
                    TransactionCardType.InputError.InsufficientFunds
                } else {
                    TransactionCardType.InputError.Empty
                }
                sendCardType.copy(
                    inputError = error,
                    header = insufficientFundsHeader,
                )
            }
        }
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = sendInput,
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = getFormattedFiatAmount(quoteModel.fromTokenInfo.amountFiat),
                token = fromCurrencyStatus,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                coinId = fromCurrencyStatus.currency.network.backendId,
                isNotNativeToken = uiStateHolder.sendCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                networkIconRes = uiStateHolder.sendCardData.networkIconRes,
                balance = fromCurrencyStatus.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(
                    showWarning = true,
                    actions.onReceiveCardWarningClick,
                ),
                amountTextFieldValue = TextFieldValue(
                    quoteModel.toTokenInfo.tokenAmount
                        .formatToUIRepresentation()
                        .appendApproximateSign(),
                ),
                amountEquivalent = getFormattedFiatAmount(quoteModel.toTokenInfo.amountFiat),
                token = toCurrencyStatus,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                coinId = toCurrencyStatus.currency.network.backendId,
                isNotNativeToken = uiStateHolder.receiveCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                networkIconRes = uiStateHolder.receiveCardData.networkIconRes,
                balance = toCurrencyStatus.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            isInsufficientFunds = isInsufficientFundsCondition(quoteModel),
            notifications = notifications,
            permissionState = convertPermissionState(
                lastPermissionState = uiStateHolder.permissionState,
                permissionDataState = quoteModel.permissionState,
                providerName = swapProvider.name,
                onGivePermissionClick = actions.onGivePermissionClick,
                onChangeApproveType = actions.onChangeApproveType,
            ),
            fee = feeState,
            swapButton = SwapButton(
                enabled = getSwapButtonEnabled(notifications),
                onClick = actions.onSwapClick,
            ),
            changeCardsButtonState = if (isReverseSwapPossible) {
                ChangeCardsButtonState.ENABLED
            } else {
                ChangeCardsButtonState.DISABLED
            },
            providerState = swapProvider.convertToContentClickableProviderState(
                isBestRate = bestRatedProviderId == swapProvider.providerId,
                fromTokenInfo = quoteModel.fromTokenInfo,
                toTokenInfo = quoteModel.toTokenInfo,
                isNeedBestRateBadge = isNeedBestRateBadge,
                selectionType = ProviderState.SelectionType.CLICK,
                onProviderClick = actions.onProviderClick,
                needApplyFCARestrictions = needApplyFCARestrictions,
            ),
            priceImpact = if (quoteModel.priceImpact.value > PRICE_IMPACT_THRESHOLD) {
                quoteModel.priceImpact
            } else {
                PriceImpact.Empty()
            },
            tosState = createTosState(swapProvider),
            shouldShowMaxAmount = shouldShowMaxAmount(fromToken, toCurrencyStatus.currency),
        )
    }

    private fun shouldShowMaxAmount(fromToken: CryptoCurrency, toCurrency: CryptoCurrency): Boolean {
        return !(fromToken is CryptoCurrency.Coin && fromToken.network.id == toCurrency.network.id)
    }

    private fun createTosState(swapProvider: SwapProvider): TosState {
        return TosState(
            tosLink = swapProvider.termsOfUse?.let {
                LegalState(
                    title = resourceReference(R.string.common_terms_of_use),
                    link = it,
                    onClick = actions.onLinkClick,
                )
            },
            policyLink = swapProvider.privacyPolicy?.let {
                LegalState(
                    title = resourceReference(R.string.common_privacy_policy),
                    link = it,
                    onClick = actions.onLinkClick,
                )
            },
        )
    }

    private fun isInsufficientFundsCondition(quoteModel: SwapState.QuotesLoadedState): Boolean {
        return !quoteModel.preparedSwapConfigState.isBalanceEnough &&
            quoteModel.preparedSwapConfigState.includeFeeInAmount !is IncludeFeeInAmount.Included
    }

    private fun getSwapButtonEnabled(notifications: ImmutableList<NotificationUM>): Boolean {
        return notifications.none {
            it is SwapNotificationUM.Error || it is NotificationUM.Error ||
                it is SwapNotificationUM.Warning.ExpressError || it is SwapNotificationUM.Warning.ExpressGeneralError ||
                it is SwapNotificationUM.Warning.NoAvailableTokensToSwap ||
                it is SwapNotificationUM.Warning.NeedReserveToCreateAccount
        }
    }

    @Suppress("LongParameterList")
    fun createQuotesErrorState(
        uiStateHolder: SwapStateHolder,
        swapProvider: SwapProvider,
        fromToken: TokenSwapInfo,
        toToken: CryptoCurrencyStatus?,
        includeFeeInAmount: IncludeFeeInAmount,
        expressDataError: ExpressDataError,
        isReverseSwapPossible: Boolean,
        needApplyFCARestrictions: Boolean,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val notifications = notificationsFactory.getQuotesErrorStateNotifications(
            expressDataError = expressDataError,
            fromToken = fromToken.cryptoCurrencyStatus.currency,
            feeItem = uiStateHolder.fee,
            includeFeeInAmount = includeFeeInAmount,
        )

        val providerState = getProviderStateForError(
            swapProvider = swapProvider,
            fromToken = fromToken.cryptoCurrencyStatus.currency,
            expressDataError = expressDataError,
            onProviderClick = actions.onProviderClick,
            selectionType = ProviderState.SelectionType.CLICK,
            needApplyFCARestrictions = needApplyFCARestrictions,
        )
        val receiveCardData = toToken?.let {
            SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(),
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
                balance = toToken.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            )
        } ?: SwapCardState.Empty(
            type = TransactionCardType.ReadOnly(),
            amountEquivalent = "0 ${appCurrencyProvider.invoke().symbol}",
            amountTextFieldValue = TextFieldValue(
                text = "0",
            ),
            canSelectAnotherToken = true,
        )
        return uiStateHolder.copy(
            sendCardData = uiStateHolder.sendCardData.copy(
                amountEquivalent = getFormattedFiatAmount(fromToken.amountFiat),
                balance = fromToken.cryptoCurrencyStatus.getFormattedAmount(isNeedSymbol = false),
            ),
            receiveCardData = receiveCardData,
            notifications = notifications,
            permissionState = GiveTxPermissionState.Empty,
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                enabled = false,
                onClick = actions.onSwapClick,
            ),
            changeCardsButtonState = if (isReverseSwapPossible) {
                ChangeCardsButtonState.ENABLED
            } else {
                ChangeCardsButtonState.DISABLED
            },
            providerState = providerState,
            priceImpact = PriceImpact.Empty(),
            tosState = createTosState(swapProvider),
        )
    }

    @Suppress("LongParameterList")
    private fun getProviderStateForError(
        swapProvider: SwapProvider,
        fromToken: CryptoCurrency,
        expressDataError: ExpressDataError,
        onProviderClick: (String) -> Unit,
        selectionType: ProviderState.SelectionType,
        needApplyFCARestrictions: Boolean,
    ): ProviderState {
        return when (expressDataError) {
            is ExpressDataError.ExchangeTooSmallAmountError -> {
                swapProvider.convertToAvailableFromProviderState(
                    swapProvider = swapProvider,
                    alertText = resourceReference(
                        R.string.express_provider_min_amount,
                        wrappedList(expressDataError.amount.getFormattedCryptoAmount(fromToken)),
                    ),
                    selectionType = selectionType,
                    onProviderClick = onProviderClick,
                    needApplyFCARestrictions = needApplyFCARestrictions,
                )
            }
            is ExpressDataError.ExchangeTooBigAmountError -> {
                swapProvider.convertToAvailableFromProviderState(
                    swapProvider = swapProvider,
                    alertText = resourceReference(
                        R.string.express_provider_max_amount,
                        wrappedList(expressDataError.amount.getFormattedCryptoAmount(fromToken)),
                    ),
                    selectionType = selectionType,
                    onProviderClick = onProviderClick,
                    needApplyFCARestrictions = needApplyFCARestrictions,
                )
            }
            else -> {
                ProviderState.Empty()
            }
        }
    }

    fun createQuotesEmptyAmountState(
        uiStateHolder: SwapStateHolder,
        emptyAmountState: SwapState.EmptyAmountState,
        fromTokenStatus: CryptoCurrencyStatus,
        toTokenStatus: CryptoCurrencyStatus?,
        isReverseSwapPossible: Boolean,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = requireNotNull(uiStateHolder.sendCardData.type as? TransactionCardType.Inputtable),
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
                token = uiStateHolder.sendCardData.token,
                tokenIconUrl = uiStateHolder.sendCardData.tokenIconUrl,
                coinId = uiStateHolder.sendCardData.coinId,
                isNotNativeToken = uiStateHolder.sendCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.sendCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.sendCardData.canSelectAnotherToken,
                networkIconRes = uiStateHolder.sendCardData.networkIconRes,
                balance = fromTokenStatus.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(),
                amountTextFieldValue = TextFieldValue("0"),
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
                token = uiStateHolder.receiveCardData.token,
                tokenIconUrl = uiStateHolder.receiveCardData.tokenIconUrl,
                coinId = uiStateHolder.receiveCardData.coinId,
                isNotNativeToken = uiStateHolder.receiveCardData.isNotNativeToken,
                tokenCurrency = uiStateHolder.receiveCardData.tokenCurrency,
                canSelectAnotherToken = uiStateHolder.receiveCardData.canSelectAnotherToken,
                networkIconRes = uiStateHolder.receiveCardData.networkIconRes,
                balance = toTokenStatus?.getFormattedAmount(isNeedSymbol = false) ?: DASH_SIGN,
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            notifications = persistentListOf(),
            isInsufficientFunds = false,
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                enabled = false,
                onClick = { },
            ),
            changeCardsButtonState = if (isReverseSwapPossible) {
                ChangeCardsButtonState.ENABLED
            } else {
                ChangeCardsButtonState.DISABLED
            },
            providerState = ProviderState.Empty(),
            priceImpact = PriceImpact.Empty(),
        )
    }

    fun createSwapInProgressState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            swapButton = uiState.swapButton.copy(
                enabled = false,
            ),
        )
    }

    fun addTokensToState(
        uiState: SwapStateHolder,
        fromToken: CryptoCurrency,
        tokensDataState: CurrenciesGroup,
    ): SwapStateHolder {
        return uiState.copy(
            selectTokenState = tokensDataConverter.convert(
                value = CurrenciesGroupWithFromCurrency(
                    fromCurrency = fromToken,
                    group = tokensDataState,
                ),
            ),
        )
    }

    fun createSilentLoadState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            changeCardsButtonState = ChangeCardsButtonState.UPDATE_IN_PROGRESS,
        )
    }

    fun updateSwapAmount(
        uiState: SwapStateHolder,
        amountFormatted: String,
        amountRaw: String,
        fromToken: CryptoCurrency,
        minTxAmount: BigDecimal?,
    ): SwapStateHolder {
        if (uiState.sendCardData !is SwapCardState.SwapCardData) return uiState
        val amountToSend = amountRaw.toBigDecimalOrNull()
        val sendInput = if (minTxAmount != null && amountToSend != null && amountToSend < minTxAmount) {
            val minAmountFormatted = minTxAmount.format {
                crypto(cryptoCurrency = fromToken, ignoreSymbolPosition = true)
            }
            (uiState.sendCardData.type as? TransactionCardType.Inputtable)?.copy(
                inputError = TransactionCardType.InputError.WrongAmount,
                header = resourceReference(R.string.transfer_min_amount_error, wrappedList(minAmountFormatted)),
            ) ?: uiState.sendCardData.type
        } else {
            (uiState.sendCardData.type as? TransactionCardType.Inputtable)?.copy(
                inputError = TransactionCardType.InputError.Empty,
                header = TextReference.Res(R.string.swapping_from_title),
            ) ?: uiState.sendCardData.type
        }
        return uiState.copy(
            sendCardData = uiState.sendCardData.copy(
                amountTextFieldValue = TextFieldValue(
                    text = amountFormatted,
                    selection = TextRange(amountFormatted.length),
                ),
                type = sendInput,
            ),
        )
    }

    fun updateSendCurrencyBalance(
        uiState: SwapStateHolder,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): SwapStateHolder {
        if (uiState.sendCardData !is SwapCardState.SwapCardData) return uiState

        return uiState.copy(
            sendCardData = uiState.sendCardData.copy(
                balance = cryptoCurrencyStatus.getFormattedAmount(isNeedSymbol = false),
                token = cryptoCurrencyStatus,
            ),
        )
    }

    fun updateReceiveCurrencyBalance(
        uiState: SwapStateHolder,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): SwapStateHolder {
        if (uiState.receiveCardData !is SwapCardState.SwapCardData) return uiState

        return uiState.copy(
            receiveCardData = uiState.receiveCardData.copy(
                balance = cryptoCurrencyStatus.getFormattedAmount(isNeedSymbol = false),
                token = cryptoCurrencyStatus,
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
        val config = uiState.bottomSheetConfig?.content as? GiveTxPermissionBottomSheetConfig
        val permissionState = (uiState.permissionState as? GiveTxPermissionState.ReadyForRequest)?.copy(
            approveType = approveType,
        ) ?: uiState.permissionState
        return if (config != null) {
            uiState.copy(
                permissionState = permissionState,
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

    fun createInitialErrorState(uiState: SwapStateHolder, code: Int, onRefreshClick: () -> Unit): SwapStateHolder {
        return uiState.copy(
            isInsufficientFunds = false,
            notifications = notificationsFactory.getInitialErrorStateNotifications(code, onRefreshClick),
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
            title = resourceReference(R.string.common_network_fee_title),
            amountCrypto = fee.feeCryptoFormattedWithNative, // display fee with native as workaround for okx
            symbolCrypto = fee.cryptoSymbol,
            amountFiatFormatted = fee.feeFiatFormattedWithNative, // display fee with native as workaround for okx
            isClickable = isClickable,
            onClick = actions.onClickFee,
        )
    }

    fun loadingPermissionState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            swapButton = uiState.swapButton.copy(
                enabled = false,
            ),
            permissionState = GiveTxPermissionState.InProgress,
            notifications = notificationsFactory.getApprovalInProgressStateNotification(uiState.notifications),
        )
    }

    @Suppress("LongParameterList")
    fun createSuccessState(
        uiState: SwapStateHolder,
        swapTransactionState: SwapTransactionState.TxSent,
        dataState: SwapProcessDataState,
        onExploreClick: () -> Unit,
        onStatusClick: () -> Unit,
        txUrl: String,
    ): SwapStateHolder {
        val fee = requireNotNull(dataState.selectedFee)
        val fromCryptoCurrency = requireNotNull(dataState.fromCryptoCurrency)
        val toCryptoCurrency = requireNotNull(dataState.toCryptoCurrency)
        val fromAmount = swapTransactionState.fromAmountValue ?: BigDecimal.ZERO
        val toAmount = swapTransactionState.toAmountValue ?: BigDecimal.ZERO
        val providerState = uiState.providerState as ProviderState.Content

        val fromFiatAmount = getFormattedFiatAmount(fromCryptoCurrency.value.fiatRate?.multiply(fromAmount))
        val toFiatAmount = getFormattedFiatAmount(toCryptoCurrency.value.fiatRate?.multiply(toAmount))

        val shouldShowStatus = providerState.type == ExchangeProviderType.CEX.providerName
        return uiState.copy(
            successState = SwapSuccessStateHolder(
                timestamp = swapTransactionState.timestamp,
                txUrl = txUrl,
                providerName = stringReference(providerState.name),
                providerType = stringReference(providerState.type),
                showStatusButton = shouldShowStatus,
                providerIcon = providerState.iconUrl,
                rate = providerState.subtitle,
                fee = stringReference("${fee.feeCryptoFormattedWithNative} (${fee.feeFiatFormattedWithNative})"),
                fromTokenAmount = stringReference(swapTransactionState.fromAmount.orEmpty()),
                toTokenAmount = stringReference(swapTransactionState.toAmount.orEmpty()),
                fromTokenFiatAmount = stringReference(fromFiatAmount),
                toTokenFiatAmount = stringReference(toFiatAmount),
                fromTokenIconState = iconStateConverter.convert(fromCryptoCurrency),
                toTokenIconState = iconStateConverter.convert(toCryptoCurrency),
                onExploreButtonClick = onExploreClick,
                onStatusButtonClick = onStatusClick,
            ),
        )
    }

    fun createErrorTransactionAlert(
        uiState: SwapStateHolder,
        error: SwapTransactionState.Error,
        onDismiss: () -> Unit,
        onSupportClick: (String) -> Unit,
    ): SwapStateHolder {
        val errorAlert = SwapTransactionErrorStateConverter(
            onSupportClick = onSupportClick,
            onDismiss = onDismiss,
        ).convert(error)
        return uiState.copy(
            event = errorAlert?.let {
                triggeredEvent(
                    data = SwapEvent.ShowAlert(errorAlert),
                    onConsume = onDismiss,
                )
            } ?: consumedEvent(),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
        )
    }

    fun createDemoModeAlert(uiState: SwapStateHolder, onDismiss: () -> Unit): SwapStateHolder {
        return uiState.copy(
            event = triggeredEvent(
                data = SwapEvent.ShowAlert(AlertDemoModeUM(onDismiss)),
                onConsume = onDismiss,
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
        )
    }

    fun createAlert(
        uiState: SwapStateHolder,
        isPriceImpact: Boolean,
        token: String,
        provider: SwapProvider,
        onDismiss: () -> Unit,
    ): SwapStateHolder {
        val slippage = provider.slippage?.let { "${it.parseBigDecimal(1)}$PERCENT" }
        val combinedMessage = buildList {
            when (provider.type) {
                ExchangeProviderType.CEX -> {
                    if (slippage != null) {
                        add(
                            resourceReference(
                                id = R.string.swapping_alert_cex_description_with_slippage,
                                formatArgs = wrappedList(token, slippage),
                            ),
                        )
                    } else {
                        add(resourceReference(R.string.swapping_alert_cex_description, wrappedList(token)))
                    }
                }
                ExchangeProviderType.DEX,
                ExchangeProviderType.DEX_BRIDGE,
                -> {
                    if (isPriceImpact) {
                        add(resourceReference(R.string.swapping_high_price_impact_description))
                        add(stringReference("\n\n"))
                    }
                    if (slippage != null) {
                        add(
                            resourceReference(
                                id = R.string.swapping_alert_dex_description_with_slippage,
                                formatArgs = wrappedList(slippage),
                            ),
                        )
                    } else {
                        add(resourceReference(R.string.swapping_alert_dex_description, wrappedList(token)))
                    }
                }
            }
        }
        return uiState.copy(
            event = triggeredEvent(
                SwapEvent.ShowAlert(
                    SwapAlertUM.InformationAlert(
                        message = combinedReference(combinedMessage.toWrappedList()),
                        onConfirmClick = onDismiss,
                    ),
                ),
                onConsume = onDismiss,
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
        )
    }

    fun addAlert(
        uiState: SwapStateHolder,
        message: TextReference = resourceReference(R.string.common_unknown_error),
        onDismiss: () -> Unit = { clearAlert(uiState) },
    ): SwapStateHolder {
        return uiState.copy(
            event = triggeredEvent(
                SwapEvent.ShowAlert(
                    SwapAlertUM.GenericError(onDismiss, message),
                ),
                onConsume = onDismiss,
            ),
        )
    }

    fun clearAlert(uiState: SwapStateHolder): SwapStateHolder = uiState.copy(event = consumedEvent())

    fun addNotification(uiState: SwapStateHolder, message: TextReference?, onClick: () -> Unit): SwapStateHolder {
        return uiState.copy(
            notifications = notificationsFactory.getGeneralErrorStateNotifications(
                message = message,
                onClick = onClick,
            ),
        )
    }

    private fun convertPermissionState(
        lastPermissionState: GiveTxPermissionState,
        permissionDataState: PermissionDataState,
        providerName: String,
        onGivePermissionClick: () -> Unit,
        onChangeApproveType: (ApproveType) -> Unit,
    ): GiveTxPermissionState {
        val approveType = if (lastPermissionState is GiveTxPermissionState.ReadyForRequest) {
            lastPermissionState.approveType
        } else {
            ApproveType.UNLIMITED
        }
        return when (permissionDataState) {
            PermissionDataState.Empty -> GiveTxPermissionState.Empty
            PermissionDataState.PermissionFailed -> GiveTxPermissionState.Empty
            PermissionDataState.PermissionLoading -> GiveTxPermissionState.InProgress
            is PermissionDataState.PermissionReadyForRequest -> {
                val permissionFee = when (val fee = permissionDataState.requestApproveData.fee) {
                    TxFeeState.Empty -> error("Fee shouldn't be empty")
                    is TxFeeState.MultipleFeeState -> fee.priorityFee
                    is TxFeeState.SingleFeeState -> fee.fee
                }
                GiveTxPermissionState.ReadyForRequest(
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
                    subtitle = resourceReference(
                        id = R.string.give_permission_swap_subtitle,
                        formatArgs = wrappedList(providerName, permissionDataState.currency),
                    ),
                    dialogText = resourceReference(R.string.swapping_approve_information_text),
                    footerText = resourceReference(R.string.swap_give_permission_fee_footer),
                )
            }
        }
    }

    fun showPermissionBottomSheet(uiState: SwapStateHolder, onDismiss: () -> Unit): SwapStateHolder {
        val permissionState = uiState.permissionState
        if (permissionState is GiveTxPermissionState.ReadyForRequest) {
            val config = GiveTxPermissionBottomSheetConfig(
                data = permissionState,
                onCancel = onDismiss,
            )
            return uiState.copy(
                bottomSheetConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = onDismiss,
                    content = config,
                ),
            )
        }
        return uiState
    }

    fun dismissBottomSheet(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            bottomSheetConfig = uiState.bottomSheetConfig?.copy(isShown = false),
        )
    }

    @Suppress("LongParameterList")
    fun showSelectProviderBottomSheet(
        uiState: SwapStateHolder,
        selectedProviderId: String,
        pricesLowerBest: Map<String, Float>,
        providersStates: Map<SwapProvider, SwapState>,
        needApplyFCARestrictions: Boolean,
        onDismiss: () -> Unit,
    ): SwapStateHolder {
        val availableProvidersStates = providersStates.entries
            .mapNotNull {
                it.convertToProviderBottomSheetState(
                    pricesLowerBest = pricesLowerBest,
                    onProviderSelect = actions.onProviderSelect,
                    needApplyFCARestrictions = needApplyFCARestrictions,
                )
            }
            .sortedWith(ProviderPercentDiffComparator)
            .toImmutableList()

        val isAnyFCABadge = availableProvidersStates.any {
            (it as? ProviderState.Content)?.additionalBadge == ProviderState.AdditionalBadge.FCAWarningList
        }
        val config = ChooseProviderBottomSheetConfig(
            selectedProviderId = selectedProviderId,
            providers = availableProvidersStates,
            notification = SwapNotificationUM.Error.FCAWarningList.takeIf { isAnyFCABadge },
        )
        return uiState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onDismiss,
                content = config,
            ),
        )
    }

    fun updateProvidersBottomSheetContent(
        uiState: SwapStateHolder,
        pricesLowerBest: Map<String, Float>,
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
                                    percentLowerThenBest = pricesLowerBest[it.id]?.let { percent ->
                                        PercentDifference.Value(percent)
                                    } ?: PercentDifference.Value(0f),
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
            readMoreUrl = buildReadMoreUrl(),
            feeItems = txFeeState.toFeeItemState(),
            readMore = resourceReference(R.string.common_read_more),
            onReadMoreClick = actions.onLinkClick,
        )
        return uiState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onDismiss,
                content = config,
            ),
        )
    }

    private fun buildReadMoreUrl(): String {
        return buildString {
            append(FEE_READ_MORE_URL_FIRST_PART)
            append(getLocaleName())
            append(FEE_READ_MORE_URL_SECOND_PART)
        }
    }

    private fun TxFeeState.MultipleFeeState.toFeeItemState(): ImmutableList<FeeItemState.Content> {
        return listOf(
            FeeItemState.Content(
                feeType = this.normalFee.feeType,
                title = resourceReference(R.string.common_network_fee_title),
                amountCrypto = this.normalFee.feeCryptoFormattedWithNative,
                symbolCrypto = this.normalFee.cryptoSymbol,
                amountFiatFormatted = this.normalFee.feeFiatFormattedWithNative,
                isClickable = true,
                onClick = {},
            ),
            FeeItemState.Content(
                feeType = this.priorityFee.feeType,
                title = resourceReference(R.string.common_network_fee_title),
                amountCrypto = this.priorityFee.feeCryptoFormattedWithNative,
                symbolCrypto = this.priorityFee.cryptoSymbol,
                amountFiatFormatted = this.priorityFee.feeFiatFormattedWithNative,
                isClickable = true,
                onClick = {},
            ),
        ).toImmutableList()
    }

    private fun Map.Entry<SwapProvider, SwapState>.convertToProviderBottomSheetState(
        pricesLowerBest: Map<String, Float>,
        onProviderSelect: (String) -> Unit,
        needApplyFCARestrictions: Boolean,
    ): ProviderState? {
        val provider = this.key
        return when (val state = this.value) {
            is SwapState.EmptyAmountState -> null
            is SwapState.QuotesLoadedState -> {
                provider.convertToContentSelectableProviderState(
                    state = state,
                    onProviderClick = onProviderSelect,
                    pricesLowerBest = pricesLowerBest,
                    selectionType = ProviderState.SelectionType.SELECT,
                    needApplyFCARestrictions = needApplyFCARestrictions,
                )
            }
            is SwapState.SwapError -> getProviderStateForError(
                swapProvider = provider,
                fromToken = state.fromTokenInfo.cryptoCurrencyStatus.currency,
                expressDataError = state.error,
                onProviderClick = onProviderSelect,
                selectionType = ProviderState.SelectionType.SELECT,
                needApplyFCARestrictions = needApplyFCARestrictions,
            )
        }
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

    @Suppress("LongParameterList")
    private fun SwapProvider.convertToContentClickableProviderState(
        isBestRate: Boolean,
        fromTokenInfo: TokenSwapInfo,
        toTokenInfo: TokenSwapInfo,
        selectionType: ProviderState.SelectionType,
        isNeedBestRateBadge: Boolean,
        onProviderClick: (String) -> Unit,
        needApplyFCARestrictions: Boolean,
    ): ProviderState {
        val rate = toTokenInfo.tokenAmount.value.calculateRate(
            fromTokenInfo.tokenAmount.value,
            toTokenInfo.cryptoCurrencyStatus.currency.decimals,
        )
        val fromCurrencySymbol = fromTokenInfo.cryptoCurrencyStatus.currency.symbol
        val rateString = buildString {
            append(BigDecimal.ONE.format { crypto(symbol = fromCurrencySymbol, decimals = 0).anyDecimals() })
            append(" ≈ ")
            append(rate.format { crypto(toTokenInfo.cryptoCurrencyStatus.currency) })
        }

        val additionalBadge = when {
            needApplyFCARestrictions && isFCARestrictedProvider() -> ProviderState.AdditionalBadge.FCAWarningList
            isRecommended -> ProviderState.AdditionalBadge.Recommended
            isNeedBestRateBadge && isBestRate && !needApplyFCARestrictions -> ProviderState.AdditionalBadge.BestTrade
            else -> ProviderState.AdditionalBadge.Empty
        }

        return ProviderState.Content(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.providerName,
            subtitle = stringReference(rateString),
            additionalBadge = additionalBadge,
            selectionType = selectionType,
            percentLowerThenBest = PercentDifference.Empty,
            namePrefix = ProviderState.PrefixType.NONE,
            onProviderClick = onProviderClick,
        )
    }

    private fun SwapProvider.convertToContentSelectableProviderState(
        state: SwapState.QuotesLoadedState,
        selectionType: ProviderState.SelectionType,
        pricesLowerBest: Map<String, Float>,
        onProviderClick: (String) -> Unit,
        needApplyFCARestrictions: Boolean,
    ): ProviderState {
        val toTokenInfo = state.toTokenInfo
        val rateString = toTokenInfo.tokenAmount.getFormattedCryptoAmount(toTokenInfo.cryptoCurrencyStatus.currency)

        val additionalBadge = when {
            needApplyFCARestrictions && isFCARestrictedProvider() -> ProviderState.AdditionalBadge.FCAWarningList
            state.permissionState is PermissionDataState.PermissionReadyForRequest -> {
                ProviderState.AdditionalBadge.PermissionRequired
            }
            isRecommended -> ProviderState.AdditionalBadge.Recommended
            else -> ProviderState.AdditionalBadge.Empty
        }

        return ProviderState.Content(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.providerName,
            subtitle = stringReference(rateString),
            additionalBadge = additionalBadge,
            selectionType = selectionType,
            percentLowerThenBest = pricesLowerBest[this.providerId]?.let { percent ->
                PercentDifference.Value(percent)
            } ?: PercentDifference.Value(0f),
            namePrefix = ProviderState.PrefixType.NONE,
            onProviderClick = onProviderClick,
        )
    }

    private fun SwapProvider.convertToAvailableFromProviderState(
        swapProvider: SwapProvider,
        alertText: TextReference,
        selectionType: ProviderState.SelectionType,
        onProviderClick: (String) -> Unit,
        needApplyFCARestrictions: Boolean,
    ): ProviderState {
        val additionalBadge = when {
            needApplyFCARestrictions && isFCARestrictedProvider() -> ProviderState.AdditionalBadge.FCAWarningList
            swapProvider.isRecommended -> ProviderState.AdditionalBadge.Recommended
            else -> ProviderState.AdditionalBadge.Empty
        }

        return ProviderState.Content(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.providerName,
            selectionType = selectionType,
            subtitle = alertText,
            additionalBadge = additionalBadge,
            percentLowerThenBest = PercentDifference.Empty,
            namePrefix = ProviderState.PrefixType.NONE,
            onProviderClick = onProviderClick,
        )
    }

    private fun CryptoCurrencyStatus.getFormattedAmount(isNeedSymbol: Boolean): String {
        val amount = value.amount ?: return DASH_SIGN
        val symbol = if (isNeedSymbol) currency.symbol else ""
        return amount.format { crypto(symbol, currency.decimals) }
    }

    private fun getFormattedFiatAmount(amount: BigDecimal?): String {
        val appCurrency = appCurrencyProvider()

        return BigDecimalFormatter.formatFiatAmount(amount, appCurrency.code, appCurrency.symbol)
    }

    private fun SwapAmount.getFormattedCryptoAmount(token: CryptoCurrency): String {
        return value.format { crypto(token) }
    }

    private fun BigDecimal.calculateRate(to: BigDecimal, decimals: Int): BigDecimal {
        val rateDecimals = if (decimals == 0) IF_ZERO_DECIMALS_TO_SHOW else decimals
        return this.divide(to, min(rateDecimals, MAX_DECIMALS_TO_SHOW), RoundingMode.HALF_UP)
    }

    private fun getLocaleName(): String {
        return if (Locale.getDefault().language == "ru") {
            RU_LOCALE
        } else {
            EN_LOCALE
        }
    }

    private fun String.appendApproximateSign(): String {
        return "$TILDE_SIGN $this"
    }

    private fun SwapProvider.isFCARestrictedProvider(): Boolean {
        return FCA_RESTRICTED_PROVIDER_IDS.contains(providerId)
    }

    private companion object {
        private const val RU_LOCALE = "ru"
        private const val EN_LOCALE = "en"
        const val ADDRESS_MIN_LENGTH = 11
        const val ADDRESS_FIRST_PART_LENGTH = 7
        const val ADDRESS_SECOND_PART_LENGTH = 4
        private const val PRICE_IMPACT_THRESHOLD = 0.1
        private const val MAX_DECIMALS_TO_SHOW = 8
        private const val IF_ZERO_DECIMALS_TO_SHOW = 2
        private const val FEE_READ_MORE_URL_FIRST_PART = "https://tangem.com/"
        private const val FEE_READ_MORE_URL_SECOND_PART = "/blog/post/what-is-a-transaction-fee-and-why-do-we-need-it/"

        private val FCA_RESTRICTED_PROVIDER_IDS = setOf(
            "changelly",
            "changenow",
            "okx-cross-chain",
            "okx-on-chain",
            "simpleswap",
        )
    }
}