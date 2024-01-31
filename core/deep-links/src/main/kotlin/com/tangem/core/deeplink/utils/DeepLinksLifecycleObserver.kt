package com.tangem.core.deeplink.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.core.deeplink.DeepLink
import com.tangem.core.deeplink.DeepLinksRegistry

internal class DeepLinksLifecycleObserver(
    private val deepLinksRegistry: DeepLinksRegistry,
    private val deepLinks: Collection<DeepLink>,
) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        deepLinksRegistry.register(deepLinks)
    }

    override fun onPause(owner: LifecycleOwner) {
        deepLinksRegistry.unregister(deepLinks)
    }
}