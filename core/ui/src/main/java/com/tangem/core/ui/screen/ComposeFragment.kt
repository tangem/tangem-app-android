package com.tangem.core.ui.screen

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.core.ui.R

/**
 * An abstract base class for fragments that use Compose for UI rendering.
 * Extends [Fragment] and implements [ComposeScreen] interface.
 */
abstract class ComposeFragment : Fragment(), ComposeScreen {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val isTransitionsInflated = TransitionInflater.from(requireContext()).inflateTransitions()

        return createComposeView(inflater.context, requireActivity()).also {
            it.isTransitionGroup = isTransitionsInflated
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        /*
         * We need to manually dispatch configuration changes to the Compose view.
         *
         * This is necessary because the fragment is not recreated when the configuration changes, because
         * `android:configChanges="uiMode"` is set in the manifest.
         * */
        view?.dispatchConfigurationChanged(newConfig)
    }

    /**
     * Inflates transitions for the fragment. Override this method to customize
     * enter and exit transitions for the fragment.
     *
     * @return `true` if transitions were inflated; `false` otherwise.
     */
    protected open fun TransitionInflater.inflateTransitions(): Boolean {
        enterTransition = inflateTransition(R.transition.fade)
        exitTransition = inflateTransition(R.transition.fade)

        return true
    }
}
