package com.tangem.features.jointaccount.creation.composition.state.transformers

import com.tangem.domain.jointaccount.model.JointAccountConfig.Companion.MAX_MEMBERS_COUNT
import com.tangem.domain.jointaccount.model.JointAccountConfig.Companion.MIN_MEMBERS_COUNT
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM.Companion.MIN_REQUIRED_TO_SIGN

internal class ChangeTotalMembersTransformer(
    private val delta: Int,
) : CompositionStepperTransformer() {

    override fun step(totalMembers: Int, requiredToSign: Int): Pair<Int, Int> {
        val newTotal = (totalMembers + delta).coerceIn(MIN_MEMBERS_COUNT, MAX_MEMBERS_COUNT)
        val newRequired = requiredToSign.coerceIn(MIN_REQUIRED_TO_SIGN, newTotal)

        return newTotal to newRequired
    }
}