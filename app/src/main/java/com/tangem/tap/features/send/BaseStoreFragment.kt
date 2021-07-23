package com.tangem.tap.features.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.wallet.redux.WalletAction
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
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val externalTransactionData = store.state.sendState.externalTransactionData
                if (externalTransactionData == null) {
                    store.dispatch(NavigationAction.PopBackTo())
                } else {
                    store.dispatch(
                        WalletAction.TradeCryptoAction.FinishSelling(externalTransactionData.transactionId)
                    )
                }
            }
        })
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
        toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
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