package com.tangem.feature.swap.ui.transfer

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldConverter
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.model.SwapProcessDataState
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.SwapButton.Mode
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.ui.SwapAmountScreenClickIntents
import com.tangem.feature.swap.ui.swapSuccessNavigation
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.utils.formatFooterFiatFee
import com.tangem.features.send.api.utils.getTronTokenFeeSendingText
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LargeClass")
internal class SwapTransferStateBuilder @Inject constructor(
    private val notificationsFactory: SwapTransferNotificationsFactory,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
) {

    private val iconConverter by lazy(::CryptoCurrencyToIconStateConverter)

    @Suppress("LongParameterList")
    fun createTransferState(
        actions: UiActions,
        transferState: SwapState.Transfer,
        uiStateHolder: SwapStateHolder,
        feePaidCryptoCurrencyStatus: CryptoCurrencyStatus?,
        feeSelectorUM: FeeSelectorUM?,
    ): SwapStateHolder {
        val fromTokenSwapInfo = transferState.fromTokenInfo
        val isInsufficientBalance = transferState.isInsufficientBalance
        val prevSendCard = uiStateHolder.sendCardData as? SwapCardState.SwapCardData
        val prevAmountField = prevSendCard?.amountField
        val notifications = notificationsFactory.getNotifications(
            transferState = transferState,
            feeSelectorUM = feeSelectorUM,
            feeCryptoCurrencyStatus = feePaidCryptoCurrencyStatus,
            actions = actions,
        )
        return uiStateHolder.copy(
            sendCardData = createSendSwapCardState(
                actions = actions,
                tokenSwapInfo = fromTokenSwapInfo,
                appCurrency = transferState.appCurrency,
                isAccountsMode = transferState.isAccountsMode,
                isBalanceHidden = transferState.isBalanceHidden,
                isInsufficientBalance = isInsufficientBalance,
                prevAmountField = prevAmountField,
            ),
            receiveCardData = createReceiveCard(actions = actions, transferState = transferState),
            isInsufficientFunds = isInsufficientBalance,
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(transferState.userWallet),
                isEnabled = false,
                mode = Mode.TRANSFER,
                onClick = actions.onTransferClick,
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
            notifications = notifications,
        )
    }

    @Suppress("LongParameterList")
    private fun createSendSwapCardState(
        actions: UiActions,
        tokenSwapInfo: TokenSwapInfo,
        appCurrency: AppCurrency,
        isAccountsMode: Boolean,
        isBalanceHidden: Boolean,
        isInsufficientBalance: Boolean,
        prevAmountField: AmountFieldModel?,
    ): SwapCardState {
        val swapCurrencyStatus = tokenSwapInfo.swapCurrencyStatus
        val currency = swapCurrencyStatus.currency

        return SwapCardState.SwapCardData(
            type = createSendTransactionCardType(
                actions = actions,
                swapCurrencyStatus = tokenSwapInfo.swapCurrencyStatus,
                isAccountsMode = isAccountsMode,
                isFromCard = true,
                isInsufficientBalance = isInsufficientBalance,
            ),
            currencyIconState = iconConverter.convert(
                value = swapCurrencyStatus.status,
            ),
            tokenSymbol = stringReference(currency.symbol),
            amountEquivalent = getFormattedFiatAmount(
                appCurrency = appCurrency,
                amount = tokenSwapInfo.amountFiat,
            ),
            balance = swapCurrencyStatus.status.getFormattedAmount(),
            isBalanceHidden = isBalanceHidden,
            appCurrency = appCurrency,
            amountField = buildAmountField(
                actions = actions,
                prevAmountField = prevAmountField,
                swapCurrencyStatus = swapCurrencyStatus,
                appCurrency = appCurrency,
            ),
        )
    }

    /**
     * Builds the read-only receive card from [SwapState.Transfer.sendingAmount] — the amount that will
     * actually be received, already reduced by the fee when fee coverage applies. While the reduced amount
     * is not yet known (fee still loading) the amount and fiat fields are null, which makes the read-only
     * card render a shimmer instead of the un-subtracted value.
     */
    private fun createReceiveCard(actions: UiActions, transferState: SwapState.Transfer): SwapCardState {
        val toTokenSwapInfo = transferState.toTokenInfo
        val swapCurrencyStatus = toTokenSwapInfo.swapCurrencyStatus
        val currency = swapCurrencyStatus.currency
        val appCurrency = transferState.appCurrency
        val sendingAmount = transferState.sendingAmount
        // No reduction can happen when the balance is insufficient (fee coverage requires balance >= amount),
        // so there is nothing to wait for — show the amount instead of a shimmer.
        val isLoading = transferState.isSendingAmountLoading && !transferState.isInsufficientBalance
        val fiatRate = swapCurrencyStatus.status.value.fiatRate

        return SwapCardState.SwapCardData(
            type = createSendTransactionCardType(
                actions = actions,
                swapCurrencyStatus = swapCurrencyStatus,
                isAccountsMode = transferState.isAccountsMode,
                isFromCard = false,
                isInsufficientBalance = transferState.isInsufficientBalance,
            ),
            currencyIconState = iconConverter.convert(
                value = swapCurrencyStatus.status,
            ),
            tokenSymbol = stringReference(currency.symbol),
            amountEquivalent = if (isLoading) {
                null
            } else {
                getFormattedFiatAmount(appCurrency = appCurrency, amount = fiatRate?.multiply(sendingAmount))
            },
            balance = swapCurrencyStatus.status.getFormattedAmount(),
            isBalanceHidden = transferState.isBalanceHidden,
            appCurrency = appCurrency,
            amountField = if (isLoading) {
                null
            } else {
                val value = sendingAmount.format {
                    simple(decimals = currency.decimals)
                }
                displayAmountField(
                    actions = actions,
                    value = value,
                    swapCurrencyStatus = swapCurrencyStatus,
                    appCurrency = appCurrency,
                )
            },
        )
    }

    /**
     * Builds the read-only receive card [AmountFieldModel] in transfer mode from a display [value].
     * The read-only UI only reads [AmountFieldModel.value].
     */
    private fun displayAmountField(
        actions: UiActions,
        value: String,
        swapCurrencyStatus: SwapCurrencyStatus,
        appCurrency: AppCurrency,
    ): AmountFieldModel = AmountFieldConverter(
        clickIntents = SwapAmountScreenClickIntents(actions),
        cryptoCurrencyStatus = swapCurrencyStatus.status,
        appCurrency = appCurrency,
    ).convert(value = "").copy(
        value = value,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number,
        ),
        keyboardActions = KeyboardActions(),
        onValuePastedTriggerDismiss = {},
    )

    /**
     * Rebuilds the "from" card [AmountFieldModel] in transfer mode, preserving the previously entered
     * value and the crypto/fiat toggle while refreshing currency-derived fields against the latest status.
     */
    private fun buildAmountField(
        actions: UiActions,
        prevAmountField: AmountFieldModel?,
        swapCurrencyStatus: SwapCurrencyStatus,
        appCurrency: AppCurrency,
    ): AmountFieldModel {
        val fiatRate = swapCurrencyStatus.status.value.fiatRate
        val isFiatValue = prevAmountField?.isFiatValue == true && fiatRate != null
        val cryptoDecimal = prevAmountField?.cryptoAmount?.value.orZero()
        val fiatDecimal = fiatRate?.multiply(cryptoDecimal)
        // The converter is the single source for cryptoAmount / fiatAmount construction (FIAT_DECIMALS = 2).
        // The previously entered value + crypto/fiat toggle display are restored afterwards via copy(...),
        // keeping the resulting AmountFieldModel field-for-field equivalent to the prior hand-rolled builder.
        return AmountFieldConverter(
            clickIntents = SwapAmountScreenClickIntents(actions),
            cryptoCurrencyStatus = swapCurrencyStatus.status,
            appCurrency = appCurrency,
        ).convert(value = cryptoDecimal.toPlainString()).copy(
            value = prevAmountField?.value.orEmpty(),
            isFiatValue = isFiatValue,
            fiatValue = fiatDecimal?.format {
                fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
            }.orEmpty(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number,
            ),
            keyboardActions = KeyboardActions(),
            onValuePastedTriggerDismiss = {},
        )
    }

    private fun createSendTransactionCardType(
        actions: UiActions,
        swapCurrencyStatus: SwapCurrencyStatus,
        isAccountsMode: Boolean,
        isFromCard: Boolean,
        isInsufficientBalance: Boolean,
    ): TransactionCardType {
        val type = if (isFromCard) {
            val accountTitleUM = if (isInsufficientBalance) {
                AccountTitleUM.Text(resourceReference(R.string.swapping_insufficient_funds))
            } else {
                getCardAccountTitle(
                    account = swapCurrencyStatus.account,
                    isAccountsMode = isAccountsMode,
                    isFromCard = true,
                )
            }
            TransactionCardType.Inputtable(
                onCurrencyChange = actions.onCurrencyChange,
                onFocusChanged = actions.onAmountSelected,
                inputError = if (isInsufficientBalance) {
                    TransactionCardType.InputError.InsufficientFunds
                } else {
                    TransactionCardType.InputError.Empty
                },
                accountTitleUM = accountTitleUM,
                isEnabled = true,
            )
        } else {
            TransactionCardType.ReadOnly(
                accountTitleUM = getCardAccountTitle(
                    account = swapCurrencyStatus.account,
                    isAccountsMode = isAccountsMode,
                    isFromCard = false,
                ),
            )
        }
        return type
    }

    private fun getCardAccountTitle(account: Account?, isAccountsMode: Boolean, isFromCard: Boolean): AccountTitleUM {
        val (prefix, placeholder) = if (isFromCard) {
            R.string.swapping_from_account_title to R.string.swapping_from_title_v2
        } else {
            R.string.swapping_to_account_title to R.string.swapping_to_title
        }
        return if (account != null && isAccountsMode) {
            AccountTitleUM.Account(
                prefixText = resourceReference(prefix),
                name = account.accountName.toUM().value,
                icon = account.toIconUM(),
            )
        } else {
            AccountTitleUM.Text(resourceReference(placeholder))
        }
    }

    private fun getFormattedFiatAmount(appCurrency: AppCurrency, amount: BigDecimal?): TextReference {
        return stringReference(
            amount.format {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                )
            },
        )
    }

    private fun CryptoCurrencyStatus.getFormattedAmount(): TextReference {
        return resourceReference(
            R.string.common_balance,
            wrappedList(value.amount.format { crypto(currency.symbol, currency.decimals) }),
        )
    }

    private fun Account.toIconUM(): AccountIconUM {
        return when (this) {
            is Account.CryptoPortfolio -> CryptoPortfolioIconConverter.convert(icon)
            is Account.Payment -> AccountIconUM.Payment
            is Account.Virtual -> AccountIconUM.Virtual
        }
    }

    /**
     * [isTangemPayWithdrawal] - if true - Tangem pay withdrawal done with no fee, skip fee nullability check
     */
    @Suppress("LongParameterList")
    fun updateTransferButtonEnableState(
        dataState: SwapProcessDataState,
        transferState: SwapState.Transfer,
        actions: UiActions,
        uiStateHolder: SwapStateHolder,
        feePaidCryptoCurrencyStatus: CryptoCurrencyStatus?,
        fee: Fee?,
        isTangemPayWithdrawal: Boolean,
        feeSelectorUM: FeeSelectorUM?,
    ): SwapStateHolder {
        val notifications = notificationsFactory.getNotifications(
            transferState = transferState,
            feeCryptoCurrencyStatus = feePaidCryptoCurrencyStatus,
            feeSelectorUM = feeSelectorUM,
            actions = actions,
        )
        return uiStateHolder.copy(
            notifications = notifications,
            // Rebuild the receive card from the refreshed transferState: this path runs after the fee
            // selector resolves, when sendingAmount may have just been reduced by the fee. Only the
            // receive card is rebuilt to avoid clobbering the user's in-progress input on the "from" card.
            receiveCardData = createReceiveCard(actions = actions, transferState = transferState),
            swapButton = uiStateHolder.swapButton.copy(
                isEnabled = getTransferButtonEnabled(notifications, fee, isTangemPayWithdrawal),
            ),
            transferFooter = getSendingFooterText(
                dataState = dataState,
                fee = fee,
                tokenSwapInfo = transferState.fromTokenInfo,
                appCurrency = transferState.appCurrency,
            ),
        )
    }

    private fun getTransferButtonEnabled(
        notifications: ImmutableList<NotificationUM>,
        fee: Fee?,
        isTangemPayWithdrawal: Boolean,
    ): Boolean {
        return (fee != null || isTangemPayWithdrawal) && notifications.none { notification ->
            notification is SwapNotificationUM.Error || notification is NotificationUM.Error ||
                notification is SwapNotificationUM.Warning.ExpressErrorWarning ||
                notification is SwapNotificationUM.Warning.ExpressGeneralError ||
                notification is SwapNotificationUM.Warning.NoAvailableTokensToSwap ||
                notification is SwapNotificationUM.Warning.SwapNotSupported ||
                notification is SwapNotificationUM.Warning.NeedReserveToCreateAccount ||
                notification is SwapNotificationUM.Info.PermissionNeeded
        }
    }

    private fun getSendingFooterText(
        dataState: SwapProcessDataState,
        fee: Fee?,
        tokenSwapInfo: TokenSwapInfo,
        appCurrency: AppCurrency,
    ): TextReference? {
        if (fee == null) return null

        val fiatAmountValue = tokenSwapInfo.amountFiat
        val status = dataState.fromSwapCurrencyStatus?.status ?: return null
        val value = dataState.feePaidCryptoCurrency?.value
        val fiatFeeValue = value?.fiatRate?.multiply(fee.amount.value)
        val isFeeConvertibleToFiat = status.currency.network.hasFiatFeeRate

        val fiatSendingValue = if (isFeeConvertibleToFiat) {
            fiatFeeValue?.let { fiatAmountValue.plus(it) }
        } else {
            fiatAmountValue
        }

        val fiatSending = fiatSendingValue.format {
            fiat(
                fiatCurrencyCode = appCurrency.code,
                fiatCurrencySymbol = appCurrency.symbol,
            )
        }

        val networkId = status.currency.network.id
        // When the fee is convertible to fiat, show the fiat-converted value; otherwise keep the raw
        // crypto fee amount — formatFooterFiatFee renders amount.value as crypto in the non-fiat case.
        val feeAmount = if (isFeeConvertibleToFiat) fee.amount.copy(value = fiatFeeValue) else fee.amount
        val fiatFee = formatFooterFiatFee(
            amount = feeAmount,
            isFeeConvertibleToFiat = isFeeConvertibleToFiat,
            isFeeApproximate = isFeeApproximateUseCase(networkId = networkId, amountType = fee.amount.type),
            appCurrency = appCurrency,
        )

        return if (fee is Fee.Tron) {
            getTronTokenFeeSendingText(
                fee = fee,
                fiatFee = fiatFee,
                fiatSending = stringReference(fiatSending),
            )
        } else {
            resourceReference(
                id = if (isFeeConvertibleToFiat) {
                    com.tangem.features.send.impl.R.string.send_summary_transaction_description
                } else {
                    com.tangem.features.send.impl.R.string.send_summary_transaction_description_no_fiat_fee
                },
                formatArgs = wrappedList(fiatSending, fiatFee),
            )
        }
    }

    fun createTransferInProgressState(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(
            swapButton = uiState.swapButton.copy(
                isEnabled = false,
                mode = Mode.TRANSFER_PROGRESSING,
            ),
        )
    }

    @Suppress("LongParameterList")
    fun createSuccessState(
        uiState: SwapStateHolder,
        dataState: SwapProcessDataState,
        fee: Fee?,
        txUrl: String,
        timestamp: Long,
        onExplorerClick: () -> Unit,
        onShareClick: () -> Unit,
    ): SwapStateHolder {
        val transferState = requireNotNull(dataState.currentTransferState)
        val fromSwapCurrencyStatus = requireNotNull(dataState.fromSwapCurrencyStatus)
        val toSwapCurrencyStatus = requireNotNull(dataState.toSwapCurrencyStatus)
        val amount = dataState.amount?.parseBigDecimalOrNull() ?: BigDecimal.ZERO

        val fromCurrency = fromSwapCurrencyStatus.currency
        val toCurrency = toSwapCurrencyStatus.currency
        val fromAmountText = amount.format { crypto(fromCurrency.symbol, fromCurrency.decimals) }
        val toAmountText = transferState.sendingAmount.format { crypto(toCurrency.symbol, toCurrency.decimals) }
        val fromFiatAmount = getFormattedFiatAmount(
            appCurrency = transferState.appCurrency,
            amount = fromSwapCurrencyStatus.status.value.fiatRate?.multiply(amount),
        )
        val toFiatAmount = getFormattedFiatAmount(
            appCurrency = transferState.appCurrency,
            amount = toSwapCurrencyStatus.status.value.fiatRate?.multiply(transferState.sendingAmount),
        )

        return uiState.copy(
            successState = SwapSuccessStateHolder(
                timestamp = timestamp,
                txUrl = txUrl,
                providerName = TextReference.EMPTY,
                providerType = TextReference.EMPTY,
                shouldShowStatusButton = false,
                isTransferMode = true,
                providerIcon = "",
                rate = TextReference.EMPTY,
                fee = fee?.let { formatFeeForSuccess(transferState = transferState, fee = it) },
                fromTitle = getCardAccountTitle(
                    account = fromSwapCurrencyStatus.account,
                    isAccountsMode = transferState.isAccountsMode,
                    isFromCard = true,
                ),
                toTitle = getCardAccountTitle(
                    account = toSwapCurrencyStatus.account,
                    isAccountsMode = transferState.isAccountsMode,
                    isFromCard = false,
                ),
                fromTokenAmount = stringReference(fromAmountText),
                toTokenAmount = stringReference(toAmountText),
                fromTokenFiatAmount = fromFiatAmount,
                toTokenFiatAmount = toFiatAmount,
                fromTokenIconState = iconConverter.convert(fromSwapCurrencyStatus.status),
                toTokenIconState = iconConverter.convert(toSwapCurrencyStatus.status),
                navigationUM = swapSuccessNavigation(
                    txUrl = txUrl,
                    exploreClick = onExplorerClick,
                    shareClick = onShareClick,
                ),
                onStatusButtonClick = {},
            ),
        )
    }

    fun createTangemPayWithdrawalSuccessState(
        uiState: SwapStateHolder,
        dataState: SwapProcessDataState,
        fee: Fee?,
        onExploreClick: () -> Unit,
        onShareClick: () -> Unit,
    ): SwapStateHolder {
        val fromSwapCurrencyStatus = requireNotNull(dataState.fromSwapCurrencyStatus)
        val toSwapCurrencyStatus = requireNotNull(dataState.toSwapCurrencyStatus)
        val transferState = requireNotNull(dataState.currentTransferState)
        val fromAmount = dataState.amount?.parseBigDecimalOrNull() ?: BigDecimal.ZERO
        val toAmount = transferState.sendingAmount
        val fromFiatAmount = getFormattedFiatAmount(
            appCurrency = transferState.appCurrency,
            amount = fromSwapCurrencyStatus.status.value.fiatRate?.multiply(fromAmount),
        )
        val toFiatAmount = getFormattedFiatAmount(
            appCurrency = transferState.appCurrency,
            amount = fromSwapCurrencyStatus.status.value.fiatRate?.multiply(toAmount),
        )

        return uiState.copy(
            successState = SwapSuccessStateHolder(
                timestamp = System.currentTimeMillis(),
                txUrl = "",
                providerName = stringReference(""),
                providerType = stringReference(""),
                shouldShowStatusButton = false,
                isTransferMode = true,
                providerIcon = "",
                rate = TextReference.EMPTY,
                fee = fee?.let { formatFeeForSuccess(transferState = transferState, fee = it) },
                fromTitle = getCardAccountTitle(
                    account = fromSwapCurrencyStatus.account,
                    isAccountsMode = transferState.isAccountsMode,
                    isFromCard = true,
                ),
                toTitle = getCardAccountTitle(
                    account = toSwapCurrencyStatus.account,
                    isAccountsMode = transferState.isAccountsMode,
                    isFromCard = false,
                ),
                fromTokenAmount = stringReference(fromAmount.toString()),
                toTokenAmount = stringReference(toAmount.toString()),
                fromTokenFiatAmount = fromFiatAmount,
                toTokenFiatAmount = toFiatAmount,
                fromTokenIconState = iconConverter.convert(fromSwapCurrencyStatus.status),
                toTokenIconState = iconConverter.convert(toSwapCurrencyStatus.status),
                navigationUM = swapSuccessNavigation(
                    txUrl = "",
                    exploreClick = onExploreClick,
                    shareClick = onShareClick,
                ),
                onStatusButtonClick = {},
            ),
        )
    }

    private fun formatFeeForSuccess(transferState: SwapState.Transfer, fee: Fee): TextReference {
        val feeAmount = fee.amount
        val totalFeeValue = feeAmount.value ?: BigDecimal.ZERO
        val cryptoFormatted = totalFeeValue.format {
            crypto(symbol = feeAmount.currencySymbol, decimals = feeAmount.decimals)
        }
        val appCurrency = transferState.appCurrency
        val swapCurrencyStatus = transferState.fromTokenInfo.swapCurrencyStatus
        val fiatRate = swapCurrencyStatus.status.value.fiatRate
        val fiatFormatted = fiatRate?.multiply(totalFeeValue).format {
            fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
        }
        return stringReference("$cryptoFormatted ($fiatFormatted)")
    }

    fun updateTransferTitle(uiState: SwapStateHolder): SwapStateHolder {
        return uiState.copy(titleId = R.string.common_transfer)
    }
}