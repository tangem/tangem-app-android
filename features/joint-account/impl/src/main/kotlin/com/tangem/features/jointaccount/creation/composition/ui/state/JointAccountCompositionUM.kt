package com.tangem.features.jointaccount.creation.composition.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class JointAccountCompositionUM(
    val totalMembers: StepperUM,
    val requiredToSign: StepperUM,
    val maxMembers: Int,
    val onContinueClick: () -> Unit,
    val onBackClick: () -> Unit,
) {

    data class StepperUM(
        val value: Int,
        val isDecrementEnabled: Boolean,
        val isIncrementEnabled: Boolean,
        val onDecrement: () -> Unit,
        val onIncrement: () -> Unit,
    )

    companion object {
        const val MIN_MEMBERS = 2
        const val MAX_MEMBERS = 5
        const val MIN_REQUIRED_TO_SIGN = 1
    }
}