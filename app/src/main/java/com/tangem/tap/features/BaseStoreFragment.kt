package com.tangem.tap.features

import com.tangem.tap.store
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
abstract class BaseStoreFragment(layoutId: Int) : BaseFragment(layoutId) {

    protected val storeSubscribersList = mutableListOf<StoreSubscriber<*>>()

    abstract fun subscribeToStore()

    override fun onStart() {
        super.onStart()
        subscribeToStore()
    }

    override fun onStop() {
        storeSubscribersList.forEach { store.unsubscribe(it) }
        storeSubscribersList.clear()
        super.onStop()
    }
}