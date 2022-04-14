package com.tangem.domain.redux.state

import com.tangem.domain.redux.DomainState
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
interface StatePrinter<A, S> {
    fun print(action: Action, domainState: DomainState): String?
    fun getStateObject(domainState: DomainState): S
}