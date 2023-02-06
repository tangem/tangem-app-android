package com.tangem.tap.features.disclaimer.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.store
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
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
        }
        is DisclaimerAction.AcceptDisclaimer -> {
            state.disclaimer.accept()
            store.dispatch(NavigationAction.PopBackTo())
            state.callback?.onAccept?.invoke()
        }
        is DisclaimerAction.OnBackPressed -> {
            store.dispatch(NavigationAction.PopBackTo())
            state.callback?.onDismiss?.invoke()
        }
    }
}
