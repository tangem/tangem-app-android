package com.tangem.tap.common.redux.navigation

import androidx.fragment.app.FragmentActivity
import org.rekotlin.Action
import java.lang.ref.WeakReference

sealed class NavigationAction : Action {
    data class NavigateTo(
        val screen: AppScreen,
        val fragmentShareTransition: FragmentShareTransition? = null,
        val addToBackstack: Boolean = true
    ) : NavigationAction()

    data class PopBackTo(val screen: AppScreen? = null) : NavigationAction()

    data class OpenUrl(val url: String) : NavigationAction()

    data class Share(val data: String) : NavigationAction()

    data class ActivityCreated(val activity: WeakReference<FragmentActivity>) : NavigationAction()
    object ActivityDestroyed : NavigationAction()
}