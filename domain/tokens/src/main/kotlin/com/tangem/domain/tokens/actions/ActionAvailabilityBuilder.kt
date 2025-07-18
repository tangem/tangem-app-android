package com.tangem.domain.tokens.actions

import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState

/**
 * Builder for creating a set of [TokenActionsState.ActionState] based on their availability
 *
[REDACTED_AUTHOR]
 */
internal class ActionAvailabilityBuilder {

    private val activeList = mutableSetOf<TokenActionsState.ActionState>()
    private val disabledList = mutableSetOf<TokenActionsState.ActionState>()

    /** Marks the current [TokenActionsState.ActionState] as active */
    fun TokenActionsState.ActionState.active() {
        activeList.add(this)
    }

    /** Marks the current [TokenActionsState.ActionState] as disabled */
    fun TokenActionsState.ActionState.disabled() {
        disabledList.add(this)
    }

    /** Marks a list of [TokenActionsState.ActionState] as disabled */
    fun List<TokenActionsState.ActionState>.disabled() {
        disabledList.addAll(this)
    }

    /**
     * Adds the current [TokenActionsState.ActionState] to the appropriate list based on its [ScenarioUnavailabilityReason].
     *
     * If the [ScenarioUnavailabilityReason] is [ScenarioUnavailabilityReason.None], the action is added to the active list.
     * Otherwise, it is added to the disabled list.
     */
    fun TokenActionsState.ActionState.addByReason() {
        if (unavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(this)
        } else {
            disabledList.add(this)
        }
    }

    fun build(): Set<TokenActionsState.ActionState> {
        return activeList + disabledList
    }
}

/**
 * This function initializes an [ActionAvailabilityBuilder], applies the given
 * [block] to it, and returns the resulting set of [TokenActionsState.ActionState]
 */
internal suspend fun actionAvailabilityBuilder(
    block: suspend ActionAvailabilityBuilder.() -> Unit,
): Set<TokenActionsState.ActionState> {
    val builder = ActionAvailabilityBuilder()

    builder.block()

    return builder.build()
}