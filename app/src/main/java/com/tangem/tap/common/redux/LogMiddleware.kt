package com.tangem.tap.common.redux

import com.tangem.domain.common.LogConfig
import org.rekotlin.Middleware
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
val logMiddleware: Middleware<AppState> = { dispatch, appState ->
    { nextDispatch ->
        { action ->
            if (LogConfig.storeAction) {
                Timber.d("Dispatch action: $action")
                // printOnboardingWalletState()
            }
            nextDispatch(action)
        }
    }
}