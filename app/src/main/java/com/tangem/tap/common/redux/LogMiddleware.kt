package com.tangem.tap.common.redux

import org.rekotlin.Middleware
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 31/08/2020.
 */
val logMiddleware: Middleware<AppState> = { _, _ ->
    { nextDispatch ->
        { action ->
            Timber.i("Dispatch action: $action")
            nextDispatch(action)
        }
    }
}
