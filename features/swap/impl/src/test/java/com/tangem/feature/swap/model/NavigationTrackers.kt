package com.tangem.feature.swap.model

import com.arkivanov.decompose.router.slot.SlotNavigation

/**
 * Minimal stand-in for `childSlot`: tracks the active configuration and invokes each navigation
 * event's completion callback, which `DefaultSlotNavigation` leaves to its host. [results] holds
 * one entry per navigation event, so a `dismiss()` shows up as a single `null`.
 */
internal class SlotTracker<C : Any>(navigation: SlotNavigation<C>) {

    val results = mutableListOf<C?>()

    private var configuration: C? = null

    init {
        navigation.subscribe { event ->
            val oldConfiguration = configuration
            configuration = event.transformer(oldConfiguration)
            results += configuration
            event.onComplete(configuration, oldConfiguration)
        }
    }
}