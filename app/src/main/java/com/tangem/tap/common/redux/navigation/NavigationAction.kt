package com.tangem.tap.common.redux.navigation

import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.transition.TransitionSet
import org.rekotlin.Action
import java.lang.ref.WeakReference

sealed class NavigationAction : Action {
    data class NavigateTo(
            val screen: AppScreen,
            val addToBackstack: Boolean = true,
            val fragmentShareTransition: FragmentShareTransition? = null
    ) : NavigationAction()

    data class PopBackTo(val screen: AppScreen? = null) : NavigationAction()

    data class OpenUrl(val url: String) : NavigationAction()

    data class ActivityCreated(val activity: WeakReference<FragmentActivity>) : NavigationAction()
    object ActivityDestroyed : NavigationAction()
}

data class FragmentShareTransition(
        val shareElements: List<ShareElement>,
        val enterTransitionSet: TransitionSet,
        val exitTransitionSet: TransitionSet
)

data class ShareElement(val wView: WeakReference<View>, val name: String)