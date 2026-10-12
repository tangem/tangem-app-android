package com.tangem.features.polymarket.impl.common

import com.arkivanov.decompose.router.slot.SlotNavigation

internal class FakeSlotHost<C : Any>(navigation: SlotNavigation<C>) {

    var configuration: C? = null
        private set

    init {
        navigation.subscribe { event ->
            val old = configuration
            configuration = event.transformer(old)
            event.onComplete(configuration, old)
        }
    }
}