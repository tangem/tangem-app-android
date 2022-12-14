package com.tangem.tap.common.redux.navigation

import com.tangem.tap.common.extensions.getPreviousScreen
import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class NavigationReducer {
    companion object {
        fun reduce(action: Action, state: AppState): NavigationState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): NavigationState {

    val navigationAction = action as? NavigationAction ?: return state.navigationState
    val navState = state.navigationState

    return when (navigationAction) {
        is NavigationAction.NavigateTo -> {
            navState.copy(backStack = navState.backStack + navigationAction.screen)
        }
        is NavigationAction.PopBackTo -> {
            if (navState.backStack.lastOrNull() == navigationAction.screen) return navState

            val screen = navigationAction.screen ?: navState.activity?.get()?.getPreviousScreen()
            val index = navState.backStack.lastIndexOf(screen) + 1
            state.navigationState.copy(backStack = navState.backStack.subList(0, index))
        }
        is NavigationAction.ActivityCreated -> navState.copy(activity = navigationAction.activity)
        is NavigationAction.ActivityDestroyed -> {
            when {
                // Destroy the activity if it invoked for the same activity. Prevents overwriting to null if there is a
                // new scan from the background AND-2366
                navState.activity?.get() == navigationAction.activity.get() -> navState.copy(activity = null)
                else -> navState
            }
        }
        else -> navState
    }
}
