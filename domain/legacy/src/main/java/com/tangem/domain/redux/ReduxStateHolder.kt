package com.tangem.domain.redux

import org.rekotlin.Action

interface ReduxStateHolder {

    fun dispatch(action: Action)
}