package com.tangem.feature.swap.ui.transfer

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.*
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.utils.formatToUIRepresentation
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal
import javax.inject.Inject

internal class SwapTransferStateBuilder @Inject constructor() {

    private val iconConverter by lazy(::CryptoCurrencyToIconStateConverter)

    fun createTransferState(
        actions: UiActions,
        transferState: SwapState.Transfer,
        uiStateHolder: SwapStateHolder,
    ): SwapStateHolder {
        val fromTokenSwapInfo = transferState.fromTokenInfo
        val toTokenSwapInfo = transferState.toTokenInfo
        return uiStateHolder.copy(
            sendCardData = createSendSwapCardState(
                actions = actions,
                tokenSwapInfo = fromTokenSwapInfo,
                appCurrency = transferState.appCurrency,
                isAccountsMode = transferState.isAccountsMode,
                isFromCard = true,
                isBalanceHidden = transferState.isBalanceHidden,
            ),
            receiveCardData = createSendSwapCardState(
                actions = actions,
                tokenSwapInfo = toTokenSwapInfo,
                appCurrency = transferState.appCurrency,
                isAccountsMode = transferState.isAccountsMode,
                isFromCard = false,
                isBalanceHidden = transferState.isBalanceHidden,
            ),
            swapButton = SwapButton(
                walletInteractionIcon = walletInterationIcon(transferState.userWallet),
                isEnabled = false,
                mode = SwapButton.Mode.TRANSFER,
                onClick = actions.onTransferClick,
            ),
            changeCardsButtonState = ChangeCardsButtonState.ENABLED,
        )
    }

    @Suppress("LongParameterList")
    private fun createSendSwapCardState(
        actions: UiActions,
        tokenSwapInfo: TokenSwapInfo,
        appCurrency: AppCurrency,
        isAccountsMode: Boolean,
        isFromCard: Boolean,
        isBalanceHidden: Boolean,
    ): SwapCardState {
        val swapCurrencyStatus = tokenSwapInfo.swapCurrencyStatus
        val formattedSwapAmount = tokenSwapInfo.tokenAmount.formatToUIRepresentation()

        return SwapCardState.SwapCardData(
            type = createSendTransactionCardType(
                actions = actions,
                swapCurrencyStatus = tokenSwapInfo.swapCurrencyStatus,
                isAccountsMode = isAccountsMode,
                isFromCard = isFromCard,
            ),
            currencyIconState = iconConverter.convert(
                value = swapCurrencyStatus.status,
            ),
            tokenSymbol = stringReference(swapCurrencyStatus.currency.symbol),
            amountEquivalent = getFormattedFiatAmount(
                appCurrency = appCurrency,
                amount = tokenSwapInfo.amountFiat,
            ),
            amountTextFieldValue = TextFieldValue(
                text = formattedSwapAmount,
                selection = TextRange(index = formattedSwapAmount.length),
            ),
            balance = swapCurrencyStatus.status.getFormattedAmount(),
            isBalanceHidden = isBalanceHidden,
        )
    }

    private fun createSendTransactionCardType(
        actions: UiActions,
        swapCurrencyStatus: SwapCurrencyStatus,
        isAccountsMode: Boolean,
        isFromCard: Boolean,
    ): TransactionCardType {
        val type = if (isFromCard) {
            TransactionCardType.Inputtable(
                onAmountChanged = actions.onAmountChanged,
                onFocusChanged = actions.onAmountSelected,
                inputError = TransactionCardType.InputError.Empty,
                accountTitleUM = getCardAccountTitle(
                    account = swapCurrencyStatus.account,
                    isAccountsMode = isAccountsMode,
                    isFromCard = true,
                ),
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
}