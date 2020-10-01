package com.tangem.tap.common.redux

import org.rekotlin.Middleware
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
val logMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            Timber.d("Dispatch action: $action")
            nextDispatch(action)
        }
    }
}