package com.tangem.tap.features.disclaimer.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.preferencesStorage
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
        is DisclaimerAction.SetDisclaimerType -> {
            handleUpdateState = action.type.createUpdateState()
        }
        is DisclaimerAction.Show -> {
            handleUpdateState = state.type.createUpdateState()
            store.dispatch(DisclaimerAction.SetOnAcceptCallback(action.onAcceptCallback))
            store.dispatch(NavigationAction.NavigateTo(AppScreen.Disclaimer))
        }
        is DisclaimerAction.AcceptDisclaimer -> {
            action.type.accept()
            handleUpdateState = action.type.createUpdateState()
            store.dispatch(NavigationAction.PopBackTo())
            state.onAcceptCallback?.invoke()
            store.dispatch(DisclaimerAction.SetOnAcceptCallback(null))
        }
    }
}

private fun DisclaimerType.accept() = when (this) {
    DisclaimerType.SaltPay -> preferencesStorage.disclaimerPrefStorage.hasSaltPayTosAccepted = true
    DisclaimerType.Tangem -> preferencesStorage.disclaimerPrefStorage.hasTangemTosAccepted = true
}

fun DisclaimerType.isAccepted(): Boolean = when (this) {
    DisclaimerType.SaltPay -> preferencesStorage.disclaimerPrefStorage.hasSaltPayTosAccepted
    DisclaimerType.Tangem -> preferencesStorage.disclaimerPrefStorage.hasTangemTosAccepted
}

private fun DisclaimerType.createUpdateState(): DisclaimerAction.UpdateState {
    return DisclaimerAction.UpdateState(this, this.isAccepted())
}

private var handleUpdateState: DisclaimerAction.UpdateState = DisclaimerAction.UpdateState(
    type = DisclaimerType.Tangem,
    accepted = false,
)
    set(value) {
        field = value
        store.dispatch(value)
    }
