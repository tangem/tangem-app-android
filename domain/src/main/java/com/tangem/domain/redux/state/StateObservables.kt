package com.tangem.domain.redux.state

import com.tangem.domain.features.addCustomToken.AddCustomTokenActionStateConverter
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.redux.DomainState
import org.rekotlin.Action
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 * Use it only in debug mode!
 */
internal fun observeReducedStates(reducedSates: List<Pair<Action, DomainState>>) {
    // we can add any logic to watch for changes of actions, states, etc.
    val isDebugMode = true
    if (!isDebugMode) return

    logStates(reducedSates)
}

private fun logStates(reducedSates: List<Pair<Action, DomainState>>) {
    reducedSates.forEach {
        val converter = stateConverters.firstNotNullOfOrNull { entry ->
            if (entry.key.isAssignableFrom(it.first::class.java)) entry.value else null
        } ?: return@forEach

        val messageToPrint = converter.convert(it.first, it.second) ?: return@forEach

        Timber.d(messageToPrint)
    }
}

// TODO: refactoring: mutate to factory
private val stateConverters = mutableMapOf(
    AddCustomTokenAction::class.java to AddCustomTokenActionStateConverter()
)