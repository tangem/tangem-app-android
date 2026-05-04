package com.tangem.tap.common.redux

import com.tangem.utils.logging.TangemLogger
import org.rekotlin.Middleware

/**
[REDACTED_AUTHOR]
 */
val logMiddleware: Middleware<AppState> = { _, _ ->
    { nextDispatch ->
        { action ->
            TangemLogger.i("Dispatch action: ${action::class.java.simpleName}")
            nextDispatch(action)
        }
    }
}