package com.tangem.tap.common.redux.navigation

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import org.rekotlin.Action
import java.lang.ref.WeakReference

sealed class NavigationAction : Action {
    data class NavigateTo(
        val screen: AppScreen,
        val fragmentShareTransition: FragmentShareTransition? = null,
        val addToBackstack: Boolean = true,
    ) : NavigationAction()

    data class PopBackTo(
        val screen: AppScreen? = null,
        val inclusive: Boolean = false,
    ) : NavigationAction()

    data class OpenUrl(val url: String) : NavigationAction()

    data class OpenDocument(val url: Uri) : NavigationAction()

    object OpenBiometricsSettings : NavigationAction()

    data class Share(val data: String) : NavigationAction()

    data class ActivityCreated(val activity: WeakReference<AppCompatActivity>) : NavigationAction()
    data class ActivityDestroyed(val activity: WeakReference<AppCompatActivity>) : NavigationAction()
}
