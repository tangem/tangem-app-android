package com.tangem.feature.swap.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldConverter
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.buttons.predefined.PredefinedPercentButtonUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.swap.models.PredefinedPercentAmount
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.converters.SwapProviderResolver
import com.tangem.feature.swap.converters.SwapProviderStateBuilder
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.model.SwapNotificationsFactory
import com.tangem.feature.swap.model.SwapProcessDataState
import com.tangem.feature.swap.model.getLastLoadedSuccessStates
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.SwapButton.Mode
import com.tangem.feature.swap.models.states.*
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.features.send.api.entity.FeeSelectorUM
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.TILDE_SIGN
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/**
 * State builder creates a specific states for SwapScreen
 */
@Suppress("LargeClass", "TooManyFunctions", "LongParameterList")
internal class StateBuilder(
    private val actions: UiActions,
    private val isBalanceHiddenProvider: Provider<Boolean>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isAccountsModeProvider: Provider<Boolean>,
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork,
    private val swapFeatureToggles: SwapFeatureToggles,
    private val appRouter: AppRouter,
) {
    private val iconStateConverter by lazy(::CryptoCurrencyToIconStateConverter)

    private val amountScreenClickIntents by lazy(LazyThreadSafetyMode.NONE) {
        SwapAmountScreenClickIntents(actions)
    }

    private val notificationsFactory by lazy(LazyThreadSafetyMode.NONE) {
        SwapNotificationsFactory(
            actions = actions,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
            appCurrencyProvider = appCurrencyProvider,
        )
    }

    fun createInitialLoadingState(swapUIMode: SwapUIMode = SwapUIMode.Detailed): SwapStateHolder {
        return SwapStateHolder(
            titleId = R.string.common_swap,
            sendCardData = getEmptyCardState(
                isFromCard = true,
                emptyAmountState = SwapState.EmptyAmountState(TextReference.EMPTY),
            ),
            receiveCardData = getEmptyCardState(
                isFromCard = false,
                emptyAmountState = SwapState.EmptyAmountState(TextReference.EMPTY),
            ),
            swapButton = SwapButton(
                walletInteractionIcon = null,
                isEnabled = false,
                mode = Mode.SWAP_PROGRESSING,
                isHoldToConfirm = false,
                onClick = {},
            ),
            onRefresh = {},
            onBackClicked = actions.onBackClicked,
            onChangeCardsClicked = actions.onChangeCardsClicked,
            onMaxAmountSelected = actions.onMaxAmountSelected,
            changeCardsButtonState = ChangeCardsButtonState.DISABLED,
            onShowPermissionBottomSheet = actions.onApproveClick,
            onSelectTokenClick = actions.onSelectTokenClick,
            onSuccess = actions.onSuccess,
            providerState = ProviderState.Empty(),
            shouldShowMaxAmount = false,
            priceImpact = PriceImpact.Empty,
            isInsufficientFunds = false,
            swapUIMode = swapUIMode,
            onSwapUIModeChange = actions.onSwapUIModeChange,
            onSwapTypeMenuOpened = actions.onSwapTypeMenuOpened,
            onTronBannerShown = actions.onTronBannerShown,
            shouldShowAbMenu = swapFeatureToggles.isSwapAbEnabled,
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
                isEnabled = toSwapCurrencyStatus != null,
            ),
            receiveCardData = createCardState(
                swapCurrencyStatus = toSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = false,
                isEnabled = true,
            ),
            notifications = persistentListOf(),
            isInsufficientFunds = false,
            swapButton = SwapButton(
                walletInteractionIcon = fromSwapCurrencyStatus?.userWallet?.let(::walletInterationIcon),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus?.userWallet?.isHotWallet == true,
                onClick = { },
            ),
            shouldShowMaxAmount = shouldShowMaxAmount(
                fromSwapCurrencyStatus?.currency,
                toSwapCurrencyStatus?.currency,
                emptyAmountState.isTransferMode,
            ),
            predefinedButtons = createPredefinedButtons(
                fromToken = fromSwapCurrencyStatus?.currency,
                toCurrency = toSwapCurrencyStatus?.currency,
                isTransferMode = emptyAmountState.isTransferMode,
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = ProviderState.Empty(),
            priceImpact = PriceImpact.Empty,
        )
    }

    fun createInitialErrorState(
        fromSwapCurrencyStatus: SwapCurrencyStatus?,
        uiStateHolder: SwapStateHolder,
        expressError: ExpressError,
        onRetry: () -> Unit,
    ): SwapStateHolder {
        return uiStateHolder.copy(
            sendCardData = (uiStateHolder.sendCardData as? SwapCardState.SwapCardData)?.copy(
                type = (uiStateHolder.sendCardData.type as? TransactionCardType.Inputtable)?.copy(
                    isEnabled = false,
                ) ?: uiStateHolder.sendCardData.type,
            ) ?: uiStateHolder.sendCardData,
            notifications = notificationsFactory.getErrorStateNotification(
                expressError = expressError,
                onRetryClick = onRetry,
            ),
            permissionUM = SwapPermissionUM.Empty,
            swapButton = fromSwapCurrencyStatus?.let {
                SwapButton(
                    walletInteractionIcon = walletInterationIcon(fromSwapCurrencyStatus.userWallet),
                    isEnabled = false,
                    isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet,
                    onClick = actions.onSwapClick,
                )
            } ?: uiStateHolder.swapButton,
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = ProviderState.Empty(),
            priceImpact = PriceImpact.Empty,
            tosState = null,
        )
    }

    fun createInitialLoadingState(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        toSwapCurrencyStatus: SwapCurrencyStatus,
        uiStateHolder: SwapStateHolder,
    ): SwapStateHolder {
        val fromCurrency = fromSwapCurrencyStatus.currency
        val toCurrency = toSwapCurrencyStatus.currency
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        return uiStateHolder.copy(
            titleId = R.string.common_swap,
            sendCardData = uiStateHolder.sendCardData.copy(
                type = TransactionCardType.Inputtable(
                    onCurrencyChange = actions.onCurrencyChange,
                    onFocusChanged = actions.onAmountSelected,
                    inputError = TransactionCardType.InputError.Empty,
                    accountTitleUM = getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true),
                    isEnabled = true,
                ),
            ),
            receiveCardData = uiStateHolder.receiveCardData.copy(
                type = TransactionCardType.ReadOnly(
                    accountTitleUM = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                ),
            ),
            notifications = persistentListOf(),
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(fromSwapCurrencyStatus.userWallet),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet,
                onClick = {},
            ),
            providerState = ProviderState.Empty(),
            changeCardsButtonState = ChangeCardsButtonState.UPDATE_IN_PROGRESS,
            priceImpact = PriceImpact.Empty,
            shouldShowMaxAmount = shouldShowMaxAmount(fromCurrency, toCurrency),
            predefinedButtons = createPredefinedButtons(fromCurrency, toCurrency),
            transferFooter = null,
        )
    }

    fun updateCurrenciesState(
        uiStateHolder: SwapStateHolder,
        emptyAmountState: SwapState.EmptyAmountState,
        fromSwapCurrencyStatus: SwapCurrencyStatus?,
        toSwapCurrencyStatus: SwapCurrencyStatus?,
        shouldResetAmount: Boolean,
    ): SwapStateHolder {
        val shouldShowMaxAmount = shouldShowMaxAmount(
            fromSwapCurrencyStatus?.currency,
            toSwapCurrencyStatus?.currency,
            emptyAmountState.isTransferMode,
        )
        return uiStateHolder.copy(
            sendCardData = uiStateHolder.sendCardData.updateCurrencyStatus(
                swapCurrencyStatus = fromSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = true,
                shouldResetAmount = shouldResetAmount,
                isEnabled = toSwapCurrencyStatus != null,
            ),
            receiveCardData = uiStateHolder.receiveCardData.updateCurrencyStatus(
                swapCurrencyStatus = toSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = false,
                shouldResetAmount = shouldResetAmount,
                isEnabled = true,
            ),
            notifications = persistentListOf(),
            isInsufficientFunds = false,
            swapButton = SwapButton(
                walletInteractionIcon = fromSwapCurrencyStatus?.userWallet?.let(::walletInterationIcon),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus?.userWallet?.isHotWallet == true,
                onClick = { },
            ),
            shouldShowMaxAmount = shouldShowMaxAmount,
            predefinedButtons = createPredefinedButtons(
                fromToken = fromSwapCurrencyStatus?.currency,
                toCurrency = toSwapCurrencyStatus?.currency,
                isTransferMode = emptyAmountState.isTransferMode,
            ),
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
        isEnabled: Boolean,
    ): SwapCardState {
        val cardType = if (isFromCard) {
            TransactionCardType.Inputtable(
                onCurrencyChange = actions.onCurrencyChange,
                onFocusChanged = actions.onAmountSelected,
                inputError = TransactionCardType.InputError.Empty,
                accountTitleUM = getCardAccountTitle(swapCurrencyStatus?.account, true),
                isEnabled = isEnabled,
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
                isEnabled = isEnabled,
            )
        } else if (shouldResetAmount) {
            copy(
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
                currencyIconState = iconStateConverter.convert(swapCurrencyStatus.status),
                tokenSymbol = stringReference(swapCurrencyStatus.currency.symbol),
                balance = swapCurrencyStatus.status.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
                type = cardType,
                amountField = if (isFromCard) {
                    emptyAmountField(swapCurrencyStatus)
                } else {
                    displayAmountField("0".appendApproximateSign(), swapCurrencyStatus)
                },
            )
        } else {
            copy(
                currencyIconState = iconStateConverter.convert(swapCurrencyStatus.status),
                tokenSymbol = stringReference(swapCurrencyStatus.currency.symbol),
                balance = swapCurrencyStatus.status.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
                type = cardType,
                amountField = if (isFromCard) {
                    if (amountField == null) {
                        emptyAmountField(swapCurrencyStatus)
                    } else {
                        buildAmountField(
                            amountRaw = amountField.cryptoAmount.value?.toPlainString().orEmpty(),
                            fieldValue = amountField.value,
                            isFiatValue = amountField.isFiatValue,
                            fromSwapCurrencyStatus = swapCurrencyStatus,
                            isPastedAmount = false,
                        )
                    }
                } else {
                    amountField
                },
            )
        }
    }

    private fun emptyAmountField(fromSwapCurrencyStatus: SwapCurrencyStatus): AmountFieldModel = buildAmountField(
        amountRaw = "",
        fieldValue = "",
        isFiatValue = false,
        fromSwapCurrencyStatus = fromSwapCurrencyStatus,
        isPastedAmount = false,
    )

    /**
     * Builds the read-only receive card [AmountFieldModel] from a display [value]. Reuses the existing
     * converter path; the read-only UI only reads [AmountFieldModel.value].
     */
    private fun displayAmountField(value: String, status: SwapCurrencyStatus): AmountFieldModel = buildAmountField(
        amountRaw = "",
        fieldValue = value,
        isFiatValue = false,
        fromSwapCurrencyStatus = status,
        isPastedAmount = false,
    )

    /**
     * Builds a status-free placeholder [AmountFieldModel] for the static [SwapCardState.Empty] card,
     * which has no [SwapCurrencyStatus]. The Empty card UI only reads [AmountFieldModel.value].
     */
    private fun placeholderAmountField(value: String): AmountFieldModel = AmountFieldModel(
        value = value,
        onValueChange = {},
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = KeyboardType.Number),
        keyboardActions = KeyboardActions(),
        cryptoAmount = Amount(currencySymbol = "", value = BigDecimal.ZERO, decimals = 0),
        fiatAmount = Amount(currencySymbol = "", value = BigDecimal.ZERO, decimals = 0),
        isFiatValue = false,
        fiatValue = "",
        isFiatUnavailable = false,
        isValuePasted = false,
        onValuePastedTriggerDismiss = {},
        isError = false,
        isWarning = false,
        error = TextReference.EMPTY,
    )

    private fun createCardState(
        swapCurrencyStatus: SwapCurrencyStatus?,
        emptyAmountState: SwapState.EmptyAmountState,
        isFromCard: Boolean,
        isEnabled: Boolean,
    ): SwapCardState {
        return if (swapCurrencyStatus == null) {
            getEmptyCardState(isFromCard = isFromCard, emptyAmountState = emptyAmountState)
        } else {
            SwapCardState.SwapCardData(
                type = if (isFromCard) {
                    TransactionCardType.Inputtable(
                        onCurrencyChange = actions.onCurrencyChange,
                        onFocusChanged = actions.onAmountSelected,
                        inputError = TransactionCardType.InputError.Empty,
                        accountTitleUM = getCardAccountTitle(swapCurrencyStatus.account, true),
                        isEnabled = isEnabled,
                    )
                } else {
                    TransactionCardType.ReadOnly(
                        inputError = TransactionCardType.InputError.Empty,
                        accountTitleUM = getCardAccountTitle(swapCurrencyStatus.account, false),
                    )
                },
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
                currencyIconState = iconStateConverter.convert(swapCurrencyStatus.status),
                tokenSymbol = stringReference(swapCurrencyStatus.currency.symbol),
                balance = swapCurrencyStatus.status.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
                amountField = if (isFromCard) {
                    emptyAmountField(swapCurrencyStatus)
                } else {
                    displayAmountField("0".appendApproximateSign(), swapCurrencyStatus)
                },
                appCurrency = appCurrencyProvider(),
            )
        }
    }

    private fun getEmptyCardState(isFromCard: Boolean, emptyAmountState: SwapState.EmptyAmountState) =
        SwapCardState.Empty(
            type = TransactionCardType.ReadOnly(
                inputError = TransactionCardType.InputError.Empty,
                accountTitleUM = AccountTitleUM.Text(
                    title = resourceReference(
                        if (isFromCard) R.string.swapping_from_title_v2 else R.string.swapping_to_title,
                    ),
                ),
            ),
            amountField = placeholderAmountField(value = if (isFromCard) "0" else "0".appendApproximateSign()),
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
                amountField = displayAmountField(value = "0", status = fromSwapCurrencyStatus),
                amountEquivalent = getFormattedFiatAmount(BigDecimal.ZERO),
                currencyIconState = iconStateConverter.convert(fromSwapCurrencyStatus.status),
                tokenSymbol = stringReference(fromSwapCurrencyStatus.currency.symbol),
                balance = fromSwapCurrencyStatus.status.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
                appCurrency = appCurrencyProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(
                    accountTitleUM = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                ),
                amountField = displayAmountField(value = "0", status = toSwapCurrencyStatus),
                amountEquivalent = getFormattedFiatAmount(BigDecimal.ZERO),
                currencyIconState = iconStateConverter.convert(toSwapCurrencyStatus.status),
                tokenSymbol = stringReference(toSwapCurrencyStatus.currency.symbol),
                balance = toSwapCurrencyStatus.status.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
                appCurrency = appCurrencyProvider(),
            ),
            notifications = notificationsFactory.getSwapNotSupportedNotifications(),
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
                    onCurrencyChange = actions.onCurrencyChange,
                    onFocusChanged = actions.onAmountSelected,
                    inputError = TransactionCardType.InputError.Empty,
                    accountTitleUM = getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true),
                    isEnabled = true,
                ),
            ),
            receiveCardData = uiStateHolder.receiveCardData.copy(
                type = TransactionCardType.ReadOnly(
                    accountTitleUM = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                ),
                amountField = null,
                amountEquivalent = null,
            ),
            notifications = persistentListOf(),
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
            predefinedButtons = createPredefinedButtons(fromCurrency, toCurrency),
        )
    }

    @Suppress("LongMethod", "LongParameterList")
    fun createQuotesLoadedState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuotesLoadedState,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        swapProvider: SwapProvider,
        additionalBadge: ProviderState.AdditionalBadge,
        swapFee: SwapFee?,
        feeError: FeeSelectorUM.Error?,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val fromSwapCurrencyStatus = quoteModel.fromTokenInfo.swapCurrencyStatus
        val toSwapCurrencyStatus = quoteModel.toTokenInfo.swapCurrencyStatus
        val isInsufficientFunds = isInsufficientFundsCondition(quoteModel)

        val notifications = notificationsFactory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            swapFee = swapFee,
            feeError = feeError?.error,
            appRouter = appRouter,
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
        val isTangemPayWithdrawal = fromSwapCurrencyStatus.account is Account.Payment
        return uiStateHolder.copy(
            sendCardData = SwapCardState.SwapCardData(
                type = sendInput,
                amountEquivalent = uiStateHolder.sendCardData.amountEquivalent,
                currencyIconState = iconStateConverter.convert(fromSwapCurrencyStatus.status),
                tokenSymbol = stringReference(fromSwapCurrencyStatus.currency.symbol),
                balance = fromSwapCurrencyStatus.status.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
                amountField = uiStateHolder.sendCardData.amountField,
                appCurrency = appCurrencyProvider(),
            ),
            receiveCardData = SwapCardState.SwapCardData(
                type = TransactionCardType.ReadOnly(
                    shouldShowWarning = true,
                    onWarningClick = actions.onReceiveCardWarningClick,
                    accountTitleUM = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                ),
                amountField = displayAmountField(
                    value = quoteModel.toTokenInfo.tokenAmount
                        .formatToUIRepresentation()
                        .appendApproximateSign(),
                    status = toSwapCurrencyStatus,
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
                balance = toSwapCurrencyStatus.status.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
                appCurrency = appCurrencyProvider(),
            ),
            isInsufficientFunds = isInsufficientFundsCondition(quoteModel),
            notifications = notifications,
            permissionUM = convertPermissionState(
                permissionDataState = quoteModel.permissionState,
            ),
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(fromSwapCurrencyStatus.userWallet),
                isEnabled = getSwapButtonEnabled(
                    notifications = notifications,
                    priceImpact = priceImpact,
                    swapFee = swapFee,
                    isTangemPayWithdrawal = isTangemPayWithdrawal,
                ),
                isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet,
                onClick = actions.onSwapClick,
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = SwapProviderStateBuilder.buildContentClickable(
                provider = swapProvider,
                state = quoteModel,
                selectionType = ProviderState.SelectionType.CLICK,
                additionalBadge = additionalBadge,
                onProviderClick = actions.onProviderClick,
            ),
            priceImpact = priceImpact,
            tosState = createTosState(swapProvider),
            shouldShowMaxAmount = shouldShowMaxAmount(fromSwapCurrencyStatus.currency, toSwapCurrencyStatus.currency),
            predefinedButtons = createPredefinedButtons(fromSwapCurrencyStatus.currency, toSwapCurrencyStatus.currency),
        )
    }

    fun createFeeErrorState(
        uiStateHolder: SwapStateHolder,
        quoteModel: SwapState.QuotesLoadedState,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus?,
        feeError: GetFeeError,
    ): SwapStateHolder {
        val fromSwapCurrencyStatus = quoteModel.fromTokenInfo.swapCurrencyStatus
        if (feeCryptoCurrencyStatus == null) return uiStateHolder

        val notifications = notificationsFactory.getConfirmationStateNotifications(
            quoteModel = quoteModel,
            feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
            swapFee = null,
            feeError = feeError,
            appRouter = appRouter,
        )

        return uiStateHolder.copy(
            notifications = notifications,
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(fromSwapCurrencyStatus.userWallet),
                isEnabled = false,
                isHoldToConfirm = fromSwapCurrencyStatus.userWallet.isHotWallet,
                onClick = actions.onSwapClick,
            ),
        )
    }

    private fun shouldShowMaxAmount(
        fromToken: CryptoCurrency?,
        toCurrency: CryptoCurrency?,
        isTransferMode: Boolean = false,
    ): Boolean {
        if (isTransferMode) return true
        return !(fromToken is CryptoCurrency.Coin && fromToken.network.id == toCurrency?.network?.id)
    }

    /**
     * Builds the predefined percent buttons once per state update (off the composition path).
     * The row is gated by the feature toggle; the MAX button is included only when
     * [shouldShowMaxAmount] is `true` (e.g. it is dropped for a native coin swapped within the same
     * network, where spending the full balance would leave nothing for the network fee).
     */
    private fun createPredefinedButtons(
        fromToken: CryptoCurrency?,
        toCurrency: CryptoCurrency?,
        isTransferMode: Boolean = false,
    ): ImmutableList<PredefinedPercentButtonUM> {
        if (!swapFeatureToggles.isSwapPredefinedButtonsEnabled) return persistentListOf()
        val shouldShowMaxAmount = shouldShowMaxAmount(fromToken, toCurrency, isTransferMode)
        return PredefinedPercentAmount.entries
            .filter { it != PredefinedPercentAmount.MAX || shouldShowMaxAmount }
            .map { percent ->
                PredefinedPercentButtonUM(
                    id = percent.name,
                    label = percent.toLabel(),
                    onClick = { actions.onPredefinedPercentSelected(percent) },
                )
            }
            .toImmutableList()
    }

    private fun PredefinedPercentAmount.toLabel(): TextReference = when (this) {
        PredefinedPercentAmount.PERCENT_25 -> stringReference("25%")
        PredefinedPercentAmount.PERCENT_50 -> stringReference("50%")
        PredefinedPercentAmount.PERCENT_75 -> stringReference("75%")
        PredefinedPercentAmount.MAX -> resourceReference(R.string.send_max_amount)
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
        return quoteModel.preparedSwapConfigState.balanceStatus is SwapBalanceStatus.InsufficientAmount
    }

    private fun getSwapButtonEnabled(
        notifications: ImmutableList<NotificationUM>,
        priceImpact: PriceImpact,
        swapFee: SwapFee?,
        isTangemPayWithdrawal: Boolean,
    ): Boolean {
        val isSwapTxReady = isTangemPayWithdrawal || swapFee != null
        return isSwapTxReady && notifications.none { notification ->
            notification is SwapNotificationUM.Error || notification is NotificationUM.Error ||
                notification is SwapNotificationUM.Warning.ExpressErrorWarning ||
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
        balanceStatus: SwapBalanceStatus,
        expressDataError: ExpressDataError,
        additionalBadge: ProviderState.AdditionalBadge,
        swapFee: SwapFee?,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val fromSwapCurrencyStatus = fromToken.swapCurrencyStatus

        val notifications = notificationsFactory.getQuotesErrorStateNotifications(
            expressDataError = expressDataError,
            fromToken = fromSwapCurrencyStatus.currency,
            balanceStatus = balanceStatus,
            swapFee = swapFee,
        )

        val providerState = getProviderStateForError(
            swapProvider = swapProvider,
            fromToken = fromSwapCurrencyStatus.currency,
            expressDataError = expressDataError,
            onProviderClick = actions.onProviderClick,
            selectionType = ProviderState.SelectionType.CLICK,
            additionalBadge = additionalBadge,
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
                amountField = displayAmountField(value = "0", status = toSwapCurrencyStatus),
                amountEquivalent = getFormattedFiatAmount(BigDecimal.ZERO),
                currencyIconState = iconStateConverter.convert(toSwapCurrencyStatus.status),
                tokenSymbol = stringReference(toSwapCurrencyStatus.currency.symbol),
                balance = toToken.getFormattedAmount(),
                isBalanceHidden = isBalanceHiddenProvider(),
                appCurrency = appCurrencyProvider(),
            )
        } ?: SwapCardState.Empty(
            type = type,
            amountEquivalent = getFormattedFiatAmount(BigDecimal.ZERO),
            amountField = null,
        )
        return uiStateHolder.copy(
            receiveCardData = receiveCardData,
            notifications = notifications,
            permissionUM = SwapPermissionUM.Empty,
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
        additionalBadge: ProviderState.AdditionalBadge,
    ): ProviderState {
        return when (expressDataError) {
            is ExpressDataError.ExchangeTooSmallAmountError -> {
                SwapProviderStateBuilder.buildAvailableFrom(
                    provider = swapProvider,
                    alertText = resourceReference(
                        R.string.express_provider_min_amount,
                        wrappedList(expressDataError.amount.getFormattedCryptoAmount(fromToken)),
                    ),
                    selectionType = selectionType,
                    additionalBadge = additionalBadge,
                    onProviderClick = onProviderClick,
                )
            }
            is ExpressDataError.ExchangeTooBigAmountError -> {
                SwapProviderStateBuilder.buildAvailableFrom(
                    provider = swapProvider,
                    alertText = resourceReference(
                        R.string.express_provider_max_amount,
                        wrappedList(expressDataError.amount.getFormattedCryptoAmount(fromToken)),
                    ),
                    selectionType = selectionType,
                    additionalBadge = additionalBadge,
                    onProviderClick = onProviderClick,
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
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
            ),
            receiveCardData = uiStateHolder.receiveCardData.copy(
                amountField = uiStateHolder.receiveCardData.amountField?.copy(value = "0"),
                amountEquivalent = emptyAmountState.zeroAmountEquivalent,
            ),
            notifications = persistentListOf(),
            isInsufficientFunds = false,
            swapButton = SwapButton(
                walletInteractionIcon = fromSwapCurrencyStatus?.userWallet?.let(::walletInterationIcon),
                isEnabled = false,
                mode = if (emptyAmountState.isTransferMode) Mode.TRANSFER else Mode.SWAP,
                isHoldToConfirm = fromSwapCurrencyStatus?.userWallet?.isHotWallet == true,
                onClick = { },
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            providerState = ProviderState.Empty(),
            priceImpact = PriceImpact.Empty,
            transferFooter = null,
        )
    }

    fun createSwapInProgressState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            swapButton = uiState.swapButton.copy(
                isEnabled = false,
                mode = Mode.SWAP_PROGRESSING,
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

    /**
     * Builds the shared [AmountFieldModel] that carries the "from" card input state.
     *
     * @param amountRaw authoritative crypto amount (ungrouped) — drives [AmountFieldModel.cryptoAmount].
     * @param fieldValue value currently shown in the input field, expressed in the active currency.
     * @param isFiatValue whether the active input currency is fiat.
     */
    private fun buildAmountField(
        amountRaw: String,
        fieldValue: String,
        isFiatValue: Boolean,
        isPastedAmount: Boolean,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
    ): AmountFieldModel {
        val appCurrency = appCurrencyProvider()
        val fiatRate = fromSwapCurrencyStatus.status.value.fiatRate
        val cryptoDecimal = amountRaw.parseBigDecimalOrNull() ?: BigDecimal.ZERO
        val fiatValue = fiatRate?.multiply(cryptoDecimal).format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }

        return AmountFieldConverter(
            clickIntents = amountScreenClickIntents,
            cryptoCurrencyStatus = fromSwapCurrencyStatus.status,
            appCurrency = appCurrency,
        ).convert(value = amountRaw).copy(
            value = fieldValue,
            isFiatValue = isFiatValue,
            fiatValue = fiatValue,
            isValuePasted = isPastedAmount,
        )
    }

    @Suppress("LongParameterList")
    fun updateSwapAmount(
        uiState: SwapStateHolder,
        amountRaw: String,
        fieldValue: String,
        isFiatValue: Boolean,
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        minTxAmount: BigDecimal?,
        isPastedAmount: Boolean,
    ): SwapStateHolder {
        if (uiState.sendCardData !is SwapCardState.SwapCardData) return uiState
        val amountToSend = amountRaw.parseBigDecimalOrNull()
        val currency = fromSwapCurrencyStatus.currency
        val fiatRate = fromSwapCurrencyStatus.status.value.fiatRate
        val isFiatUnavailable = fiatRate == null
        val isFiatEffective = isFiatValue && !isFiatUnavailable
        val sendInput = if (minTxAmount != null && amountToSend != null && amountToSend < minTxAmount) {
            val minAmountFormatted = minTxAmount.format {
                crypto(cryptoCurrency = currency, ignoreSymbolPosition = true)
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
        // The secondary line shows the opposite currency: crypto when entering fiat, fiat otherwise.
        val amountEquivalent = if (isFiatEffective) {
            stringReference(amountToSend.orZero().format { crypto(currency) })
        } else {
            getFormattedFiatAmount(fiatRate?.let { amountToSend?.multiply(it).orZero() })
        }
        return uiState.copy(
            sendCardData = uiState.sendCardData.copy(
                amountField = buildAmountField(
                    amountRaw = amountRaw,
                    fieldValue = fieldValue,
                    isFiatValue = isFiatEffective,
                    fromSwapCurrencyStatus = fromSwapCurrencyStatus,
                    isPastedAmount = isPastedAmount,
                ),
                amountEquivalent = amountEquivalent,
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
                isEnabled = toSwapCurrencyStatus != null,
            ),
            receiveCardData = uiState.receiveCardData.updateCurrencyStatus(
                swapCurrencyStatus = toSwapCurrencyStatus,
                emptyAmountState = emptyAmountState,
                isFromCard = false,
                shouldResetAmount = false,
                isEnabled = true,
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

    fun loadingPermissionState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            swapButton = uiState.swapButton.copy(
                isEnabled = false,
                mode = Mode.SWAP,
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
        swapFee: SwapFee?,
    ): SwapStateHolder {
        val fromSwapCurrencyStatus = requireNotNull(dataState.fromSwapCurrencyStatus)
        val toSwapCurrencyStatus = requireNotNull(dataState.toSwapCurrencyStatus)
        val fromAmount = swapTransactionState.fromAmountValue ?: BigDecimal.ZERO
        val toAmount = swapTransactionState.toAmountValue ?: BigDecimal.ZERO
        val providerState = uiState.providerState as ProviderState.Content

        val fromFiatAmount = getFormattedFiatAmount(fromSwapCurrencyStatus.status.value.fiatRate?.multiply(fromAmount))
        val toFiatAmount = getFormattedFiatAmount(toSwapCurrencyStatus.status.value.fiatRate?.multiply(toAmount))

        val shouldShowStatus = providerState.type == ExchangeProviderType.CEX.providerName
        val isFloatRate = dataState.selectedProvider?.rateTypes?.contains(RateType.FLOAT) == true
        return uiState.copy(
            successState = SwapSuccessStateHolder(
                timestamp = swapTransactionState.timestamp,
                txUrl = txUrl,
                providerName = stringReference(providerState.name),
                providerType = stringReference(providerState.type),
                shouldShowStatusButton = shouldShowStatus,
                isTransferMode = false,
                providerIcon = providerState.iconUrl,
                rate = providerState.subtitle,
                fee = swapFee?.let { fee -> formatSwapFeeForSuccess(fee) },
                fromTitle = getCardAccountTitle(fromSwapCurrencyStatus.account, isFromCard = true),
                toTitle = getCardAccountTitle(toSwapCurrencyStatus.account, isFromCard = false),
                fromTokenAmount = stringReference(swapTransactionState.fromAmount.orEmpty()),
                toTokenAmount = stringReference(
                    swapTransactionState.toAmount.orEmpty()
                        .let { if (isFloatRate) it.appendApproximateSign() else it },
                ),
                fromTokenFiatAmount = fromFiatAmount,
                toTokenFiatAmount = toFiatAmount,
                fromTokenIconState = iconStateConverter.convert(fromSwapCurrencyStatus.status),
                toTokenIconState = iconStateConverter.convert(toSwapCurrencyStatus.status),
                navigationUM = swapSuccessNavigation(txUrl = txUrl, exploreClick = onExploreClick),
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
                isTransferMode = false,
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
                navigationUM = swapSuccessNavigation(txUrl = txUrl, exploreClick = onExploreClick),
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
        isSwapBestDexRateEnabled: Boolean,
        onDismiss: () -> Unit,
    ): SwapStateHolder {
        val successStates = providersStates.getLastLoadedSuccessStates()
        val availableProvidersStates = providersStates.entries
            .mapNotNull { entry ->
                val additionalBadge = SwapProviderResolver.resolveBadge(
                    states = successStates,
                    provider = entry.key,
                    needApplyFCARestrictions = needApplyFCARestrictions,
                    state = entry.value,
                    isSwapBestDexRateEnabled = isSwapBestDexRateEnabled,
                )
                entry.convertToProviderBottomSheetState(
                    pricesLowerBest = pricesLowerBest,
                    onProviderSelect = actions.onProviderSelect,
                    onApprovalSelectClick = actions.onApproveTypeSelect,
                    additionalBadge = additionalBadge,
                )
            }
            .sortedWith(ProviderPercentDiffComparator)
            .toImmutableList()

        val isAnyFCABadge = availableProvidersStates.any {
            (it as? ProviderState.Content)?.additionalBadge == ProviderState.AdditionalBadge.FCAWarningList
        }
        val hasCex = availableProvidersStates.any { state ->
            (state as? ProviderState.Content)?.type == ExchangeProviderType.CEX.providerName ||
                (state as? ProviderState.Unavailable)?.type == ExchangeProviderType.CEX.providerName
        }
        val hasDex = availableProvidersStates.any { state ->
            val providerType = (state as? ProviderState.Content)?.type ?: (state as? ProviderState.Unavailable)?.type
            providerType == ExchangeProviderType.DEX.providerName ||
                providerType == ExchangeProviderType.DEX_BRIDGE.providerName
        }
        val availableFilters = if (swapFeatureToggles.isSwapProviderFilterEnabled && hasCex && hasDex) {
            persistentListOf(ProviderFilterType.ALL, ProviderFilterType.CEX, ProviderFilterType.DEX)
        } else {
            persistentListOf()
        }
        val config = ChooseProviderBottomSheetConfig(
            selectedProviderId = selectedProviderId,
            providers = availableProvidersStates,
            allProviders = availableProvidersStates,
            notification = SwapNotificationUM.Error.FCAWarningList.takeIf { isAnyFCABadge },
            selectedFilter = ProviderFilterType.ALL,
            availableFilters = availableFilters,
            onFilterSelect = actions.onProviderFilterSelect,
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
            fun updateState(providerState: ProviderState): ProviderState {
                val tokenInfo = tokenSwapInfoForProviders[providerState.id]
                return if (providerState is ProviderState.Content && tokenInfo != null) {
                    providerState.copy(
                        subtitle = SwapProviderStateBuilder.buildSelectableSubtitle(tokenInfo),
                        percentLowerThenBest = pricesLowerBest[providerState.id]?.let { percent ->
                            PercentDifference.Value(percent)
                        } ?: PercentDifference.Value(0f),
                    )
                } else {
                    providerState
                }
            }
            uiState.copy(
                bottomSheetConfig = uiState.bottomSheetConfig.copy(
                    content = config.copy(
                        providers = config.providers.map(::updateState).toImmutableList(),
                        allProviders = config.allProviders.map(::updateState).toImmutableList(),
                    ),
                ),
            )
        } else {
            uiState
        }
    }

    fun updateProviderFilterType(uiState: SwapStateHolder, filterType: ProviderFilterType): SwapStateHolder {
        val config = uiState.bottomSheetConfig?.content as? ChooseProviderBottomSheetConfig ?: return uiState
        val filtered = config.allProviders.filter { matchesTypeFilter(it, filterType) }.toImmutableList()
        return uiState.copy(
            bottomSheetConfig = uiState.bottomSheetConfig.copy(
                content = config.copy(
                    providers = filtered,
                    selectedFilter = filterType,
                ),
            ),
        )
    }

    private fun formatSwapFeeForSuccess(swapFee: SwapFee): TextReference {
        val feeAmount = swapFee.fee.amount
        val totalFeeValue = (feeAmount.value ?: BigDecimal.ZERO) + swapFee.otherNativeFee
        val cryptoFormatted = totalFeeValue.format {
            crypto(symbol = feeAmount.currencySymbol, decimals = feeAmount.decimals)
        }
        val appCurrency = appCurrencyProvider()
        val fiatRate = swapFee.selectedFeeToken.value.fiatRate
        val fiatFormatted = fiatRate?.multiply(totalFeeValue).format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }
        return stringReference("$cryptoFormatted ($fiatFormatted)")
    }

    private fun Map.Entry<SwapProvider, SwapState>.convertToProviderBottomSheetState(
        pricesLowerBest: Map<String, Float>,
        onProviderSelect: (String) -> Unit,
        onApprovalSelectClick: (SwapProvider) -> Unit,
        additionalBadge: ProviderState.AdditionalBadge,
    ): ProviderState? {
        val (provider, state) = this
        return when (state) {
            is SwapState.EmptyAmountState, is SwapState.Transfer -> null
            is SwapState.QuotesLoadedState -> {
                SwapProviderStateBuilder.buildContentSelectable(
                    provider = provider,
                    state = state,
                    pricesLowerBest = pricesLowerBest,
                    selectionType = ProviderState.SelectionType.SELECT,
                    additionalBadge = additionalBadge,
                    onProviderClick = onProviderSelect,
                    onApprovalSelectClick = onApprovalSelectClick,
                )
            }
            is SwapState.SwapError -> getProviderStateForError(
                swapProvider = provider,
                fromToken = state.fromTokenInfo.swapCurrencyStatus.currency,
                expressDataError = state.error,
                onProviderClick = onProviderSelect,
                selectionType = ProviderState.SelectionType.SELECT,
                additionalBadge = additionalBadge,
            )
        }
    }

    private fun CryptoCurrencyStatus?.getFormattedAmount(): TextReference {
        if (this == null) return stringReference(DASH_SIGN)
        return resourceReference(
            R.string.common_balance,
            wrappedList(this.value.amount.format { crypto(currency.symbol, currency.decimals) }),
        )
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

    private fun String.appendApproximateSign(): String {
        return "$TILDE_SIGN $this"
    }

    private fun getCardAccountTitle(account: Account?, isFromCard: Boolean): AccountTitleUM {
        val (prefix, placeholder) = if (isFromCard) {
            R.string.swapping_from_account_title to R.string.swapping_from_title_v2
        } else {
            R.string.swapping_to_account_title to R.string.swapping_to_title
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
            is Account.Virtual -> AccountIconUM.Virtual
        }
    }

    private fun matchesTypeFilter(state: ProviderState, filterType: ProviderFilterType): Boolean {
        val typeStr = when (state) {
            is ProviderState.Content -> state.type
            is ProviderState.Unavailable -> state.type
            else -> null
        } ?: return filterType == ProviderFilterType.ALL
        return when (filterType) {
            ProviderFilterType.ALL -> true
            ProviderFilterType.CEX -> typeStr == ExchangeProviderType.CEX.providerName
            ProviderFilterType.DEX -> typeStr == ExchangeProviderType.DEX.providerName ||
                typeStr == ExchangeProviderType.DEX_BRIDGE.providerName
        }
    }
}