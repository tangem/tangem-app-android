package com.tangem.tap.common.redux.navigation

import android.content.Intent
import com.tangem.tap.common.CustomTabsManager
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.openFragment
import com.tangem.tap.common.extensions.popBackTo
import com.tangem.tap.common.extensions.shareText
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.store
import org.rekotlin.Middleware

val navigationMiddleware: Middleware<AppState> = { _, state ->
    { next ->
        { action ->
            if (action is NavigationAction) {
                val navState = state()?.navigationState
                when (action) {
                    is NavigationAction.NavigateTo -> {
                        navState?.activity?.get()?.openFragment(
                            screen = action.screen,
                            addToBackstack = action.addToBackstack,
                            fgShareTransition = action.fragmentShareTransition,
                        )
                    }
                    is NavigationAction.PopBackTo -> {
                        when (val screen = action.screen) {
                            AppScreen.Home,
                            AppScreen.Welcome,
                            -> {
                                navState?.activity?.get()?.popBackTo(screen = null, inclusive = true)
                                if (navState?.backStack?.contains(screen) != true) {
                                    store.dispatchOnMain(NavigationAction.NavigateTo(screen))
                                }
                            }
                            else -> {
                                navState?.activity?.get()?.popBackTo(screen = action.screen)
                            }
                        }
                    }
                    is NavigationAction.OpenUrl -> {
                        navState?.activity?.get()?.let {
                            CustomTabsManager().openUrl(action.url, it)
                        }
                    }
                    is NavigationAction.OpenDocument -> {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = action.url
                        navState?.activity?.get()?.startActivity(intent)
                    }
                    is NavigationAction.Share -> {
                        navState?.activity?.get()?.shareText(action.data)
                    }
                    is NavigationAction.ActivityCreated,
                    is NavigationAction.ActivityDestroyed,
                    -> Unit
                }
            }
            next(action)
        }
    }
}