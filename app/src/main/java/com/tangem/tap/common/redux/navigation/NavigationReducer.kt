package com.tangem.tap.common.redux.navigation

import com.tangem.tap.common.extensions.getPreviousScreen
import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

fun navigationReducer(action: Action, state: AppState): NavigationState {

    val navigationAction = action as? NavigationAction ?: return state.navigationState
    val navState = state.navigationState

    return when (navigationAction) {
        is NavigationAction.NavigateTo -> {
            navState.copy(backStack = navState.backStack + navigationAction.screen)
        }
        is NavigationAction.PopBackTo -> {
            val screen =
                navigationAction.screen ?: navState.activity?.get()?.getPreviousScreen()
            val index = navState.backStack.lastIndexOf(screen) + 1
            state.navigationState.copy(backStack = navState.backStack.subList(0, index))
        }
        is NavigationAction.ActivityCreated -> navState.copy(activity = navigationAction.activity)
        is NavigationAction.ActivityDestroyed -> navState.copy(activity = null)
    }
}