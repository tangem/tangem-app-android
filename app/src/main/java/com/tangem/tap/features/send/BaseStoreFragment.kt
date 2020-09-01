package com.tangem.tap.features.send

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.store
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
abstract class BaseStoreFragment(layoutId: Int) : Fragment(layoutId) {

    abstract fun subscribeToStore()

    protected val storeSubscribersList = mutableListOf<StoreSubscriber<*>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener { store.dispatch(NavigationAction.PopBackTo()) }
    }

    override fun onStart() {
        super.onStart()
        subscribeToStore()
    }


    override fun onStop() {
        storeSubscribersList.forEach { store.unsubscribe(it) }
        super.onStop()
    }
}