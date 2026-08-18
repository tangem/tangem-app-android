package com.tangem.features.jointaccount.creation.composition.state.transformers

import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM.Companion.MIN_REQUIRED_TO_SIGN

internal class ChangeRequiredToSignTransformer(
    private val delta: Int,
) : CompositionStepperTransformer() {

    override fun step(totalMembers: Int, requiredToSign: Int): Pair<Int, Int> {
        val newRequired = (requiredToSign + delta).coerceIn(MIN_REQUIRED_TO_SIGN, totalMembers)

        return totalMembers to newRequired
    }
}