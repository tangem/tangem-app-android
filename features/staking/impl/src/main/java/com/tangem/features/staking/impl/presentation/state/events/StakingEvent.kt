package com.tangem.features.staking.impl.presentation.state.events

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class StakingEvent {

    data class ShowSnackBar(val text: TextReference) : StakingEvent()

    data class ShowAlert(val alert: AlertUM) : StakingEvent()
}