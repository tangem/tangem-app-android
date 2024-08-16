package com.tangem.tap.common.redux

import org.rekotlin.Middleware
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
val logMiddleware: Middleware<AppState> = { _, _ ->
    { nextDispatch ->
        { action ->
            Timber.i("Dispatch action: $action")
            nextDispatch(action)
        }
    }
}
