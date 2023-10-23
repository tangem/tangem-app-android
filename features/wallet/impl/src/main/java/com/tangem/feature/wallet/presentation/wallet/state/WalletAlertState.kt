package com.tangem.feature.wallet.presentation.wallet.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
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
}