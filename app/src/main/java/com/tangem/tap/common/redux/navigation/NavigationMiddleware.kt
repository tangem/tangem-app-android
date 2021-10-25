package com.tangem.tap.common.redux.navigation

import com.tangem.tap.common.CustomTabsManager
import com.tangem.tap.common.extensions.openFragment
import com.tangem.tap.common.extensions.popBackTo
import com.tangem.tap.common.redux.AppState
import org.rekotlin.Middleware

val navigationMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            if (action is NavigationAction) {
                val navState = state()?.navigationState
                when (action) {
                    is NavigationAction.NavigateTo -> {
                        navState?.activity?.get()?.openFragment(
                                action.screen,
                                action.addToBackstack,
                                action.fragmentShareTransition
                        )
                    }
                    is NavigationAction.PopBackTo -> {
                        if (action.screen == AppScreen.Home) {
                            navState?.activity?.get()?.popBackTo(null, true)
                        } else {
                            navState?.activity?.get()?.popBackTo(action.screen)
                        }
                    }
                    is NavigationAction.OpenUrl -> {
                        navState?.activity?.get()?.let {
                            CustomTabsManager().openUrl(action.url, it)
                        }
                    }
                }
            }
            next(action)
        }
    }
}