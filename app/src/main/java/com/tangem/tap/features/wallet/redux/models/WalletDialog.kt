package com.tangem.tap.features.wallet.redux.models

import com.tangem.blockchain.common.Amount
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.redux.StateDialog
import com.tangem.wallet.R

sealed interface WalletDialog : StateDialog {
    data class SelectAmountToSendDialog(val amounts: List<Amount>?) : WalletDialog
    object SignedHashesMultiWalletDialog : WalletDialog
    data class ChooseTradeActionDialog(
        val buyAllowed: Boolean,
        val sellAllowed: Boolean,
        val swapAllowed: Boolean,
    ) : WalletDialog

    data class CurrencySelectionDialog(
        val currenciesList: List<FiatCurrency>,
        val currentAppCurrency: FiatCurrency,
    ) : WalletDialog

    data class RemoveWalletDialog(
        val currencyTitle: String,
        val onOk: () -> Unit,
    ) : WalletDialog {
        val messageRes: Int = R.string.token_details_hide_alert_message
        val titleRes: Int = R.string.token_details_hide_alert_title
        val primaryButtonRes: Int = R.string.token_details_hide_alert_hide
    }

    data class TokensAreLinkedDialog(
        val currencyTitle: String,
        val currencySymbol: String,
    ) : WalletDialog {
        val messageRes: Int = R.string.token_details_unable_hide_alert_message
        val titleRes: Int = R.string.token_details_unable_hide_alert_title
    }

    data class RussianCardholdersWarningDialog(val data: Data?) : WalletDialog {
        data class Data(val topUpUrl: String)
    }
}
