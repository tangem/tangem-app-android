package com.tangem.feature.wallet.presentation.wallet.state2.transformers.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.utils.converter.Converter

internal class VisaWalletCardStateConverter(
    private val status: CryptoCurrencyStatus,
    private val selectedWallet: UserWallet,
    private val appCurrency: AppCurrency,
) : Converter<WalletCardState, WalletCardState> {

    override fun convert(value: WalletCardState): WalletCardState {
        return when (status.value) {
            is CryptoCurrencyStatus.Loading -> value.toLoadingState()
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.UnreachableWithoutAddresses,
            -> value.toErrorState()
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.NoAmount,
            -> value.toContentState(status)
        }
    }

    private fun WalletCardState.toLoadingState(): WalletCardState {
        return WalletCardState.Loading(
            id = id,
            title = title,
            imageResId = imageResId,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
        )
    }

    private fun WalletCardState.toErrorState(): WalletCardState {
        return WalletCardState.Error(
            id = id,
            title = title,
            imageResId = imageResId,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
        )
    }

    private fun WalletCardState.toContentState(status: CryptoCurrencyStatus): WalletCardState {
        return WalletCardState.Content(
            id = id,
            title = title,
            additionalInfo = createAdditionalInfo(status),
            imageResId = imageResId,
            onRenameClick = onRenameClick,
            onDeleteClick = onDeleteClick,
            balance = formatAmount(status),
            cardCount = selectedWallet.getCardsCount(),
        )
    }

    private fun createAdditionalInfo(status: CryptoCurrencyStatus): WalletAdditionalInfo {
        val fiatAmount = BigDecimalFormatter.formatFiatAmount(
            status.value.fiatAmount,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
        val infoContent = stringReference(
            value = buildString {
                append(fiatAmount)
                append(" • ")
                append(status.currency.network.name)
            },
        )

        return WalletAdditionalInfo(hideable = true, infoContent)
    }

    private fun formatAmount(status: CryptoCurrencyStatus): String {
        val amount = status.value.amount ?: return BigDecimalFormatter.EMPTY_BALANCE_SIGN

        return BigDecimalFormatter.formatCryptoAmount(amount, status.currency)
    }
}