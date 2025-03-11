package com.tangem.core.deeplink

import android.content.Intent
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel

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
     *
     * @see registerWithLifecycle
     * @see registerWithViewModel
     */
    fun register(deepLink: DeepLink)

    /**
     * Registers the given [deepLinks].
     *
     * @see registerWithLifecycle
     * @see registerWithViewModel
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
     * Registers the [deepLinks] when the [owner] is resumed and ensures that they are unregistered when the [owner] is
     * stopped.
     */
    fun registerWithLifecycle(owner: LifecycleOwner, deepLinks: Collection<DeepLink>)

    /**
     * Registers the [deepLinks] and ensures that they are unregistered when the [ViewModel] is closed.
     */
    fun registerWithViewModel(viewModel: ViewModel, deepLinks: Collection<DeepLink>)

    /**
     * Triggers run last launched [Intent] with deeplink handlers that can handle delayed deeplink
     * after handle [Intent] clear that and second time no intent will be handled
     */
    fun triggerDelayedDeeplink()

    fun cancelDelayedDeeplink()
}