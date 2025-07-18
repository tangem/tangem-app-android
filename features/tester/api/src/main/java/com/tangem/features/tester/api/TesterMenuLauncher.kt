package com.tangem.features.tester.api

import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Interface for launching the tester menu
 *
[REDACTED_AUTHOR]
 */
interface TesterMenuLauncher {

    /** Observer for detecting shake events and launching the tester menu */
    val launchOnShakeObserver: DefaultLifecycleObserver
}