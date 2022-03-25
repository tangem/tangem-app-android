package com.tangem.tap.common.redux

import com.tangem.tap.logConfig
import org.rekotlin.Middleware
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
val logMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            if (logConfig.storeAction) Timber.d("Dispatch action: $action")
            nextDispatch(action)
        }
    }
}