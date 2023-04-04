package com.tangem.tap.proxy.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Middleware

object DaggerGraphMiddleware {
    val daggerGraphMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action -> next(action) }
        }
    }
}