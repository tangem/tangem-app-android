package com.tangem.domain.redux

import com.tangem.domain.redux.global.DomainGlobalHub
import org.rekotlin.Action
import org.rekotlin.Store

/**
[REDACTED_AUTHOR]
 */
private val RE_STORE_HUBS: List<ReStoreHub<DomainState, *>> = listOf(DomainGlobalHub())

val domainStore = Store(
    state = DomainState(),
    middleware = RE_STORE_HUBS.map { it.getMiddleware() },
    reducer = { action, state -> reduce(action, state) },
)

private fun reduce(action: Action, domainState: DomainState?): DomainState {
    requireNotNull(domainState)

    // we can examine the store state after each change by reducer
    var assembleReducedDomainState: DomainState = domainState
    val reducedStatesByAction = mutableListOf<Pair<Action, DomainState>>()

    RE_STORE_HUBS.forEach {
        val reducedState = it.reduce(action, assembleReducedDomainState)

        assembleReducedDomainState = if (reducedState != assembleReducedDomainState) {
            reducedStatesByAction.add(action to assembleReducedDomainState)
            reducedState
        } else {
            assembleReducedDomainState
        }
    }

    return assembleReducedDomainState
}