package com.tangem.tap.features.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
abstract class BaseStoreFragment(layoutId: Int) : Fragment(layoutId) {

    abstract fun subscribeToStore()

    protected lateinit var mainView: View
    protected val storeSubscribersList = mutableListOf<StoreSubscriber<*>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureTransitions()
    }

    protected open fun configureTransitions() {
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.slide_right)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainView = super.onCreateView(inflater, container, savedInstanceState)!!
        return mainView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar?.setNavigationOnClickListener { activity?.onBackPressed() }
    }

    override fun onStart() {
        super.onStart()
        subscribeToStore()
    }


    override fun onStop() {
        storeSubscribersList.forEach { store.unsubscribe(it) }
        storeSubscribersList.clear()
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