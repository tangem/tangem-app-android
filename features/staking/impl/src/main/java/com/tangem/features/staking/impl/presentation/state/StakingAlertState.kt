package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.staking.impl.R

@Immutable
internal sealed class StakingAlertState {

    abstract val title: TextReference?
    abstract val message: TextReference
    open val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    open val onConfirmClick: (() -> Unit)? = null

    data class GenericError(
        override val title: TextReference? = TODO(),
        override val onConfirmClick: () -> Unit,
    ) : StakingAlertState() {
        override val message: TextReference = resourceReference(R.string.common_unknown_error)
        override val confirmButtonText: TextReference =
            resourceReference(id = R.string.common_support)
    }
}