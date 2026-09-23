package com.tangem.features.jointaccount.creation.composition.state.transformers

import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM
import com.tangem.domain.jointaccount.model.JointAccountConfig.Companion.MAX_MEMBERS_COUNT
import com.tangem.domain.jointaccount.model.JointAccountConfig.Companion.MIN_MEMBERS_COUNT
import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM.Companion.MIN_REQUIRED_TO_SIGN
import com.tangem.utils.transformer.Transformer

internal abstract class CompositionStepperTransformer : Transformer<JointAccountCompositionUM> {

    protected abstract fun step(totalMembers: Int, requiredToSign: Int): Pair<Int, Int>

    override fun transform(prevState: JointAccountCompositionUM): JointAccountCompositionUM {
        val (newTotal, newRequired) = step(
            totalMembers = prevState.totalMembers.value,
            requiredToSign = prevState.requiredToSign.value,
        )

        return prevState.copy(
            totalMembers = prevState.totalMembers.copy(
                value = newTotal,
                isDecrementEnabled = newTotal > MIN_MEMBERS_COUNT,
                isIncrementEnabled = newTotal < MAX_MEMBERS_COUNT,
            ),
            requiredToSign = prevState.requiredToSign.copy(
                value = newRequired,
                isDecrementEnabled = newRequired > MIN_REQUIRED_TO_SIGN,
                isIncrementEnabled = newRequired < newTotal,
            ),
        )
    }
}