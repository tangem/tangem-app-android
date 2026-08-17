package com.tangem.features.jointaccount.creation.composition.state.transformers

import com.tangem.features.jointaccount.creation.model.JointAccountCreationDraft

internal class RestoreCompositionDraftTransformer(
    private val draft: JointAccountCreationDraft.Composition,
) : CompositionStepperTransformer() {

    override fun step(totalMembers: Int, requiredToSign: Int): Pair<Int, Int> {
        return draft.totalMembers to draft.requiredToSign
    }
}