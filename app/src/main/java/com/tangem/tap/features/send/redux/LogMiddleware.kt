package com.tangem.tap.features.send.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Middleware
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
val logMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            Timber.d("$action")
            nextDispatch(action)
        }
    }
}