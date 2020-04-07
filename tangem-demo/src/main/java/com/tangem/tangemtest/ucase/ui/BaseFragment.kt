package com.tangem.tangemtest.ucase.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.tangem.tangemtest.ucase.getDefaultNavigationOptions
import com.tangem.tangemtest.ucase.tunnel.SnackbarHolder
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder

/**
[REDACTED_AUTHOR]
 */
abstract class BaseFragment : Fragment(), LayoutHolder, SnackbarHolder {

    protected lateinit var mainView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainView = inflater.inflate(getLayoutId(), container, false)
        return mainView
    }

    protected fun navigateTo(navigationId: Int, bundle: Bundle? = null, options: NavOptions? = getDefaultNavigationOptions()) {
        findNavController(this).navigate(navigationId, bundle, options)
    }

    override fun showSnackbar(id: Int, length: Int) {
        showSnackbar(requireContext().getString(id), length)
    }

    override fun showSnackbar(message: String, length: Int) {
        Snackbar.make(mainView, message, length).show()
    }
}