package com.tangem.domain.tokens.actions

import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState.ActionState

/**
 * Factory for creating a set of token action states for missed derivations
 *
[REDACTED_AUTHOR]
 */
internal object MissedDerivationsActionsFactory {

    /** Creates a set of token actions */
    fun create(): Set<ActionState> {
        val action = ActionState.HideToken(unavailabilityReason = ScenarioUnavailabilityReason.None)

        return setOf(action)
    }
}