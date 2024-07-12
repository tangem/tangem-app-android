package com.tangem.data.card.sdk

import androidx.fragment.app.FragmentActivity

/**
 * Lifecycle observer for creating Card SDK instance
 *
 * @author Andrew Khokhlov on 13/07/2023
 */
interface CardSdkOwner {

    fun register(activity: FragmentActivity)
}
