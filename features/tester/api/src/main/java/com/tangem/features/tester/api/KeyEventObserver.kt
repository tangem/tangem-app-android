package com.tangem.features.tester.api

import android.view.KeyEvent
import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Interface for observing to key events in a lifecycle-aware manner.
 * Implementations should handle key events and return true if the event was consumed.
 */
interface KeyEventObserver : DefaultLifecycleObserver {

    fun dispatchKeyEvent(event: KeyEvent): Boolean
}