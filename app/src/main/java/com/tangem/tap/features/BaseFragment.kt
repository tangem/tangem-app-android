package com.tangem.tap.features

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
abstract class BaseFragment(layoutId: Int) : Fragment(layoutId), FragmentOnBackPressedHandler {

    protected lateinit var mainView: View

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

    override fun handleOnBackPressed() {
        store.dispatch(NavigationAction.PopBackTo())
    }

    fun showRetrySnackbar(message: String, action: VoidCallback) {
        val snackbar = Snackbar.make(mainView, message, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(getString(R.string.common_retry)) {
            snackbar.dismiss()
            action()
        }.show()
    }
}

interface FragmentOnBackPressedHandler {
    fun handleOnBackPressed()
}

@SuppressLint("FragmentBackPressedCallback")
fun Fragment.addBackPressHandler(handler: FragmentOnBackPressedHandler) {
    activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handler.handleOnBackPressed()
        }
    })
    view?.findViewById<Toolbar>(R.id.toolbar)?.setNavigationOnClickListener { activity?.onBackPressed() }
}