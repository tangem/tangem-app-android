package com.tangem.tap.features.disclaimer.ui

import android.os.Bundle
import android.view.View
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.core.ui.fragments.setStatusBarColor
import com.tangem.tap.common.extensions.configureSettings
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.BaseFragment
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerState
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentDisclaimerBinding
import org.rekotlin.StoreSubscriber

class DisclaimerFragment : BaseFragment(R.layout.fragment_disclaimer), StoreSubscriber<DisclaimerState> {

    private val binding: FragmentDisclaimerBinding by viewBinding(FragmentDisclaimerBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addBackPressHandler(handler = this)
        binding.toolbar.setNavigationOnClickListener { handleOnBackPressed() }
        binding.webView.configureSettings()
    }

    override fun onStart() {
        super.onStart()
        setStatusBarColor(R.color.backgroundLightGray)
        store.subscribe(subscriber = this) { state ->
            state
                .skipRepeats { oldState, newState -> oldState.disclaimerState == newState.disclaimerState }
                .select(AppState::disclaimerState)
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun configureTransitions() {
        super.configureTransitions()
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(android.R.transition.fade)
        exitTransition = inflater.inflateTransition(android.R.transition.fade)
    }

    override fun newState(state: DisclaimerState) = with(binding) {
        if (activity == null || view == null) return

        if (state.accepted) {
            halfTransparentOverlay.hide()
            btnAccept.hide()
        }

        btnAccept.setOnClickListener {
            store.dispatch(DisclaimerAction.AcceptDisclaimer(state.type))
        }

        webView.loadUrl(state.type.uri.toString())
    }

    override fun handleOnBackPressed() {
        store.dispatch(DisclaimerAction.OnBackPressed)
    }
}