package com.tangem.features.staking.impl.presentation.state.events

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.staking.impl.R

@Immutable
internal sealed class StakingAlertUM : AlertUM {

    data class GenericError(
        override val onConfirmClick: () -> Unit,
    ) : StakingAlertUM() {
        override val title: TextReference = resourceReference(R.string.common_error)
        override val message: TextReference = resourceReference(R.string.common_unknown_error)
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_support)
    }

    data class StakingError(
        val code: String,
        override val onConfirmClick: () -> Unit,
    ) : StakingAlertUM() {
        override val title: TextReference = resourceReference(R.string.common_error)
        override val message: TextReference = resourceReference(R.string.generic_error_code, wrappedList(code))
        override val confirmButtonText: TextReference = resourceReference(id = R.string.common_support)
    }
}