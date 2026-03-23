package com.tangem.tap.common.redux

import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.redux.StateDialog
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
sealed class AppDialog : StateDialog {
    data class SimpleOkDialogRes(
        val headerId: Int,
        val messageId: Int,
        val args: List<String> = emptyList(),
        val onOk: VoidCallback? = null,
    ) : AppDialog()

    data class TokensAreLinkedDialog(
        val currencyTitle: String,
        val currencySymbol: String,
        val networkName: String,
    ) : AppDialog() {
        val messageRes: Int = R.string.token_details_unable_hide_alert_message
        val titleRes: Int = R.string.token_details_unable_hide_alert_title
    }

    data class WalletAlreadyWasUsedDialog(
        val onOk: () -> Unit,
        val onSupportClick: () -> Unit,
        val onCancel: () -> Unit,
    ) : AppDialog()
}