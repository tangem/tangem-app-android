package com.tangem.features.tester.api

/**
 * Interface for launching the tester menu
 *
[REDACTED_AUTHOR]
 */
interface TesterMenuLauncher {

    /**
     * Observer for key events to open the tester menu.
     * Implementations should handle key events and return true if the event was consumed.
     */
    val launchOnKeyEventObserver: KeyEventObserver

    /**
     * Registers an app launcher shortcut (shown on long tap of the app icon) that opens the tester menu.
     * Must be called only for builds where the tester menu is available.
     */
    fun registerTesterMenuShortcut()
}