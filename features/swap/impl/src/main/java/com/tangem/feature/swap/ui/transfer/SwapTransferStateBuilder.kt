package com.tangem.feature.swap.ui.transfer

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.model.SwapProcessDataState
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.models.SwapButton.Mode
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.feature.swap.presentation.R
import com.tangem.utils.StringsSigns.DASH_SIGN
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import javax.inject.Inject

internal class SwapTransferStateBuilder @Inject constructor(
    private val notificationsFactory: SwapTransferNotificationsFactory,
) {

    private val iconConverter by lazy(::CryptoCurrencyToIconStateConverter)

    fun createTransferState(
        actions: UiActions,
        transferState: SwapState.Transfer,
        uiStateHolder: SwapStateHolder,
        feePaidCryptoCurrencyStatus: CryptoCurrencyStatus?,
        fee: Fee?,
    ): SwapStateHolder {
        val fromTokenSwapInfo = transferState.fromTokenInfo
        val toTokenSwapInfo = transferState.toTokenInfo
        val isInsufficientBalance = transferState.isInsufficientBalance
        val amountTextFieldValue = (uiStateHolder.sendCardData as? SwapCardState.SwapCardData)?.amountTextFieldValue
        val notifications = notificationsFactory.getNotifications(
            transferState = transferState,
            feeCryptoCurrencyStatus = feePaidCryptoCurrencyStatus,
            fee = fee,
            onReduceByAmount = actions.onReduceByAmount,
            onReduceToAmount = actions.onReduceToAmount,
        )
        return uiStateHolder.copy(
            sendCardData = createSendSwapCardState(
                actions = actions,
                amountTextFieldValue = amountTextFieldValue,
                tokenSwapInfo = fromTokenSwapInfo,
                appCurrency = transferState.appCurrency,
                isAccountsMode = transferState.isAccountsMode,
                isFromCard = true,
                isBalanceHidden = transferState.isBalanceHidden,
                isInsufficientBalance = isInsufficientBalance,
            ),
            receiveCardData = createSendSwapCardState(
                actions = actions,
                amountTextFieldValue = amountTextFieldValue,
                tokenSwapInfo = toTokenSwapInfo,
                appCurrency = transferState.appCurrency,
                isAccountsMode = transferState.isAccountsMode,
                isFromCard = false,
                isBalanceHidden = transferState.isBalanceHidden,
                isInsufficientBalance = isInsufficientBalance,
            ),
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
        amountTextFieldValue: TextFieldValue?,
        tokenSwapInfo: TokenSwapInfo,
        appCurrency: AppCurrency,
        isAccountsMode: Boolean,
        isFromCard: Boolean,
        isBalanceHidden: Boolean,
        isInsufficientBalance: Boolean,
    ): SwapCardState {
        val swapCurrencyStatus = tokenSwapInfo.swapCurrencyStatus

        return SwapCardState.SwapCardData(
            type = createSendTransactionCardType(
                actions = actions,
                swapCurrencyStatus = tokenSwapInfo.swapCurrencyStatus,
                isAccountsMode = isAccountsMode,
                isFromCard = isFromCard,
                isInsufficientBalance = isInsufficientBalance,
            ),
            currencyIconState = iconConverter.convert(
                value = swapCurrencyStatus.status,
            ),
            tokenSymbol = stringReference(swapCurrencyStatus.currency.symbol),
            amountEquivalent = getFormattedFiatAmount(
                appCurrency = appCurrency,
                amount = tokenSwapInfo.amountFiat,
            ),
            amountTextFieldValue = amountTextFieldValue,
            balance = swapCurrencyStatus.status.getFormattedAmount(),
            isBalanceHidden = isBalanceHidden,
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
                onAmountChanged = actions.onAmountChanged,
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

    private fun CryptoCurrencyStatus.getFormattedAmount(): String {
        val amount = this.value.amount ?: return DASH_SIGN
        return amount.format { crypto(symbol = "", decimals = currency.decimals) }
    }

    private fun Account.toIconUM(): AccountIconUM {
        return when (this) {
            is Account.CryptoPortfolio -> CryptoPortfolioIconConverter.convert(icon)
            is Account.Payment -> AccountIconUM.Payment
        }
    }

    fun updateTransferButtonEnableState(
        transferState: SwapState.Transfer,
        actions: UiActions,
        uiStateHolder: SwapStateHolder,
        feePaidCryptoCurrencyStatus: CryptoCurrencyStatus?,
        fee: Fee?,
    ): SwapStateHolder {
        val notifications = notificationsFactory.getNotifications(
            transferState = transferState,
            feeCryptoCurrencyStatus = feePaidCryptoCurrencyStatus,
            fee = fee,
            onReduceByAmount = actions.onReduceByAmount,
            onReduceToAmount = actions.onReduceToAmount,
        )
        return uiStateHolder.copy(
            notifications = notifications,
            swapButton = uiStateHolder.swapButton.copy(
                isEnabled = getTransferButtonEnabled(notifications, fee),
            ),
        )
    }

    private fun getTransferButtonEnabled(notifications: ImmutableList<NotificationUM>, fee: Fee?): Boolean {
        return fee != null && notifications.none { notification ->
            notification is SwapNotificationUM.Error || notification is NotificationUM.Error ||
                notification is SwapNotificationUM.Warning.ExpressErrorWarning ||
                notification is SwapNotificationUM.Warning.ExpressGeneralError ||
                notification is SwapNotificationUM.Warning.NoAvailableTokensToSwap ||
                notification is SwapNotificationUM.Warning.SwapNotSupported ||
                notification is SwapNotificationUM.Warning.NeedReserveToCreateAccount ||
                notification is SwapNotificationUM.Info.PermissionNeeded
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
        appCurrency: AppCurrency,
        isAccountsMode: Boolean,
        txUrl: String,
        timestamp: Long,
        fee: TextReference?,
    ): SwapStateHolder {
        val fromSwapCurrencyStatus = requireNotNull(dataState.fromSwapCurrencyStatus)
        val toSwapCurrencyStatus = requireNotNull(dataState.toSwapCurrencyStatus)
        val amount = dataState.amount?.parseBigDecimalOrNull() ?: BigDecimal.ZERO

        val fromCurrency = fromSwapCurrencyStatus.currency
        val toCurrency = toSwapCurrencyStatus.currency
        val fromAmountText = amount.format { crypto(fromCurrency.symbol, fromCurrency.decimals) }
        val toAmountText = amount.format { crypto(toCurrency.symbol, toCurrency.decimals) }
        val fromFiatAmount = getFormattedFiatAmount(
            appCurrency = appCurrency,
            amount = fromSwapCurrencyStatus.status.value.fiatRate?.multiply(amount),
        )
        val toFiatAmount = getFormattedFiatAmount(
            appCurrency = appCurrency,
            amount = toSwapCurrencyStatus.status.value.fiatRate?.multiply(amount),
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
                fee = fee,
                fromTitle = getCardAccountTitle(
                    account = fromSwapCurrencyStatus.account,
                    isAccountsMode = isAccountsMode,
                    isFromCard = true,
                ),
                toTitle = getCardAccountTitle(
                    account = toSwapCurrencyStatus.account,
                    isAccountsMode = isAccountsMode,
                    isFromCard = false,
                ),
                fromTokenAmount = stringReference(fromAmountText),
                toTokenAmount = stringReference(toAmountText),
                fromTokenFiatAmount = fromFiatAmount,
                toTokenFiatAmount = toFiatAmount,
                fromTokenIconState = iconConverter.convert(fromSwapCurrencyStatus.status),
                toTokenIconState = iconConverter.convert(toSwapCurrencyStatus.status),
                onExploreButtonClick = {},
                onStatusButtonClick = {},
            ),
        )
    }
}