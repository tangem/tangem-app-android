package com.tangem.feature.referral.router

import androidx.fragment.app.FragmentManager
import java.lang.ref.WeakReference

internal class ReferralRouter(private val fragmentManager: WeakReference<FragmentManager>) {

    fun back() {
        fragmentManager.get()?.popBackStack()
    }
}
