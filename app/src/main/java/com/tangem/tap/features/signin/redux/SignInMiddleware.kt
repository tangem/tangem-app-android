package com.tangem.tap.features.signin.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Middleware

/**
[REDACTED_AUTHOR]
 */
object SignInMiddleware {
    val middleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action -> next(action) }
        }
    }
}