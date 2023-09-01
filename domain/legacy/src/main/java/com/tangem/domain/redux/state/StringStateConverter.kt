package com.tangem.domain.redux.state

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
interface StringStateConverter<StateHolder> {
    fun convert(stateHolder: StateHolder): String
}
