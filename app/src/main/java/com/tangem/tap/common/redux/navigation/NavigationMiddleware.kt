package com.tangem.tap.common.redux.navigation

import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.os.Build
import android.provider.Settings
import com.tangem.tap.activityResultCaller
import com.tangem.tap.common.CustomTabsManager
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.openFragment
import com.tangem.tap.common.extensions.popBackTo
import com.tangem.tap.common.extensions.shareText
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.store
import org.rekotlin.Action
import org.rekotlin.Middleware
import timber.log.Timber

val navigationMiddleware: Middleware<AppState> = { _, state ->
    { next ->
        { action ->
            handleNavigation(action, state)
            next(action)
        }
    }
}

private fun handleNavigation(navAction: Action, state: () -> AppState?) {
    val action = navAction as? NavigationAction ?: return
    val navState = state()?.navigationState ?: return

    when (action) {
        is NavigationAction.NavigateTo -> {
            navState.activity?.get()?.openFragment(
                screen = action.screen,
                addToBackstack = action.addToBackstack,
                fgShareTransition = action.fragmentShareTransition,
                bundle = action.bundle,
            )
        }
        is NavigationAction.PopBackTo -> {
            val lastFromBackStack = navState.backStack.lastOrNull()
            if (lastFromBackStack == action.screen) return
            if (action.popOnlyFrom != null && action.popOnlyFrom != lastFromBackStack) {
                Timber.w("PopBackTo action was blocked")
                return
            }

            val activity = navState.activity?.get()
            when (val screen = action.screen) {
                AppScreen.Home, AppScreen.Welcome -> {
                    if (!navState.backStack.contains(screen)) {
                        // Pop back to activity
                        activity?.popBackTo(screen = null, inclusive = true)
                        store.dispatchOnMain(NavigationAction.NavigateTo(screen))
                    } else {
                        activity?.popBackTo(screen, action.inclusive)
                    }
                }
                else -> {
                    activity?.popBackTo(screen, action.inclusive)
                }
            }
        }
        is NavigationAction.OpenUrl -> {
            navState.activity?.get()?.let {
                CustomTabsManager().openUrl(action.url, it)
            }
        }
        is NavigationAction.OpenDocument -> {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = action.url
            navState.activity?.get()?.startActivity(intent)
        }
        is NavigationAction.OpenBiometricsSettings -> {
            val settingsAction = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Settings.ACTION_BIOMETRIC_ENROLL
                else -> Settings.ACTION_SECURITY_SETTINGS
            }
            val intent = Intent(settingsAction).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BiometricManager.Authenticators.BIOMETRIC_STRONG,
                    )
                }
            }
            activityResultCaller.activityResultLauncher?.launch(intent)
        }
        is NavigationAction.Share -> {
            navState.activity?.get()?.shareText(action.data)
        }
        else -> Unit
    }
}
