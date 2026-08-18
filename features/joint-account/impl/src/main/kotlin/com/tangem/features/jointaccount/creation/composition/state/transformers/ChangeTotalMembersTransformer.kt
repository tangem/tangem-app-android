package com.tangem.features.jointaccount.creation.composition.state.transformers

import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM.Companion.MAX_MEMBERS
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM.Companion.MIN_MEMBERS
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM.Companion.MIN_REQUIRED_TO_SIGN

internal class ChangeTotalMembersTransformer(
    private val delta: Int,
) : CompositionStepperTransformer() {

    override fun step(totalMembers: Int, requiredToSign: Int): Pair<Int, Int> {
        val newTotal = (totalMembers + delta).coerceIn(MIN_MEMBERS, MAX_MEMBERS)
        val newRequired = requiredToSign.coerceIn(MIN_REQUIRED_TO_SIGN, newTotal)

        return newTotal to newRequired
    }
}