package com.tangem.data.card.sdk

import android.content.Context

/**
 * Lifecycle observer for creating Card SDK instance
 *
* [REDACTED_AUTHOR]
 */
interface CardSdkLifecycleObserver {

    /** Callback of creating activity [context]  */
    fun onCreate(context: Context)

    /** Callback of destroying activity */
    fun onDestroy()
}
