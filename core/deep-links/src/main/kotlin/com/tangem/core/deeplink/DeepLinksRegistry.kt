package com.tangem.core.deeplink

import android.content.Intent

// TODO: Add tests
/**
 * Provides functionality to handle deep links.
 *
 * Allows deep links to be launched, registered, or unregistered.
 */
interface DeepLinksRegistry {

    /**
     * Finds matches registered deep links for the given [intent] and launches them.
     *
     * @return `true` if any deep link was received, `false` otherwise.
     */
    fun launch(intent: Intent): Boolean

    /**
     * Registers the given [deepLink].
     */
    fun register(deepLink: DeepLink)

    /**
     * Registers the given [deepLinks].
     */
    fun register(deepLinks: Collection<DeepLink>)

    /**
     * Unregisters the given [deepLinks].
     */
    fun unregister(deepLinks: Collection<DeepLink>)

    /**
     * Unregisters the given [deepLink].
     */
    fun unregister(deepLink: DeepLink)

    /**
     * Unregisters deep links with the given [ids].
     * */
    fun unregisterByIds(ids: Collection<String>)

    /**
     * Triggers run last launched [Intent] with deeplink handlers that can handle delayed deeplink
     * after handle [Intent] clear that and second time no intent will be handled
     */
    fun triggerDelayedDeeplink()

    fun cancelDelayedDeeplink()
}