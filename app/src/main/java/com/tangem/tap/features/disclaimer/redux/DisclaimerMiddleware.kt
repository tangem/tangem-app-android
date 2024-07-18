package com.tangem.tap.features.disclaimer.redux

import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.mainScope
import com.tangem.tap.store
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

class DisclaimerMiddleware {
    val disclaimerMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                state()?.let { handleDisclaimerMiddleware(action, it) }
                next(action)
            }
        }
    }
}

private fun handleDisclaimerMiddleware(action: Action, appState: AppState) {
    val state = appState.disclaimerState

    when (action) {
        is DisclaimerAction.Show -> {
            store.dispatchNavigationAction {
                push(AppRoute.Disclaimer(isTosAccepted = action.from == DisclaimerSource.Details))
            }
        }
        is DisclaimerAction.AcceptDisclaimer -> {
            mainScope.launch {
                state.disclaimer.accept()
                store.dispatchNavigationAction(AppRouter::pop)
                state.callback?.onAccept?.invoke()
            }
        }
        is DisclaimerAction.OnBackPressed -> {
            store.dispatchNavigationAction(AppRouter::pop)
            state.callback?.onDismiss?.invoke()
        }
    }
}