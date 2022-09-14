package com.tangem.tap.common.redux

import com.tangem.tap.store
import org.rekotlin.Middleware
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
val logMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            if (store.state.domainState.globalState.logConfig.storeAction) {
                Timber.d("Dispatch action: $action")
            }
            nextDispatch(action)
        }
    }
}