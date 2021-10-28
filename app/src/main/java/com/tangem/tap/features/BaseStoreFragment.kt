package com.tangem.tap.features

import com.tangem.tap.store
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
abstract class BaseStoreFragment(layoutId: Int) : BaseFragment(layoutId) {

    abstract fun subscribeToStore()
    protected val storeSubscribersList = mutableListOf<StoreSubscriber<*>>()

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