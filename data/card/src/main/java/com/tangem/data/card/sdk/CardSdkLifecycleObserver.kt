package com.tangem.data.card.sdk

import android.content.Context

/**
 * Lifecycle observer for creating Card SDK instance
 *
 * @author Andrew Khokhlov on 13/07/2023
 */
interface CardSdkLifecycleObserver {

    /** Callback of creating activity [context]  */
    fun onCreate(context: Context)

    /** Callback of destroying activity */
    fun onDestroy()
}
