package com.tangem.tap.features.disclaimer.redux

import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
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
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
        }
        is DisclaimerAction.AcceptDisclaimer -> {
            mainScope.launch {
                state.disclaimer.accept()
                store.dispatch(NavigationAction.PopBackTo())
                state.callback?.onAccept?.invoke()
            }
        }
        is DisclaimerAction.OnBackPressed -> {
            store.dispatch(NavigationAction.PopBackTo())
            state.callback?.onDismiss?.invoke()
        }
    }
}
