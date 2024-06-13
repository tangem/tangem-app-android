package com.tangem.tap.features

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.tangem.common.extensions.VoidCallback
import com.tangem.common.routing.AppRouter
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
abstract class BaseFragment(layoutId: Int) : Fragment(layoutId), FragmentOnBackPressedHandler {

    private lateinit var mainView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureTransitions()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainView = super.onCreateView(inflater, container, savedInstanceState)!!
        return mainView
    }

    override fun handleOnBackPressed() {
        store.dispatchNavigationAction(AppRouter::pop)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadToolbarMenu()?.let {
            val menuHost = requireActivity() as? MenuHost ?: return
            menuHost.addMenuProvider(it, viewLifecycleOwner, Lifecycle.State.RESUMED)
        }
    }

    protected open fun loadToolbarMenu(): MenuProvider? = null

    protected open fun configureTransitions() {
        configureDefaultTransactions()
    }

    protected fun configureDefaultTransactions() {
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
        exitTransition = inflater.inflateTransition(R.transition.fade)
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
    requireActivity().onBackPressedDispatcher.addCallback(
        owner = this,
        onBackPressed = { handler.handleOnBackPressed() },
    )

    view?.findViewById<Toolbar>(R.id.toolbar)?.setNavigationOnClickListener {
        handler.handleOnBackPressed()
    }
}