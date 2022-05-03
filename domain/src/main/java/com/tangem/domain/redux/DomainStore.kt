package com.tangem.domain.redux

import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenHub
import com.tangem.domain.redux.global.DomainGlobalHub
import com.tangem.domain.redux.state.observeReducedStates
import org.rekotlin.Store

/**
[REDACTED_AUTHOR]
 */
private class DomainStore // for simple search

private val RE_STORE_HUBS: List<ReStoreHub<DomainState, *>> = listOf(
    DomainGlobalHub(),
    AddCustomTokenHub(),
)

val domainStore = Store(
    state = DomainState(),
    middleware = RE_STORE_HUBS.map { it.getMiddleware() },
    reducer = { action, state ->
        requireNotNull(state)

        // we can examine the store state after each change by reducer
        val reducedSates = RE_STORE_HUBS.mapNotNull {
            val reducedState = it.reduce(action, state)
            if (reducedState == state) null else Pair(action, reducedState)
        }
        observeReducedStates(reducedSates)

        if (reducedSates.isEmpty()) state else reducedSates.last().second
    }
)
