package com.tangem.feature.swap.ui

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.IncludeFeeInAmount
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.model.SwapNotificationsFactory
import com.tangem.feature.swap.model.SwapProcessDataState
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.*
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.TILDE_SIGN
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
    private val isAccountsModeProvider: Provider<Boolean>,
    private val iGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork,
) {
    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    private val notificationsFactory by lazy(LazyThreadSafetyMode.NONE) {
        SwapNotificationsFactory(actions, iGaslessFeeSupportedForNetwork)
    }

    fun createInitialLoadingState(): SwapStateHolder {
        return SwapStateHolder(
            sendCardData = getEmptyCardState(
                isFromCard = true,
                emptyAmountState = SwapState.EmptyAmountState(TextReference.EMPTY),
            ),
            receiveCardData = getEmptyCardState(
                isFromCard = false,
                emptyAmountState = SwapState.EmptyAmountState(TextReference.EMPTY),
            ),
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                walletInteractionIcon = null,
                isEnabled = false,
                isInProgress = true,
                isHoldToConfirm = false,
                onClick = {},
            ),
            onRefresh = {},
            onBackClicked = actions.onBackClicked,
            onChangeCardsClicked = actions.onChangeCardsClicked,
            onMaxAmountSelected = actions.onMaxAmountSelected,
            changeCardsButtonState = ChangeCardsButtonState.DISABLED,
            onShowPermissionBottomSheet = actions.openPermissionBottomSheet,
            onSelectTokenClick = actions.onSelectTokenClick,
            onSuccess = actions.onSuccess,
            providerState = ProviderState.Empty(),
            shouldShowMaxAmount = false,
            priceImpact = PriceImpact.Empty,
            isInsufficientFunds = false,
        )
    }

    fun createInitialReadyState(
        uiStateHolder: SwapStateHolder,
        emptyAmountState: SwapState.EmptyAmountState,
        fromSwapCurrencyStatus: SwapCurrencyStatus?,
        toSwapCurrencyStatus: SwapCurrencyStatus?,
    ): SwapStateHolder {
        return uiStateHolder.copy(
            sendCardData = createCardState(
                swapCurrencyStatus = fromSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = true,
            ),
            receiveCardData = createCardState(
                swapCurrencyStatus = toSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = false,
            ),
            notifications = persistentListOf(),
            isInsufficientFunds = false,
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                walletInteractionIcon = fromSwapCurrencyStatus?.userWallet?.let(::walletInterationIcon),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus?.userWallet?.isHotWallet == true,
                onClick = { },
            ),
            shouldShowMaxAmount = shouldShowMaxAmount(fromSwapCurrencyStatus?.currency, toSwapCurrencyStatus?.currency),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = ProviderState.Empty(),
            priceImpact = PriceImpact.Empty,
        )
    }

    fun updateCurrenciesState(
        uiStateHolder: SwapStateHolder,
        emptyAmountState: SwapState.EmptyAmountState,
        fromSwapCurrencyStatus: SwapCurrencyStatus?,
        toSwapCurrencyStatus: SwapCurrencyStatus?,
        shouldResetAmount: Boolean,
    ): SwapStateHolder {
        return uiStateHolder.copy(
            sendCardData = uiStateHolder.sendCardData.updateCurrencyStatus(
                swapCurrencyStatus = fromSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = true,
                shouldResetAmount = shouldResetAmount,
            ),
            receiveCardData = uiStateHolder.receiveCardData.updateCurrencyStatus(
                swapCurrencyStatus = toSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = false,
                shouldResetAmount = shouldResetAmount,
            ),
            notifications = persistentListOf(),
            isInsufficientFunds = false,
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                walletInteractionIcon = fromSwapCurrencyStatus?.userWallet?.let(::walletInterationIcon),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus?.userWallet?.isHotWallet == true,
                onClick = { },
            ),
            shouldShowMaxAmount = shouldShowMaxAmount(fromSwapCurrencyStatus?.currency, toSwapCurrencyStatus?.currency),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = ProviderState.Empty(),
            priceImpact = PriceImpact.Empty,
        )
    }

    private fun SwapCardState.updateCurrencyStatus(
        swapCurrencyStatus: SwapCurrencyStatus?,
        emptyAmountState: SwapState.EmptyAmountState,
        shouldResetAmount: Boolean,
        isFromCard: Boolean,
    ): SwapCardState {
        val cardType = if (isFromCard) {
            TransactionCardType.Inputtable(
                onAmountChanged = actions.onAmountChanged,
                onFocusChanged = actions.onAmountSelected,
                inputError = TransactionCardType.InputError.Empty,
                accountTitleUM = getCardAccountTitle(swapCurrencyStatus?.account, true),
            )
        } else {
            TransactionCardType.ReadOnly(
                inputError = TransactionCardType.InputError.Empty,
                accountTitleUM = getCardAccountTitle(swapCurrencyStatus?.account, false),
            )
        }
        return if (this !is SwapCardState.SwapCardData || swapCurrencyStatus == null) {
            createCardState(
                swapCurrencyStatus = swapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = isFromCard,
            )
        } else if (shouldResetAmount) {
            copy(
                amountTextFieldValue = if (isFromCard) {
                    null
                } else {
                    TextFieldValue("0".appendApproximateSign())
                },
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
                currencyIconState = iconStateConverter.convert(swapCurrencyStatus.status),
                tokenSymbol = stringReference(swapCurrencyStatus.currency.symbol),
                balance = swapCurrencyStatus.status.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
                type = cardType,
            )
        } else {
            copy(
                currencyIconState = iconStateConverter.convert(swapCurrencyStatus.status),
                tokenSymbol = stringReference(swapCurrencyStatus.currency.symbol),
                balance = swapCurrencyStatus.status.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
                type = cardType,
            )
        }
    }

    private fun createCardState(
        swapCurrencyStatus: SwapCurrencyStatus?,
        emptyAmountState: SwapState.EmptyAmountState,
        isFromCard: Boolean,
    ): SwapCardState {
        return if (swapCurrencyStatus == null) {
            getEmptyCardState(isFromCard = isFromCard, emptyAmountState = emptyAmountState)
        } else {
            SwapCardState.SwapCardData(
                type = if (isFromCard) {
                    TransactionCardType.Inputtable(
                        onAmountChanged = actions.onAmountChanged,
                        onFocusChanged = actions.onAmountSelected,
                        inputError = TransactionCardType.InputError.Empty,
                        accountTitleUM = getCardAccountTitle(swapCurrencyStatus.account, true),
                    )
                } else {
                    TransactionCardType.ReadOnly(
                        inputError = TransactionCardType.InputError.Empty,
                        accountTitleUM = getCardAccountTitle(swapCurrencyStatus.account, false),
                    )
                },
                amountTextFieldValue = if (isFromCard) {
                    null
                } else {
                    TextFieldValue("0".appendApproximateSign())
                },
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
                currencyIconState = iconStateConverter.convert(swapCurrencyStatus.status),
                tokenSymbol = stringReference(swapCurrencyStatus.currency.symbol),
                balance = swapCurrencyStatus.status.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            )
        }
    }

    private fun getEmptyCardState(isFromCard: Boolean, emptyAmountState: SwapState.EmptyAmountState) =
        SwapCardState.Empty(
            type = TransactionCardType.ReadOnly(
                inputError = TransactionCardType.InputError.Empty,
                accountTitleUM = AccountTitleUM.Text(
                    title = resourceReference(
                        if (isFromCard) R.string.swapping_from_title else R.string.swapping_to_title,
                    ),
                ),
            ),
            amountTextFieldValue = TextFieldValue(text = if (isFromCard) "0" else "0".appendApproximateSign()),
            amountEquivalent = emptyAmountState.zeroAmountEquivalent,
        )

    fun createSwapNotSupportedState(
        uiStateHolder: SwapStateHolder,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(
                    accountTitleUM = getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true),
                ),
                amountTextFieldValue = TextFieldValue(
                    text = "0",
                ),
                amountEquivalent = getFormattedFiatAmount(BigDecimal.ZERO),
                currencyIconState = iconStateConverter.convert(fromSwapCurrencyStatus.status),
                tokenSymbol = stringReference(fromSwapCurrencyStatus.currency.symbol),
                balance = fromSwapCurrencyStatus.status.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(
                    accountTitleUM = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                ),
                amountTextFieldValue = TextFieldValue(
                    text = "0",
                ),
                amountEquivalent = getFormattedFiatAmount(BigDecimal.ZERO),
                currencyIconState = iconStateConverter.convert(toSwapCurrencyStatus.status),
                tokenSymbol = stringReference(toSwapCurrencyStatus.currency.symbol),
                balance = toSwapCurrencyStatus.status.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            notifications = notificationsFactory.getSwapNotSupportedNotifications(),
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(fromSwapCurrencyStatus.userWallet),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet,
                onClick = { },
            ),
            changeCardsButtonState = ChangeCardsButtonState.DISABLED,
            providerState = ProviderState.Empty(),
            priceImpact = PriceImpact.Empty,
        )
    }

    @Suppress("LongParameterList")
    fun createQuotesLoadingState(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        uiStateHolder: SwapStateHolder,
    ): SwapStateHolder {
        val fromCurrency = fromSwapCurrencyStatus.currency
        val toCurrency = toSwapCurrencyStatus.currency
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = uiStateHolder.sendCardData.copy(
                type = TransactionCardType.Inputtable(
                    onAmountChanged = actions.onAmountChanged,
                    onFocusChanged = actions.onAmountSelected,
                    inputError = TransactionCardType.InputError.Empty,
                    accountTitleUM = getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true),
                ),
            ),
            receiveCardData = uiStateHolder.receiveCardData.copy(
                type = TransactionCardType.ReadOnly(
                    accountTitleUM = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                ),
                amountTextFieldValue = null,
                amountEquivalent = null,
            ),
            notifications = persistentListOf(),
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(fromSwapCurrencyStatus.userWallet),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet,
                onClick = {},
            ),
            providerState = ProviderState.Loading(),
            permissionUM = uiStateHolder.permissionUM,
            changeCardsButtonState = ChangeCardsButtonState.UPDATE_IN_PROGRESS,
            priceImpact = PriceImpact.Empty,
            shouldShowMaxAmount = shouldShowMaxAmount(fromCurrency, toCurrency),
        )
    }

    @Suppress("LongMethod", "LongParameterList")
    fun createQuotesLoadedState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuotesLoadedState,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        swapProvider: SwapProvider,
        bestRatedProviderId: String,
        isNeedBestRateBadge: Boolean,
        selectedFeeType: FeeType,
        needApplyFCARestrictions: Boolean,
        hideFee: Boolean,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val feeState = if (hideFee) FeeItemState.Empty else createFeeState(quoteModel.txFee, selectedFeeType)
        val fromSwapCurrencyStatus = quoteModel.fromTokenInfo.swapCurrencyStatus
        val toSwapCurrencyStatus = quoteModel.toTokenInfo.swapCurrencyStatus
        val isInsufficientFunds = isInsufficientFundsCondition(quoteModel)

        val notifications = notificationsFactory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            selectedFeeType = selectedFeeType,
            providerName = swapProvider.name,
            hideFee = hideFee,
        )

        val fromAccountTitleUM = when {
            isInsufficientFunds -> AccountTitleUM.Text(TextReference.Res(R.string.swapping_insufficient_funds))
            else -> getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true)
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
                    accountTitleUM = fromAccountTitleUM,
                )
            }
        }
        val priceImpact = quoteModel.priceImpact
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = sendInput,
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = uiStateHolder.sendCardData.amountEquivalent,
                currencyIconState = iconStateConverter.convert(fromSwapCurrencyStatus.status),
                tokenSymbol = stringReference(fromSwapCurrencyStatus.currency.symbol),
                balance = fromSwapCurrencyStatus.status.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(
                    shouldShowWarning = true,
                    onWarningClick = actions.onReceiveCardWarningClick,
                    accountTitleUM = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                ),
                amountTextFieldValue = TextFieldValue(
                    quoteModel.toTokenInfo.tokenAmount
                        .formatToUIRepresentation()
                        .appendApproximateSign(),
                ),
                amountEquivalent = if (priceImpact.type.ordinal > PriceImpact.Type.LOW.ordinal) {
                    combinedReference(
                        getFormattedFiatAmount(quoteModel.toTokenInfo.amountFiat),
                        stringReference(StringsSigns.WHITE_SPACE),
                        styledStringReference(
                            value = "(${StringsSigns.MINUS}${priceImpact.value.format { percent() }})",
                            spanStyleReference = {
                                SpanStyle(
                                    color = when (priceImpact.type) {
                                        PriceImpact.Type.HIGH -> TangemTheme.colors.text.warning
                                        PriceImpact.Type.MEDIUM -> TangemTheme.colors.text.attention
                                        else -> TangemTheme.colors.text.tertiary
                                    },
                                )
                            },
                        ),
                    )
                } else {
                    getFormattedFiatAmount(quoteModel.toTokenInfo.amountFiat)
                },
                currencyIconState = iconStateConverter.convert(toSwapCurrencyStatus.status),
                tokenSymbol = stringReference(toSwapCurrencyStatus.currency.symbol),
                balance = toSwapCurrencyStatus.status.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            isInsufficientFunds = isInsufficientFundsCondition(quoteModel),
            notifications = notifications,
            permissionUM = convertPermissionState(
                permissionDataState = quoteModel.permissionState,
            ),
            fee = feeState,
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(fromSwapCurrencyStatus.userWallet),
                isEnabled = getSwapButtonEnabled(notifications, priceImpact),
                isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet,
                onClick = actions.onSwapClick,
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = swapProvider.convertToContentClickableProviderState(
                isBestRate = bestRatedProviderId == swapProvider.providerId && !priceImpact.shouldShowWarning(),
                fromTokenInfo = quoteModel.fromTokenInfo,
                toTokenInfo = quoteModel.toTokenInfo,
                isNeedBestRateBadge = isNeedBestRateBadge,
                selectionType = ProviderState.SelectionType.CLICK,
                onProviderClick = actions.onProviderClick,
                needApplyFCARestrictions = needApplyFCARestrictions,
                permissionState = quoteModel.permissionState,
            ),
            priceImpact = priceImpact,
            tosState = createTosState(swapProvider),
            shouldShowMaxAmount = shouldShowMaxAmount(fromSwapCurrencyStatus.currency, toSwapCurrencyStatus.currency),
        )
    }

    private fun shouldShowMaxAmount(fromToken: CryptoCurrency?, toCurrency: CryptoCurrency?): Boolean {
        return !(fromToken is CryptoCurrency.Coin && fromToken.network.id == toCurrency?.network?.id)
    }

    private fun createTosState(swapProvider: SwapProvider): TosState {
        return TosState(
            tosLink = swapProvider.termsOfUse?.let { termsUrl ->
                LegalState(
                    title = resourceReference(R.string.common_terms_of_use),
                    link = termsUrl,
                    onClick = actions.onLinkClick,
                )
            },
            policyLink = swapProvider.privacyPolicy?.let { policyUrl ->
                LegalState(
                    title = resourceReference(R.string.common_privacy_policy),
                    link = policyUrl,
                    onClick = actions.onLinkClick,
                )
            },
        )
    }

    private fun isInsufficientFundsCondition(quoteModel: SwapState.QuotesLoadedState): Boolean {
        return !quoteModel.preparedSwapConfigState.isBalanceEnough &&
            quoteModel.preparedSwapConfigState.includeFeeInAmount !is IncludeFeeInAmount.Included
    }

    private fun getSwapButtonEnabled(notifications: ImmutableList<NotificationUM>, priceImpact: PriceImpact): Boolean {
        return notifications.none { notification ->
            notification is SwapNotificationUM.Error || notification is NotificationUM.Error ||
                notification is SwapNotificationUM.Warning.ExpressError ||
                notification is SwapNotificationUM.Warning.ExpressGeneralError ||
                notification is SwapNotificationUM.Warning.NoAvailableTokensToSwap ||
                notification is SwapNotificationUM.Warning.SwapNotSupported ||
                notification is SwapNotificationUM.Warning.NeedReserveToCreateAccount ||
                notification is SwapNotificationUM.Info.PermissionNeeded
        } && !priceImpact.shouldDisableButton()
    }

    @Suppress("LongParameterList")
    fun createQuotesErrorState(
        uiStateHolder: SwapStateHolder,
        swapProvider: SwapProvider,
        fromToken: TokenSwapInfo,
        toSwapCurrencyStatus: SwapCurrencyStatus?,
        includeFeeInAmount: IncludeFeeInAmount,
        expressDataError: ExpressDataError,
        needApplyFCARestrictions: Boolean,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val fromSwapCurrencyStatus = fromToken.swapCurrencyStatus

        val notifications = notificationsFactory.getQuotesErrorStateNotifications(
            expressDataError = expressDataError,
            fromToken = fromSwapCurrencyStatus.currency,
            feeItem = uiStateHolder.fee,
            includeFeeInAmount = includeFeeInAmount,
        )

        val providerState = getProviderStateForError(
            swapProvider = swapProvider,
            fromToken = fromSwapCurrencyStatus.currency,
            expressDataError = expressDataError,
            onProviderClick = actions.onProviderClick,
            selectionType = ProviderState.SelectionType.CLICK,
            needApplyFCARestrictions = needApplyFCARestrictions,
        )
        val type = TransactionCardType.ReadOnly(
            accountTitleUM = getCardAccountTitle(
                toSwapCurrencyStatus?.account,
                isFromCard = false,
            ),
        )
        val receiveCardData = toSwapCurrencyStatus?.status?.let { toToken ->
            SwapCardState.SwapCardData(
                type = type,
                amountTextFieldValue = TextFieldValue(
                    text = "0",
                ),
                amountEquivalent = getFormattedFiatAmount(BigDecimal.ZERO),
                currencyIconState = iconStateConverter.convert(toSwapCurrencyStatus.status),
                tokenSymbol = stringReference(toSwapCurrencyStatus.currency.symbol),
                balance = toToken.getFormattedAmount(isNeedSymbol = false),
                isBalanceHidden = isBalanceHiddenProvider(),
            )
        } ?: SwapCardState.Empty(
            type = type,
            amountEquivalent = getFormattedFiatAmount(BigDecimal.ZERO),
            amountTextFieldValue = null,
        )
        return uiStateHolder.copy(
            receiveCardData = receiveCardData,
            notifications = notifications,
            permissionUM = SwapPermissionUM.Empty,
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(fromSwapCurrencyStatus.userWallet),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet,
                onClick = actions.onSwapClick,
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = providerState,
            priceImpact = PriceImpact.Empty,
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
        fromSwapCurrencyStatus: SwapCurrencyStatus?,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            sendCardData = uiStateHolder.sendCardData.copy(
                amountTextFieldValue = uiStateHolder.sendCardData.amountTextFieldValue,
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
            ),
            receiveCardData = uiStateHolder.receiveCardData.copy(
                amountTextFieldValue = TextFieldValue("0"),
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
            ),
            notifications = persistentListOf(),
            isInsufficientFunds = false,
            fee = FeeItemState.Empty,
            swapButton = SwapButton(
                walletInteractionIcon = fromSwapCurrencyStatus?.userWallet?.let(::walletInterationIcon),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus?.userWallet?.isHotWallet == true,
                onClick = { },
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = ProviderState.Empty(),
            priceImpact = PriceImpact.Empty,
        )
    }

    fun createSwapInProgressState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            swapButton = uiState.swapButton.copy(
                isEnabled = false,
                isInProgress = true,
            ),
        )
    }

    fun createSilentLoadState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            changeCardsButtonState = ChangeCardsButtonState.UPDATE_IN_PROGRESS,
            notifications = uiState.notifications
                .filterNot { it is SwapNotificationUM.Info.PermissionNeeded }
                .toImmutableList(),
        )
    }

    @Suppress("LongParameterList")
    fun updateSwapAmount(
        uiState: SwapStateHolder,
        amountFormatted: String,
        amountRaw: String,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        minTxAmount: BigDecimal?,
    ): SwapStateHolder {
        if (uiState.sendCardData !is SwapCardState.SwapCardData) return uiState
        val amountToSend = amountRaw.toBigDecimalOrNull()
        val sendInput = if (minTxAmount != null && amountToSend != null && amountToSend < minTxAmount) {
            val minAmountFormatted = minTxAmount.format {
                crypto(cryptoCurrency = fromSwapCurrencyStatus.currency, ignoreSymbolPosition = true)
            }
            (uiState.sendCardData.type as? TransactionCardType.Inputtable)?.copy(
                inputError = TransactionCardType.InputError.WrongAmount,
                accountTitleUM = AccountTitleUM.Text(
                    resourceReference(R.string.transfer_min_amount_error, wrappedList(minAmountFormatted)),
                ),
            ) ?: uiState.sendCardData.type
        } else {
            (uiState.sendCardData.type as? TransactionCardType.Inputtable)?.copy(
                inputError = TransactionCardType.InputError.Empty,
                accountTitleUM = getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true),
            ) ?: uiState.sendCardData.type
        }
        return uiState.copy(
            sendCardData = uiState.sendCardData.copy(
                amountTextFieldValue = TextFieldValue(
                    text = amountFormatted,
                    selection = TextRange(amountFormatted.length),
                ),
                amountEquivalent = getFormattedFiatAmount(
                    fromSwapCurrencyStatus.status.value.fiatRate?.let { fiatRate ->
                        amountToSend?.multiply(fiatRate)
                    },
                ),
                type = sendInput,
            ),
        )
    }

    fun updateCurrencyBalanceStatus(
        uiState: SwapStateHolder,
        fromSwapCurrencyStatus: SwapCurrencyStatus?,
        toSwapCurrencyStatus: SwapCurrencyStatus?,
        emptyAmountState: SwapState.EmptyAmountState,
    ): SwapStateHolder {
        return uiState.copy(
            sendCardData = uiState.sendCardData.updateCurrencyStatus(
                swapCurrencyStatus = fromSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = true,
                shouldResetAmount = false,
            ),
            receiveCardData = uiState.receiveCardData.updateCurrencyStatus(
                swapCurrencyStatus = toSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = false,
                shouldResetAmount = false,
            ),
        )
    }

    fun updateBalanceHiddenState(uiState: SwapStateHolder, isBalanceHidden: Boolean): SwapStateHolder {
        val patchedSendCardData = (uiState.sendCardData as? SwapCardState.SwapCardData)?.copy(
            isBalanceHidden = isBalanceHidden,
        ) ?: uiState.sendCardData

        val patchedReceiveCardData = (uiState.receiveCardData as? SwapCardState.SwapCardData)?.copy(
            isBalanceHidden = isBalanceHidden,
        ) ?: uiState.receiveCardData

        return uiState.copy(
            sendCardData = patchedSendCardData,
            receiveCardData = patchedReceiveCardData,
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
                isEnabled = false,
                isInProgress = false,
            ),
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
        val fromSwapCurrencyStatus = requireNotNull(dataState.fromSwapCurrencyStatus)
        val toSwapCurrencyStatus = requireNotNull(dataState.toSwapCurrencyStatus)
        val fromAmount = swapTransactionState.fromAmountValue ?: BigDecimal.ZERO
        val toAmount = swapTransactionState.toAmountValue ?: BigDecimal.ZERO
        val providerState = uiState.providerState as ProviderState.Content

        val fromFiatAmount = getFormattedFiatAmount(fromSwapCurrencyStatus.status.value.fiatRate?.multiply(fromAmount))
        val toFiatAmount = getFormattedFiatAmount(toSwapCurrencyStatus.status.value.fiatRate?.multiply(toAmount))

        val shouldShowStatus = providerState.type == ExchangeProviderType.CEX.providerName
        return uiState.copy(
            successState = SwapSuccessStateHolder(
                timestamp = swapTransactionState.timestamp,
                txUrl = txUrl,
                providerName = stringReference(providerState.name),
                providerType = stringReference(providerState.type),
                shouldShowStatusButton = shouldShowStatus,
                providerIcon = providerState.iconUrl,
                rate = providerState.subtitle,
                fee = dataState.selectedFee?.let { fee ->
                    stringReference("${fee.feeCryptoFormattedWithNative} (${fee.feeFiatFormattedWithNative})")
                },
                fromTitle = getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true),
                toTitle = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                fromTokenAmount = stringReference(swapTransactionState.fromAmount.orEmpty()),
                toTokenAmount = stringReference(swapTransactionState.toAmount.orEmpty()),
                fromTokenFiatAmount = fromFiatAmount,
                toTokenFiatAmount = toFiatAmount,
                fromTokenIconState = iconStateConverter.convert(fromSwapCurrencyStatus.status),
                toTokenIconState = iconStateConverter.convert(toSwapCurrencyStatus.status),
                onExploreButtonClick = onExploreClick,
                onStatusButtonClick = onStatusClick,
            ),
        )
    }

    fun createTangemPayWithdrawalSuccessState(
        uiState: SwapStateHolder,
        swapTransactionState: SwapTransactionState.TangemPayWithdrawalData,
        dataState: SwapProcessDataState,
        txUrl: String,
        onExploreClick: () -> Unit,
    ): SwapStateHolder {
        val fromSwapCurrencyStatus = requireNotNull(dataState.fromSwapCurrencyStatus)
        val toSwapCurrencyStatus = requireNotNull(dataState.toSwapCurrencyStatus)
        val fromAmount = swapTransactionState.fromAmountValue ?: BigDecimal.ZERO
        val toAmount = swapTransactionState.toAmountValue ?: BigDecimal.ZERO
        val providerState = uiState.providerState as ProviderState.Content

        val fromFiatAmount = getFormattedFiatAmount(fromSwapCurrencyStatus.status.value.fiatRate?.multiply(fromAmount))
        val toFiatAmount = getFormattedFiatAmount(toSwapCurrencyStatus.status.value.fiatRate?.multiply(toAmount))

        return uiState.copy(
            successState = SwapSuccessStateHolder(
                timestamp = System.currentTimeMillis(),
                txUrl = txUrl,
                providerName = stringReference(providerState.name),
                providerType = stringReference(providerState.type),
                shouldShowStatusButton = false,
                providerIcon = providerState.iconUrl,
                rate = providerState.subtitle,
                fee = TextReference.EMPTY,
                fromTitle = getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true),
                toTitle = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                fromTokenAmount = stringReference(swapTransactionState.fromAmount.orEmpty()),
                toTokenAmount = stringReference(swapTransactionState.toAmount.orEmpty()),
                fromTokenFiatAmount = fromFiatAmount,
                toTokenFiatAmount = toFiatAmount,
                fromTokenIconState = iconStateConverter.convert(fromSwapCurrencyStatus.status),
                toTokenIconState = iconStateConverter.convert(toSwapCurrencyStatus.status),
                onExploreButtonClick = onExploreClick,
                onStatusButtonClick = {},
            ),
        )
    }

    fun addNotification(uiState: SwapStateHolder, message: TextReference?, onClick: () -> Unit): SwapStateHolder {
        return uiState.copy(
            notifications = notificationsFactory.getGeneralErrorStateNotifications(
                message = message,
                onClick = onClick,
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun convertPermissionState(permissionDataState: PermissionDataState): SwapPermissionUM {
        return when (permissionDataState) {
            is PermissionDataState.PermissionRequired -> SwapPermissionUM.PermissionRequired(
                isResetApproval = permissionDataState.isResetApproval,
                spenderAddress = permissionDataState.spenderAddress,
            )
            else -> SwapPermissionUM.Empty
        }
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
            .mapNotNull { entry ->
                entry.convertToProviderBottomSheetState(
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
                        providers = providers.map { providerState ->
                            val tokenInfo = tokenSwapInfoForProviders[providerState.id]
                            if (providerState is ProviderState.Content && tokenInfo != null) {
                                val rateString = tokenInfo.tokenAmount
                                    .getFormattedCryptoAmount(tokenInfo.swapCurrencyStatus.currency)
                                providerState.copy(
                                    subtitle = stringReference(rateString),
                                    percentLowerThenBest = pricesLowerBest[providerState.id]?.let { percent ->
                                        PercentDifference.Value(percent)
                                    } ?: PercentDifference.Value(0f),
                                )
                            } else {
                                providerState
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
        readMoreUrl: String,
        onDismiss: () -> Unit,
    ): SwapStateHolder {
        val config = ChooseFeeBottomSheetConfig(
            selectedFee = selectedFee,
            onSelectFeeType = { feeType ->
                val selectedItem = when (feeType) {
                    FeeType.NORMAL -> txFeeState.normalFee
                    FeeType.PRIORITY -> txFeeState.priorityFee
                }
                actions.onSelectFeeType.invoke(selectedItem)
            },
            readMoreUrl = readMoreUrl,
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
                fromToken = state.fromTokenInfo.swapCurrencyStatus.currency,
                expressDataError = state.error,
                onProviderClick = onProviderSelect,
                selectionType = ProviderState.SelectionType.SELECT,
                needApplyFCARestrictions = needApplyFCARestrictions,
            )
        }
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
        permissionState: PermissionDataState,
    ): ProviderState {
        val rate = toTokenInfo.tokenAmount.value.calculateRate(
            fromTokenInfo.tokenAmount.value,
            toTokenInfo.swapCurrencyStatus.currency.decimals,
        )
        val fromCurrencySymbol = fromTokenInfo.swapCurrencyStatus.currency.symbol
        val rateString = buildString {
            append(BigDecimal.ONE.format { crypto(symbol = fromCurrencySymbol, decimals = 0).anyDecimals() })
            append(" ≈ ")
            append(rate.format { crypto(toTokenInfo.swapCurrencyStatus.currency) })
        }

        val additionalBadge = when {
            needApplyFCARestrictions && isFCARestrictedProvider() -> ProviderState.AdditionalBadge.FCAWarningList
            permissionState is PermissionDataState.PermissionRequired ->
                ProviderState.AdditionalBadge.PermissionRequired
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
        val rateString = toTokenInfo.tokenAmount.getFormattedCryptoAmount(toTokenInfo.swapCurrencyStatus.currency)

        val additionalBadge = when {
            needApplyFCARestrictions && isFCARestrictedProvider() -> ProviderState.AdditionalBadge.FCAWarningList
            state.permissionState is PermissionDataState.PermissionRequired -> {
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

    private fun CryptoCurrencyStatus?.getFormattedAmount(isNeedSymbol: Boolean): String {
        val amount = this?.value?.amount ?: return DASH_SIGN
        val symbol = if (isNeedSymbol) currency.symbol else ""
        return amount.format { crypto(symbol, currency.decimals) }
    }

    private fun getFormattedFiatAmount(amount: BigDecimal?): TextReference {
        val appCurrency = appCurrencyProvider()

        return stringReference(
            amount.format {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                )
            },
        )
    }

    private fun SwapAmount.getFormattedCryptoAmount(token: CryptoCurrency): String {
        return value.format { crypto(token) }
    }

    private fun BigDecimal.calculateRate(to: BigDecimal, decimals: Int): BigDecimal {
        val rateDecimals = if (decimals == 0) IF_ZERO_DECIMALS_TO_SHOW else decimals
        return this.divide(to, min(rateDecimals, MAX_DECIMALS_TO_SHOW), RoundingMode.HALF_UP)
    }

    private fun String.appendApproximateSign(): String {
        return "$TILDE_SIGN $this"
    }

    private fun SwapProvider.isFCARestrictedProvider(): Boolean {
        return FCA_RESTRICTED_PROVIDER_IDS.contains(providerId)
    }

    private fun getCardAccountTitle(account: Account?, isFromCard: Boolean): AccountTitleUM {
        val (prefix, placeholder) = if (isFromCard) {
            R.string.common_from to R.string.swapping_from_title
        } else {
            R.string.common_to to R.string.swapping_to_title
        }
        return if (account != null && isAccountsModeProvider()) {
            AccountTitleUM.Account(
                prefixText = resourceReference(prefix),
                name = account.accountName.toUM().value,
                icon = account.toIconUM(),
            )
        } else {
            AccountTitleUM.Text(resourceReference(placeholder))
        }
    }

    private fun Account.toIconUM(): AccountIconUM {
        return when (this) {
            is Account.CryptoPortfolio -> CryptoPortfolioIconConverter.convert(icon)
            is Account.Payment -> AccountIconUM.Payment
        }
    }

    private companion object {
        private const val MAX_DECIMALS_TO_SHOW = 8
        private const val IF_ZERO_DECIMALS_TO_SHOW = 2

        private val FCA_RESTRICTED_PROVIDER_IDS = setOf(
            "changelly",
            "changenow",
            "okx-cross-chain",
            "okx-on-chain",
            "simpleswap",
        )
    }
}