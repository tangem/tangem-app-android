package com.tangem.tap.common.redux

import org.rekotlin.Action

abstract class Request : Action {
    abstract suspend fun execute()
}