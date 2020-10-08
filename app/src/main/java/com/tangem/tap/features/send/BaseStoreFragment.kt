package com.tangem.tap.features.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.rekotlin.StoreSubscriber

/**
* [REDACTED_AUTHOR]
 */
abstract class BaseStoreFragment(layoutId: Int) : Fragment(layoutId) {

    abstract fun subscribeToStore()

    protected lateinit var mainView: View
    protected val storeSubscribersList = mutableListOf<StoreSubscriber<*>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainView = super.onCreateView(inflater, container, savedInstanceState)!!
        return mainView
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

    fun showRetrySnackbar(message: String, action: () -> Unit) {
        val snackbar = Snackbar.make(mainView, message, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(getString(R.string.common_retry)) {
            snackbar.dismiss()
            action()
        }.show()
    }
}