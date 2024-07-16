package com.tangem.tap.common.redux

import com.tangem.common.extensions.VoidCallback
import com.tangem.core.navigation.StateDialog
import com.tangem.tap.common.TestAction
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.domain.model.WalletAddressData
import com.tangem.wallet.R

/**
* [REDACTED_AUTHOR]
 */
sealed class AppDialog : StateDialog {
    data class SimpleOkDialogRes(
        val headerId: Int,
        val messageId: Int,
        val args: List<String> = emptyList(),
        val onOk: VoidCallback? = null,
    ) : AppDialog()

    internal data class AddressInfoDialog(
        val currency: Currency,
        val addressData: WalletAddressData,
    ) : AppDialog()

    data class TestActionsDialog(
        val actionsList: List<TestAction>,
    ) : AppDialog()

    data class RussianCardholdersWarningDialog(val data: Data?) : AppDialog() {
        data class Data(val topUpUrl: String)
    }

    data class RemoveWalletDialog(
        val currencyTitle: String,
        val onOk: () -> Unit,
    ) : AppDialog() {
        val messageRes: Int = R.string.token_details_hide_alert_message
        val titleRes: Int = R.string.token_details_hide_alert_title
        val primaryButtonRes: Int = R.string.token_details_hide_alert_hide
    }

    data class TokensAreLinkedDialog(
        val currencyTitle: String,
        val currencySymbol: String,
        val networkName: String,
    ) : AppDialog() {
        val messageRes: Int = R.string.token_details_unable_hide_alert_message
        val titleRes: Int = R.string.token_details_unable_hide_alert_title
    }
}
