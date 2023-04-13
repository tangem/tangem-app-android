package com.tangem.tap.common.utils

import android.os.Handler
import android.os.Looper
import org.rekotlin.StoreSubscriber

private val mainLooper by lazy { Looper.getMainLooper() }
private val mainHandler by lazy { Handler(mainLooper) }

/**
 * A subscriber interface for safely subscribing to state changes in a Store.
 *
 * @param State the type of the state in the Store
 */
interface SafeStoreSubscriber<State> : StoreSubscriber<State> {

    /**
     * A function that will be called when the state in the Store changes.
     *
     * @param state the new state in the Store
     */
    override fun newState(state: State) {
        if (Thread.currentThread() != mainLooper.thread) {
            mainHandler.post { newStateOnMain(state) }
        } else {
            newStateOnMain(state)
        }
    }

    /**
     * A function that will be called on the main thread when the state in the Store changes.
     *
     * @param state the new state in the Store
     */
    fun newStateOnMain(state: State)
}
