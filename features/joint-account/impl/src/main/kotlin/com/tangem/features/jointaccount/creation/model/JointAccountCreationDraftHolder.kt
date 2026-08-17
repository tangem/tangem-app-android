package com.tangem.features.jointaccount.creation.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Mutable accumulator of the creation flow, shared between the step models.
 *
 * Owned by [JointAccountCreationModel], so it lives exactly as long as the flow and survives configuration
 * changes with it. Steps write their chunk on Continue and read it back to restore their state after being

 */
internal class JointAccountCreationDraftHolder {

    val draft: StateFlow<JointAccountCreationDraft>
        field = MutableStateFlow(JointAccountCreationDraft())

    fun setConfig(config: JointAccountCreationDraft.Config) {
        draft.update { it.copy(config = config) }
    }

    fun setComposition(composition: JointAccountCreationDraft.Composition) {
        draft.update { it.copy(composition = composition) }
    }

    fun setDisplayName(displayName: String) {
        draft.update { it.copy(displayName = displayName) }
    }
}