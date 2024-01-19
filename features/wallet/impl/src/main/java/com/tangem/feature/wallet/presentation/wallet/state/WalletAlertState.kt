package com.tangem.feature.wallet.presentation.wallet.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.wallet.impl.R

@Immutable
internal sealed class WalletAlertState {

    abstract val title: TextReference?
    abstract val message: TextReference
    open val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    open val isWarningConfirmButton: Boolean = false
    abstract val onConfirmClick: (() -> Unit)?

    data class DefaultAlert(
        override val title: TextReference,
        override val message: TextReference,
        override val onConfirmClick: (() -> Unit)?,
    ) : WalletAlertState()

    data class RemoveWalletAlert(override val onConfirmClick: (() -> Unit)?) : WalletAlertState() {
        override val title: TextReference? = null
        override val message: TextReference = resourceReference(id = R.string.user_wallet_list_delete_prompt)
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_delete)
        override val isWarningConfirmButton: Boolean = true
    }

    object WrongCardIsScanned : WalletAlertState() {
        override val title: TextReference = resourceReference(R.string.common_warning)
        override val message: TextReference = resourceReference(R.string.error_wrong_wallet_tapped)
        override val onConfirmClick: (() -> Unit)? = null
    }

    object RescanWallets : WalletAlertState() {
        override val title: TextReference = resourceReference(R.string.common_attention)
        override val message: TextReference = resourceReference(R.string.key_invalidated_warning_description)
        override val onConfirmClick: (() -> Unit)? = null
    }

    object VisaBalancesInfo : WalletAlertState() {
        override val title: TextReference? = null
        override val message: TextReference = stringReference(
            value = "Available balance is actual funds available, considering pending transactions, " +
                "blocked amounts, and debit balance to prevent overdrafts.",
        )
        override val onConfirmClick: (() -> Unit)? = null
    }

    object VisaLimitsInfo : WalletAlertState() {
        override val title: TextReference? = null
        override val message: TextReference = stringReference(
            value = "Limits are needed to control costs, improve security, manage risk. " +
                "You can spend 1 000 USDT during the week for card payments in shops and " +
                "100 USDT for other transactions, e. g. subscriptions or debts.",
        )
        override val onConfirmClick: (() -> Unit)? = null
    }
}