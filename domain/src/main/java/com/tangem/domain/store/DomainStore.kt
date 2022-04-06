package com.tangem.domain.store

import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenHub
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokensState
import org.rekotlin.Action
import org.rekotlin.Middleware
import org.rekotlin.StateType
import org.rekotlin.Store

/**
[REDACTED_AUTHOR]
 */
private class DomainStore // for simple search

val domainStore = Store(
    state = DomainState(),
    middleware = domainMiddlewares(),
    reducer = { action, state -> domainReduce(action, state) }
)

data class DomainState(
    val addCustomTokensState: AddCustomTokensState = AddCustomTokenHub.initialState
) : StateType

private fun domainMiddlewares(): List<Middleware<DomainState>> {
    return listOf(
        AddCustomTokenHub.middleware
    )
}

private fun domainReduce(action: Action, state: DomainState?): DomainState {
    requireNotNull(state)

    return DomainState(
        addCustomTokensState = AddCustomTokenHub.reduceAction(action, state.addCustomTokensState)
    )
}