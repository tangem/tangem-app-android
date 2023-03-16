package com.tangem.tap.features.signin.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Middleware

/**
 * @author Andrew Khokhlov on 21/03/2023
 */
object SignInMiddleware {
    val middleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action -> next(action) }
        }
    }
}
