package com.tangem.domain.redux.state

/**
[REDACTED_AUTHOR]
 */
interface StringStateConverter<StateHolder> {
    fun convert(stateHolder: StateHolder): String
}