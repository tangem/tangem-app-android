package com.tangem.domain.redux.state

import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
interface StringStateConverter<StateHolder> {
    fun convert(stateHolder: StateHolder): String
}

interface StringActionConverter<StateHolder> {
    fun convert(action: Action, stateHolder: StateHolder): String?
}