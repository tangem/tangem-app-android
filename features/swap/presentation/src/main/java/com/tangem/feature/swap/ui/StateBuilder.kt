package com.tangem.feature.swap.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.common.ui.bottomsheet.permission.state.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.converters.SwapTransactionErrorStateConverter
import com.tangem.feature.swap.converters.TokensDataConverter
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.*
import com.tangem.feature.swap.domain.models.ui.*
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.states.*
import com.tangem.feature.swap.models.states.events.SwapEvent
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.feature.swap.utils.getExpressErrorMessage
import com.tangem.feature.swap.utils.getExpressErrorTitle
import com.tangem.feature.swap.viewmodels.SwapProcessDataState
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.PERCENT
import com.tangem.utils.StringsSigns.TILDE_SIGN
import kotlinx.collections.immutable.ImmutableList
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
                    isError = false,
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
                networkIconRes = getActiveIconRes(initialCurrencyFrom.network.id.value),
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
                networkIconRes = initialCurrencyTo?.let { getActiveIconRes(it.network.id.value) },
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
            providerState = ProviderState.Empty(),
            shouldShowMaxAmount = false,
            priceImpact = PriceImpact.Empty(),
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
                networkIconRes = getActiveIconRes(fromToken.currency.network.id.value),
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
        val sendInput = if (sendInputType.isError) {
            sendInputType
        } else {
            sendInputType.copy(
                isError = false,
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
                networkIconRes = getActiveIconRes(fromToken.network.id.value),
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
                networkIconRes = getActiveIconRes(toToken.network.id.value),
                isBalanceHidden = isBalanceHiddenProvider(),
            ),
            warnings = emptyList(),
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
        swapProvider: SwapProvider,
        bestRatedProviderId: String,
        isNeedBestRateBadge: Boolean,
        selectedFeeType: FeeType,
        isReverseSwapPossible: Boolean,
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val warnings = getWarningsForSuccessState(
            quoteModel = quoteModel,
            fromToken = fromToken,
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
        val sendInput = if (sendCardType.isError && !isInsufficientFunds) {
            // if any error in inputField and funds enough -> show that error else show fund is not enough error
            sendCardType
        } else {
            sendCardType.copy(
                isError = isInsufficientFunds,
                header = insufficientFundsHeader,
            )
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
            warnings = warnings,
            permissionState = convertPermissionState(
                lastPermissionState = uiStateHolder.permissionState,
                permissionDataState = quoteModel.permissionState,
                providerName = swapProvider.name,
                onGivePermissionClick = actions.onGivePermissionClick,
                onChangeApproveType = actions.onChangeApproveType,
            ),
            fee = feeState,
            swapButton = SwapButton(
                enabled = getSwapButtonEnabled(quoteModel),
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
                    onClick = actions.onTosClick,
                )
            },
            policyLink = swapProvider.privacyPolicy?.let {
                LegalState(
                    title = resourceReference(R.string.common_privacy_policy),
                    link = it,
                    onClick = actions.onPolicyClick,
                )
            },
        )
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun getWarningsForSuccessState(
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
        selectedFeeType: FeeType,
        providerName: String,
    ): List<SwapWarning> {
        val warnings = mutableListOf<SwapWarning>()
        maybeAddDomainWarnings(quoteModel, warnings)
        maybeAddNeedReserveToCreateAccountWarning(quoteModel, warnings)
        maybeAddPermissionNeededWarning(quoteModel, warnings, fromToken, providerName)
        maybeAddNetworkFeeCoverageWarning(quoteModel, warnings, selectedFeeType)
        maybeAddUnableCoverFeeWarning(quoteModel, fromToken, warnings)
        maybeAddInsufficientFundsWarning(quoteModel, warnings)
        maybeAddTransactionInProgressWarning(quoteModel, warnings)
        return warnings
    }

    private fun maybeAddTransactionInProgressWarning(
        quoteModel: SwapState.QuotesLoadedState,
        warnings: MutableList<SwapWarning>,
    ) {
        if (quoteModel.permissionState is PermissionDataState.PermissionLoading) {
            warnings.add(
                SwapWarning.TransactionInProgressWarning(
                    title = resourceReference(R.string.warning_express_approval_in_progress_title),
                    description = resourceReference(R.string.warning_express_approval_in_progress_message),
                ),
            )
        } else if (quoteModel.preparedSwapConfigState.hasOutgoingTransaction) {
            warnings.add(
                SwapWarning.TransactionInProgressWarning(
                    title = resourceReference(R.string.warning_express_active_transaction_title),
                    description = resourceReference(
                        id = R.string.warning_express_active_transaction_message,
                        formatArgs = wrappedList(
                            quoteModel.fromTokenInfo.cryptoCurrencyStatus.currency.network.currencySymbol,
                        ),
                    ),
                ),
            )
        }
    }

    private fun maybeAddDomainWarnings(quoteModel: SwapState.QuotesLoadedState, warnings: MutableList<SwapWarning>) {
        quoteModel.warnings.forEach {
            when (it) {
                is Warning.ExistentialDepositWarning -> {
                    warnings.add(createLeaveExistentialDepositWarning(quoteModel, it))
                }
                is Warning.MinAmountWarning -> {
                    warnings.add(
                        SwapWarning.GeneralError(
                            NotificationConfig(
                                title = resourceReference(R.string.send_notification_invalid_amount_title),
                                subtitle = resourceReference(
                                    R.string.warning_express_dust_message,
                                    wrappedList(
                                        it.dustValue.toPlainString(),
                                        it.dustValue.toPlainString(),
                                    ),
                                ),
                                iconResId = R.drawable.ic_alert_circle_24,
                            ),
                        ),
                    )
                }
                is Warning.ReduceAmountWarning -> {
                    warnings.add(
                        SwapWarning.ReduceAmount(
                            notificationConfig = createReduceAmountNotificationConfig(
                                currencyName = quoteModel.fromTokenInfo.cryptoCurrencyStatus.currency.name,
                                amount = it.tezosFeeThreshold.toPlainString(),
                                onConfirmClick = {
                                    val fromAmount = quoteModel.fromTokenInfo.tokenAmount
                                    val patchedAmount = fromAmount.copy(
                                        value = fromAmount.value - it.tezosFeeThreshold,
                                    )
                                    actions.onReduceAmount(patchedAmount)
                                },
                            ),
                        ),
                    )
                }
                Warning.Cardano.InsufficientBalanceToTransferCoin -> {
                    warnings.add(createInsufficientBalanceToTransferCoin())
                }
                is Warning.Cardano.InsufficientBalanceToTransferToken -> {
                    warnings.add(createInsufficientBalanceToTransferToken(tokenName = it.tokenName))
                }
                is Warning.Cardano.MinAdaValueCharged -> {
                    warnings.add(createMinAdaValueCharged(minAdaValue = it.minAdaValue, tokenName = it.tokenName))
                }
            }
        }
    }

    private fun maybeAddNeedReserveToCreateAccountWarning(
        quoteModel: SwapState.QuotesLoadedState,
        warnings: MutableList<SwapWarning>,
    ) {
        val status = quoteModel.toTokenInfo.cryptoCurrencyStatus.value
        if (status is CryptoCurrencyStatus.NoAccount) {
            val amount = quoteModel.toTokenInfo.tokenAmount.value
            val amountToCreateAccount = status.amountToCreateAccount

            if (amount < amountToCreateAccount) {
                warnings.add(
                    SwapWarning.NeedReserveToCreateAccount(
                        notificationConfig = createActivateAccountNotificationConfig(
                            status.amountToCreateAccount,
                            quoteModel.toTokenInfo.cryptoCurrencyStatus.currency.symbol,
                        ),
                    ),
                )
            }
        }
    }

    private fun createLeaveExistentialDepositWarning(
        quoteModel: SwapState.QuotesLoadedState,
        domainWarning: Warning.ExistentialDepositWarning,
    ): SwapWarning {
        val fromCurrency = quoteModel.fromTokenInfo.cryptoCurrencyStatus.currency
        val deposit = domainWarning.existentialDeposit.format { crypto(fromCurrency).uncapped() }

        return SwapWarning.GeneralError(
            NotificationConfig(
                title = resourceReference(R.string.send_notification_existential_deposit_title),
                subtitle = resourceReference(
                    R.string.send_notification_existential_deposit_text,
                    wrappedList(deposit),
                ),
                iconResId = R.drawable.ic_alert_circle_24,
                buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                    text = resourceReference(R.string.common_ok),
                    onClick = {
                        actions.onLeaveExistentialDeposit(
                            SwapAmount(
                                domainWarning.minAvailableAmount,
                                fromCurrency.decimals,
                            ),
                        )
                    },
                ),
            ),
        )
    }

    private fun maybeAddPermissionNeededWarning(
        quoteModel: SwapState.QuotesLoadedState,
        warnings: MutableList<SwapWarning>,
        fromToken: CryptoCurrency,
        providerName: String,
    ) {
        if (!quoteModel.preparedSwapConfigState.isAllowedToSpend &&
            quoteModel.preparedSwapConfigState.feeState is SwapFeeState.Enough &&
            quoteModel.permissionState is PermissionDataState.PermissionReadyForRequest
        ) {
            warnings.add(
                SwapWarning.PermissionNeeded(
                    createPermissionNotificationConfig(fromToken.symbol, providerName),
                ),
            )
        }
    }

    private fun maybeAddNetworkFeeCoverageWarning(
        quoteModel: SwapState.QuotesLoadedState,
        warnings: MutableList<SwapWarning>,
        selectedFeeType: FeeType,
    ) {
        when (quoteModel.preparedSwapConfigState.includeFeeInAmount) {
            is IncludeFeeInAmount.Included -> {
                val fee = selectFeeByType(selectedFeeType, quoteModel.txFee) ?: return
                if (needShowNetworkFeeCoverageWarningShow(quoteModel)) {
                    warnings.add(
                        SwapWarning.GeneralWarning(
                            createNetworkFeeCoverageNotificationConfig(
                                fee.feeCryptoFormattedWithNative,
                                fee.feeFiatFormattedWithNative,
                            ),
                        ),
                    )
                }
            }
            else -> Unit
        }
    }

    private fun needShowNetworkFeeCoverageWarningShow(quoteModel: SwapState.QuotesLoadedState): Boolean {
        return quoteModel.warnings.none { it is Warning.ExistentialDepositWarning }
    }

    private fun selectFeeByType(feeType: FeeType, txFeeState: TxFeeState): TxFee? {
        return when (txFeeState) {
            TxFeeState.Empty -> null
            is TxFeeState.SingleFeeState -> txFeeState.fee
            is TxFeeState.MultipleFeeState -> when (feeType) {
                FeeType.NORMAL -> txFeeState.normalFee
                FeeType.PRIORITY -> txFeeState.priorityFee
            }
        }
    }

    private fun maybeAddUnableCoverFeeWarning(
        quoteModel: SwapState.QuotesLoadedState,
        fromToken: CryptoCurrency,
        warnings: MutableList<SwapWarning>,
    ) {
        val feeEnoughState = quoteModel.preparedSwapConfigState.feeState as? SwapFeeState.NotEnough ?: return
        val needShowCoverWarning = quoteModel.preparedSwapConfigState.isBalanceEnough &&
            quoteModel.permissionState !is PermissionDataState.PermissionLoading &&
            feeEnoughState.feeCurrency != fromToken
        if (needShowCoverWarning) {
            warnings.add(
                SwapWarning.UnableToCoverFeeWarning(
                    createUnableToCoverFeeNotificationConfig(
                        fromToken = fromToken,
                        feeCurrency = feeEnoughState.feeCurrency,
                        currencyName = feeEnoughState.currencyName ?: fromToken.network.name,
                        currencySymbol = feeEnoughState.currencySymbol ?: fromToken.network.currencySymbol,
                    ),
                ),
            )
        }
    }

    private fun maybeAddInsufficientFundsWarning(
        quoteModel: SwapState.QuotesLoadedState,
        warnings: MutableList<SwapWarning>,
    ) {
        // check isBalanceEnough, but for dex includeFeeInAmount always Excluded
        if (isInsufficientFundsCondition(quoteModel)) {
            warnings.add(SwapWarning.InsufficientFunds)
        }
    }

    private fun isInsufficientFundsCondition(quoteModel: SwapState.QuotesLoadedState): Boolean {
        return !quoteModel.preparedSwapConfigState.isBalanceEnough &&
            quoteModel.preparedSwapConfigState.includeFeeInAmount !is IncludeFeeInAmount.Included
    }

    private fun getSwapButtonEnabled(quoteModel: SwapState.QuotesLoadedState): Boolean {
        val status = quoteModel.toTokenInfo.cryptoCurrencyStatus.value
        if (status is CryptoCurrencyStatus.NoAccount) {
            val amount = quoteModel.toTokenInfo.tokenAmount.value
            val amountToCreateAccount = status.amountToCreateAccount

            if (amount < amountToCreateAccount) {
                return false
            }
        }

        val preparedSwapConfigState = quoteModel.preparedSwapConfigState
        // check has has outgoing transaction
        if (preparedSwapConfigState.hasOutgoingTransaction) return false

        // check has MinAmountWarning warning
        val hasCriticalWarning = quoteModel.warnings.any {
            it is Warning.MinAmountWarning ||
                it is Warning.Cardano.InsufficientBalanceToTransferCoin ||
                it is Warning.Cardano.InsufficientBalanceToTransferToken ||
                it is Warning.ExistentialDepositWarning
        }

        if (hasCriticalWarning) return false

        return when (preparedSwapConfigState.includeFeeInAmount) {
            IncludeFeeInAmount.BalanceNotEnough -> false
            IncludeFeeInAmount.Excluded ->
                preparedSwapConfigState.isAllowedToSpend &&
                    preparedSwapConfigState.isBalanceEnough &&
                    preparedSwapConfigState.feeState is SwapFeeState.Enough
            is IncludeFeeInAmount.Included -> true
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
    ): SwapStateHolder {
        if (uiStateHolder.sendCardData !is SwapCardState.SwapCardData) return uiStateHolder
        if (uiStateHolder.receiveCardData !is SwapCardState.SwapCardData) return uiStateHolder
        val warnings = mutableListOf<SwapWarning>()
        warnings.add(getWarningForError(expressDataError, fromToken.cryptoCurrencyStatus.currency))
        if (includeFeeInAmount is IncludeFeeInAmount.Included && uiStateHolder.fee is FeeItemState.Content) {
            val feeCoverageNotification = createNetworkFeeCoverageNotificationConfig(
                uiStateHolder.fee.amountCrypto,
                uiStateHolder.fee.amountFiatFormatted,
            )
            warnings.add(SwapWarning.GeneralWarning(feeCoverageNotification))
        }
        val providerState = getProviderStateForError(
            swapProvider = swapProvider,
            fromToken = fromToken.cryptoCurrencyStatus.currency,
            expressDataError = expressDataError,
            onProviderClick = actions.onProviderClick,
            selectionType = ProviderState.SelectionType.CLICK,
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
            warnings = warnings,
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

    private fun getProviderStateForError(
        swapProvider: SwapProvider,
        fromToken: CryptoCurrency,
        expressDataError: ExpressDataError,
        onProviderClick: (String) -> Unit,
        selectionType: ProviderState.SelectionType,
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
                )
            }
            else -> {
                ProviderState.Empty()
            }
        }
    }

    private fun getWarningForError(expressDataError: ExpressDataError, fromToken: CryptoCurrency): SwapWarning {
        val providerErrorMessage = getExpressErrorMessage(expressDataError)
        val providerErrorTitle = getExpressErrorTitle(expressDataError)
        return when (expressDataError) {
            is ExpressDataError.ExchangeTooSmallAmountError -> SwapWarning.GeneralError(
                notificationConfig = NotificationConfig(
                    title = resourceReference(
                        id = R.string.warning_express_too_minimal_amount_title,
                        formatArgs = wrappedList(expressDataError.amount.getFormattedCryptoAmount(fromToken)),
                    ),
                    subtitle = resourceReference(R.string.warning_express_wrong_amount_description),
                    iconResId = R.drawable.ic_alert_circle_24,
                ),
            )
            is ExpressDataError.ExchangeTooBigAmountError -> SwapWarning.GeneralError(
                notificationConfig = NotificationConfig(
                    title = resourceReference(
                        id = R.string.warning_express_too_maximum_amount_title,
                        formatArgs = wrappedList(expressDataError.amount.getFormattedCryptoAmount(fromToken)),
                    ),
                    subtitle = resourceReference(R.string.warning_express_wrong_amount_description),
                    iconResId = R.drawable.ic_alert_circle_24,
                ),
            )
            is ExpressDataError.ProviderDifferentAmountError -> SwapWarning.GeneralError(
                notificationConfig = NotificationConfig(
                    title = resourceReference(id = R.string.common_error),
                    subtitle = resourceReference(
                        R.string.express_error_provider_amount_roundup,
                        formatArgs = wrappedList(
                            expressDataError.code,
                            expressDataError.fromProviderAmount.format { simple(decimals = expressDataError.decimals) },
                        ),
                    ),
                    iconResId = R.drawable.ic_alert_circle_24,
                ),
            )
            else -> SwapWarning.GeneralWarning(
                notificationConfig = NotificationConfig(
                    title = providerErrorTitle,
                    subtitle = providerErrorMessage,
                    iconResId = R.drawable.img_attention_20,
                    buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                        text = resourceReference(R.string.warning_button_refresh),
                        onClick = actions.onRetryClick,
                    ),
                ),
            )
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
            warnings = emptyList(),
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
                isError = true,
                header = resourceReference(R.string.transfer_min_amount_error, wrappedList(minAmountFormatted)),
            ) ?: uiState.sendCardData.type
        } else {
            (uiState.sendCardData.type as? TransactionCardType.Inputtable)?.copy(
                isError = false,
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
            warnings = listOf(
                SwapWarning.GeneralWarning(
                    notificationConfig = NotificationConfig(
                        title = TextReference.Res(R.string.warning_express_refresh_required_title),
                        subtitle = TextReference.Res(R.string.express_error_code, wrappedList(code)),
                        iconResId = R.drawable.ic_alert_triangle_20,
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
            title = resourceReference(R.string.common_network_fee_title),
            amountCrypto = fee.feeCryptoFormattedWithNative, // display fee with native as workaround for okx
            symbolCrypto = fee.cryptoSymbol,
            amountFiatFormatted = fee.feeFiatFormattedWithNative, // display fee with native as workaround for okx
            isClickable = isClickable,
            onClick = actions.onClickFee,
        )
    }

    fun loadingPermissionState(uiState: SwapStateHolder): SwapStateHolder {
        val warnings = uiState.warnings.filterNot { it is SwapWarning.PermissionNeeded }.toMutableList()
        warnings.add(
            0,
            SwapWarning.TransactionInProgressWarning(
                title = resourceReference(R.string.warning_express_approval_in_progress_title),
                description = resourceReference(R.string.warning_express_approval_in_progress_message),
            ),
        )
        return uiState.copy(
            permissionState = GiveTxPermissionState.InProgress,
            warnings = warnings,
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
                                formatArgs = wrappedList(token, slippage),
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

    fun addWarning(uiState: SwapStateHolder, message: TextReference?, onClick: () -> Unit): SwapStateHolder {
        val renewWarnings = uiState.warnings.toMutableList()
        renewWarnings.add(
            SwapWarning.GenericWarning(
                message = message,
                onClick = onClick,
            ),
        )
        return uiState.copy(
            warnings = renewWarnings,
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

    fun showWebViewBottomSheet(uiState: SwapStateHolder, url: String, onDismiss: () -> Unit): SwapStateHolder {
        val config = WebViewBottomSheetConfig(url = url)
        return uiState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = onDismiss,
                content = config,
            ),
        )
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
        pricesLowerBest: Map<String, Float>,
        providersStates: Map<SwapProvider, SwapState>,
        onDismiss: () -> Unit,
    ): SwapStateHolder {
        val availableProvidersStates = providersStates.entries
            .mapNotNull {
                it.convertToProviderBottomSheetState(pricesLowerBest, actions.onProviderSelect)
            }
            .sortedWith(ProviderPercentDiffComparator)
            .toImmutableList()
        val config = ChooseProviderBottomSheetConfig(
            selectedProviderId = selectedProviderId,
            providers = availableProvidersStates,
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
            readMoreUrl = buildReadMoreUrl(),
            feeItems = txFeeState.toFeeItemState(),
            readMore = resourceReference(R.string.common_read_more),
            onReadMoreClick = actions.onFeeReadMoreClick,
        )
        return uiState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
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
                )
            }
            is SwapState.SwapError -> getProviderStateForError(
                swapProvider = provider,
                fromToken = state.fromTokenInfo.cryptoCurrencyStatus.currency,
                expressDataError = state.error,
                onProviderClick = onProviderSelect,
                selectionType = ProviderState.SelectionType.SELECT,
            )
        }
    }

    // region warnings
    private fun createPermissionNotificationConfig(fromTokenSymbol: String, providerName: String): NotificationConfig {
        return NotificationConfig(
            title = resourceReference(R.string.express_provider_permission_needed),
            subtitle = resourceReference(
                id = R.string.give_permission_swap_subtitle,
                formatArgs = wrappedList(providerName, fromTokenSymbol),
            ),
            iconResId = R.drawable.ic_locked_24,
        )
    }

    private fun createActivateAccountNotificationConfig(amount: BigDecimal, token: String): NotificationConfig {
        return NotificationConfig(
            title = resourceReference(
                id = R.string.send_notification_invalid_reserve_amount_title,
                formatArgs = wrappedList("$amount $token"),
            ),
            subtitle = resourceReference(R.string.send_notification_invalid_reserve_amount_text),
            iconResId = R.drawable.img_attention_20,
        )
    }

    private fun createReduceAmountNotificationConfig(
        currencyName: String,
        amount: String,
        onConfirmClick: () -> Unit,
    ): NotificationConfig {
        return NotificationConfig(
            title = resourceReference(R.string.send_notification_high_fee_title),
            subtitle = resourceReference(R.string.send_notification_high_fee_text, wrappedList(currencyName, amount)),
            iconResId = R.drawable.img_attention_20,
            buttonsState = NotificationConfig.ButtonsState.PrimaryButtonConfig(
                text = resourceReference(R.string.xtz_withdrawal_message_reduce, wrappedList(amount)),
                onClick = onConfirmClick,
            ),
        )
    }

    private fun createUnableToCoverFeeNotificationConfig(
        fromToken: CryptoCurrency,
        feeCurrency: CryptoCurrency?,
        currencyName: String,
        currencySymbol: String,
    ): NotificationConfig {
        val buttonState = feeCurrency?.let {
            NotificationConfig.ButtonsState.SecondaryButtonConfig(
                text = resourceReference(R.string.common_buy_currency, wrappedList(currencySymbol)),
                onClick = { actions.onBuyClick(it) },
            )
        }
        return NotificationConfig(
            title = resourceReference(
                R.string.warning_express_not_enough_fee_for_token_tx_title,
                wrappedList(fromToken.network.name),
            ),
            subtitle = resourceReference(
                R.string.warning_express_not_enough_fee_for_token_tx_description,
                wrappedList(currencyName, currencySymbol),
            ),
            iconResId = fromToken.networkIconResId,
            buttonsState = buttonState,
        )
    }

    private fun createNetworkFeeCoverageNotificationConfig(
        cryptoAmount: String,
        fiatAmount: String,
    ): NotificationConfig {
        return NotificationConfig(
            title = resourceReference(R.string.send_network_fee_warning_title),
            subtitle = resourceReference(
                R.string.common_network_fee_warning_content,
                wrappedList(cryptoAmount, fiatAmount),
            ),
            iconResId = R.drawable.img_attention_20,
        )
    }

    private fun createMinAdaValueCharged(minAdaValue: String, tokenName: String): SwapWarning {
        return SwapWarning.Cardano.MinAdaValueCharged(
            NotificationConfig(
                title = resourceReference(id = R.string.cardano_coin_will_be_send_with_token_title),
                subtitle = resourceReference(
                    id = R.string.cardano_coin_will_be_send_with_token_description,
                    formatArgs = wrappedList(minAdaValue, tokenName),
                ),
                iconResId = R.drawable.img_attention_20,
            ),
        )
    }

    private fun createInsufficientBalanceToTransferCoin(): SwapWarning {
        return SwapWarning.Cardano.InsufficientBalanceToTransferCoin(
            NotificationConfig(
                title = resourceReference(id = R.string.cardano_max_amount_has_token_title),
                subtitle = resourceReference(id = R.string.cardano_max_amount_has_token_description),
                iconResId = R.drawable.ic_alert_circle_24,
            ),
        )
    }

    private fun createInsufficientBalanceToTransferToken(tokenName: String): SwapWarning {
        return SwapWarning.Cardano.InsufficientBalanceToTransferToken(
            NotificationConfig(
                title = resourceReference(id = R.string.cardano_insufficient_balance_to_send_token_title),
                subtitle = resourceReference(
                    id = R.string.cardano_insufficient_balance_to_send_token_description,
                    formatArgs = wrappedList(tokenName),
                ),
                iconResId = R.drawable.ic_alert_circle_24,
            ),
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
        isNeedBestRateBadge: Boolean,
        onProviderClick: (String) -> Unit,
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
        // val rateString = "1 $fromCurrencySymbol ≈ $rate $toCurrencySymbol"
        val badge = if (isRecommended) {
            ProviderState.AdditionalBadge.Recommended
        } else if (isNeedBestRateBadge && isBestRate) {
            ProviderState.AdditionalBadge.BestTrade
        } else {
            ProviderState.AdditionalBadge.Empty
        }
        return ProviderState.Content(
            id = this.providerId,
            name = this.name,
            iconUrl = this.imageLarge,
            type = this.type.providerName,
            subtitle = stringReference(rateString),
            additionalBadge = badge,
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
    ): ProviderState {
        val toTokenInfo = state.toTokenInfo
        val rateString = toTokenInfo.tokenAmount.getFormattedCryptoAmount(toTokenInfo.cryptoCurrencyStatus.currency)
        val additionalBadge = if (state.permissionState is PermissionDataState.PermissionReadyForRequest) {
            ProviderState.AdditionalBadge.PermissionRequired
        } else if (isRecommended) {
            ProviderState.AdditionalBadge.Recommended
        } else {
            ProviderState.AdditionalBadge.Empty
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
    ): ProviderState {
        val additionalBadge = if (swapProvider.isRecommended) {
            ProviderState.AdditionalBadge.Recommended
        } else {
            ProviderState.AdditionalBadge.Empty
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

    @Suppress("UnusedPrivateMember")
    private fun CryptoCurrencyStatus.getFormattedFiatAmount(): String {
        val fiatAmount = value.fiatAmount ?: return DASH_SIGN
        val appCurrency = appCurrencyProvider()

        return BigDecimalFormatter.formatFiatAmount(fiatAmount, appCurrency.code, appCurrency.symbol)
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

    private fun toBigDecimalOrNull(text: String): BigDecimal? {
        return text.replace(",", ".").toBigDecimalOrNull()
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
    }
}