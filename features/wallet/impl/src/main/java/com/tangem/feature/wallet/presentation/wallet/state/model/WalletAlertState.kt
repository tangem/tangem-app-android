package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.wallet.impl.R

@Immutable
internal sealed interface WalletAlertState {

    @Immutable
    sealed class Basic : WalletAlertState {
        abstract val title: TextReference?
        abstract val message: TextReference
        open val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
        open val isWarningConfirmButton: Boolean = false
        abstract val onConfirmClick: (() -> Unit)?
        open val cancelButtonText: TextReference? = null
        open val onCancelClick: (() -> Unit)? = null
    }

    @Immutable
    sealed class TextInput : WalletAlertState {
        abstract val title: TextReference
        abstract val label: TextReference
        open val text: String = ""
        open val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
        abstract val onConfirmClick: (String) -> Unit
        abstract val errorTextProvider: (String) -> TextReference?
    }

    data class SimpleOkAlert(val message: TextReference, val onOkClick: () -> Unit) : WalletAlertState

    data class DefaultAlert(
        override val title: TextReference?,
        override val message: TextReference,
        override val onConfirmClick: (() -> Unit)?,
    ) : Basic()

    data class RenameWalletAlert(
        override val text: String,
        override val onConfirmClick: (String) -> Unit,
        override val errorTextProvider: (String) -> TextReference?,
    ) : TextInput() {
        override val title: TextReference = resourceReference(id = R.string.user_wallet_list_rename_popup_title)
        override val label: TextReference = resourceReference(id = R.string.user_wallet_list_rename_popup_placeholder)
    }

    data class RemoveWalletAlert(override val onConfirmClick: (() -> Unit)?) : Basic() {
        override val title: TextReference? = null
        override val message: TextReference = resourceReference(id = R.string.user_wallet_list_delete_prompt)
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_delete)
        override val isWarningConfirmButton: Boolean = true
    }

    data class VisaLimitsInfo(
        val totalLimit: String,
        val otherLimit: String,
    ) : Basic() {
        override val title: TextReference? = null
        override val message: TextReference = stringReference(
            value = "Limits are needed to control costs, improve security, manage risk. " +
                "You can spend $totalLimit during the week for card payments in shops and " +
                "$otherLimit for other transactions, e. g. subscriptions or debts.",
        )
        override val onConfirmClick: (() -> Unit)? = null
    }

    data object WrongCardIsScanned : Basic() {
        override val title: TextReference = resourceReference(R.string.common_warning)
        override val message: TextReference = resourceReference(R.string.error_wrong_wallet_tapped)
        override val onConfirmClick: (() -> Unit)? = null
    }

    data object RescanWallets : Basic() {
        override val title: TextReference = resourceReference(R.string.common_attention)
        override val message: TextReference = resourceReference(R.string.key_invalidated_warning_description)
        override val onConfirmClick: (() -> Unit)? = null
    }

    data object VisaBalancesInfo : Basic() {
        override val title: TextReference? = null
        override val message: TextReference = stringReference(
            value = "Available balance is actual funds available, considering pending transactions, " +
                "blocked amounts, and debit balance to prevent overdrafts.",
        )
        override val onConfirmClick: (() -> Unit)? = null
    }

    data object ProvidersStillLoading : Basic() {
        override val title: TextReference = resourceReference(R.string.action_buttons_service_loading_alert_title)
        override val message: TextReference = resourceReference(R.string.action_buttons_service_loading_alert_message)
        override val onConfirmClick: (() -> Unit)? = null
    }

    data object UnavailableOperation : Basic() {
        override val title: TextReference = resourceReference(R.string.action_buttons_something_wrong_alert_title)
        override val message: TextReference = resourceReference(R.string.action_buttons_something_wrong_alert_message)
        override val onConfirmClick: (() -> Unit)? = null
    }

    data object InsufficientTokensCountForSwapping : Basic() {
        override val title: TextReference =
            resourceReference(id = R.string.action_buttons_swap_no_tokens_added_alert_title)

        override val message: TextReference =
            resourceReference(id = R.string.action_buttons_swap_no_tokens_added_alert_message)

        override val onConfirmClick: (() -> Unit)? = null
    }

    data class ConfirmExpressStatusHide(
        override val onConfirmClick: (() -> Unit),
        override val onCancelClick: (() -> Unit),
    ) : Basic() {
        override val title: TextReference = resourceReference(R.string.express_status_hide_dialog_title)
        override val message: TextReference = resourceReference(R.string.express_status_hide_dialog_text)
        override val confirmButtonText: TextReference = resourceReference(R.string.common_hide)
        override val cancelButtonText: TextReference = resourceReference(R.string.common_cancel)
    }
}