package com.tangem.core.deeplink.utils

import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.deeplink.DeepLink
import com.tangem.core.deeplink.DeepLinksRegistry

fun AppComponentContext.registerDeepLinks(registry: DeepLinksRegistry, vararg deepLinks: DeepLink) {
    registerDeepLinks(registry, deepLinks.toList())
}

fun AppComponentContext.registerDeepLinks(registry: DeepLinksRegistry, deepLinks: Collection<DeepLink>) {
    lifecycle.subscribe(
        onCreate = {
            registry.register(deepLinks)
        },
        onDestroy = {
            registry.unregister(deepLinks)
        },
    )
}