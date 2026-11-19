package com.tangem.features.jointaccount.creation.composition.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.jointaccount.common.ui.jointAccountAccessLossNote

@Immutable
internal data class JointAccountCompositionUM(
    val totalMembers: StepperUM,
    val requiredToSign: StepperUM,
    val maxMembers: Int,
    val onContinueClick: () -> Unit,
    val onBackClick: () -> Unit,
) {

    val accessLossNote: TextReference
        get() = jointAccountAccessLossNote(threshold = requiredToSign.value, total = totalMembers.value)

    data class StepperUM(
        val value: Int,
        val isDecrementEnabled: Boolean,
        val isIncrementEnabled: Boolean,
        val onDecrement: () -> Unit,
        val onIncrement: () -> Unit,
    )

    companion object {
        const val MIN_REQUIRED_TO_SIGN = 1
    }
}