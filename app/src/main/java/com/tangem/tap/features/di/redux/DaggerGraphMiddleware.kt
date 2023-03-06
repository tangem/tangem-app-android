package com.tangem.tap.features.di.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Middleware

object DaggerGraphMiddleware {
    val daggerGraphMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action -> next(action) }
        }
    }
}