package com.tangem.feature.tester.presentation.navigation

import android.content.Context
import com.tangem.features.tester.api.TesterMenuLauncher

/**
 * Default implementation of [TesterMenuLauncher] that listens for double-press events
 * of the volume down button. When a double press is detected, it opens the tester menu.
 *
 * @param context the application context used to launch the tester menu
 */
internal class DefaultTesterMenuLauncher(private val context: Context) : TesterMenuLauncher {

    override val launchOnKeyEventObserver = VolumeButtonDoublePressObserver(context)
}