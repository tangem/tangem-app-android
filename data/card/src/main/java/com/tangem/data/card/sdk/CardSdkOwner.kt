package com.tangem.data.card.sdk

import androidx.fragment.app.FragmentActivity

/**
 * Lifecycle observer for creating Card SDK instance
 *
[REDACTED_AUTHOR]
 */
interface CardSdkOwner {

    fun register(activity: FragmentActivity)
}