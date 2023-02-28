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
                            bundle = action.bundle,
                        )
                    }
                    is NavigationAction.PopBackTo -> {
                        if (navState?.backStack?.lastOrNull() != action.screen) {
                            when (val screen = action.screen) {
                                AppScreen.Home,
                                AppScreen.Welcome,
                                -> {
                                    if (navState?.backStack?.contains(screen) == false) {
                                        // Pop back to activity
                                        navState.activity?.get()?.popBackTo(screen = null, inclusive = true)
                                        store.dispatchOnMain(NavigationAction.NavigateTo(screen))
                                    } else {
                                        navState?.activity?.get()?.popBackTo(screen, action.inclusive)
                                    }
                                }
                                else -> {
                                    navState?.activity?.get()?.popBackTo(screen, action.inclusive)
                                }
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
                    is NavigationAction.OpenBiometricsSettings -> {
                        val settingsAction = when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                                Settings.ACTION_BIOMETRIC_ENROLL
                            }
                            else -> {
                                Settings.ACTION_SECURITY_SETTINGS
                            }
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
