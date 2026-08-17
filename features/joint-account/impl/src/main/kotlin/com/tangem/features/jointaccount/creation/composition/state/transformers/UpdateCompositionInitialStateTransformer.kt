package com.tangem.features.jointaccount.creation.composition.state.transformers

import com.tangem.features.jointaccount.creation.composition.ui.state.JointAccountCompositionUM
import com.tangem.utils.transformer.Transformer

internal class UpdateCompositionInitialStateTransformer(
    private val onTotalMembersDecrement: () -> Unit,
    private val onTotalMembersIncrement: () -> Unit,
    private val onRequiredToSignDecrement: () -> Unit,
    private val onRequiredToSignIncrement: () -> Unit,
    private val onContinueClick: () -> Unit,
    private val onBackClick: () -> Unit,
) : Transformer<JointAccountCompositionUM> {

    override fun transform(prevState: JointAccountCompositionUM): JointAccountCompositionUM = prevState.copy(
        totalMembers = prevState.totalMembers.copy(
            onDecrement = onTotalMembersDecrement,
            onIncrement = onTotalMembersIncrement,
        ),
        requiredToSign = prevState.requiredToSign.copy(
            onDecrement = onRequiredToSignDecrement,
            onIncrement = onRequiredToSignIncrement,
        ),
        onContinueClick = onContinueClick,
        onBackClick = onBackClick,
    )
}