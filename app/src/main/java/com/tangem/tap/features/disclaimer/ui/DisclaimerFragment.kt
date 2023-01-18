package com.tangem.tap.features.disclaimer.ui

import android.os.Bundle
import android.view.View
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tangem.core.ui.fragments.setStatusBarColor
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.features.BaseFragment
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.disclaimer.Disclaimer
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerState
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentDisclaimerBinding
import org.rekotlin.StoreSubscriber

class DisclaimerFragment : BaseFragment(R.layout.fragment_disclaimer), StoreSubscriber<DisclaimerState> {

    private val binding: FragmentDisclaimerBinding by viewBinding(FragmentDisclaimerBinding::bind)
    private val webViewClient = DisclaimerWebViewClient()

    override fun configureTransitions() {
        val inflater = TransitionInflater.from(requireContext())
        when (store.state.disclaimerState.showedFromScreen) {
            AppScreen.Home -> {
                enterTransition = inflater.inflateTransition(android.R.transition.slide_bottom)
                exitTransition = inflater.inflateTransition(android.R.transition.slide_top)
            }
            AppScreen.Details -> {
                enterTransition = inflater.inflateTransition(android.R.transition.fade)
                exitTransition = inflater.inflateTransition(android.R.transition.fade)
            }
            else -> {}
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addBackPressHandler(handler = this)

        binding.apply {
            toolbar.setNavigationOnClickListener { handleOnBackPressed() }
            webView.apply {
                settings.allowFileAccess = false
                settings.javaScriptEnabled = false
                webViewClient = this@DisclaimerFragment.webViewClient
            }
            webView.hide()
            groupError.hide()
            groupAccept.hide()
            groupLoading.hide()

            btnAccept.setOnClickListener {
                store.dispatch(DisclaimerAction.AcceptDisclaimer)
            }
            btnRepeat.setOnClickListener {
                webViewClient.reset()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setStatusBarColor(R.color.backgroundLightGray)

        webViewClient.onProgressStateChanged = { store.dispatch(DisclaimerAction.OnProgressStateChanged(it)) }
        store.subscribe(subscriber = this) { state ->
            state
                .skipRepeats { oldState, newState -> oldState.disclaimerState == newState.disclaimerState }
                .select(AppState::disclaimerState)
        }
    }

    override fun onStop() {
        webViewClient.onProgressStateChanged = null
        store.unsubscribe(this)
        super.onStop()
    }

    override fun newState(state: DisclaimerState) = with(binding) {
        if (activity == null || view == null) return

        updateUiVisibility(state.disclaimer, state.progressState)
        binding.webView.loadUrl(state.disclaimer.getUri().toString())
    }

    private fun updateUiVisibility(disclaimer: Disclaimer, progressState: ProgressState?) = with(binding) {
        when (progressState) {
            ProgressState.Loading -> {
                root.beginDelayedTransition()
                webView.hide()
                groupError.hide()
                groupAccept.hide()
                groupLoading.show()
            }
            ProgressState.Done -> {
                root.beginDelayedTransition()
                groupError.hide()
                groupLoading.hide()
                webView.show()
                groupAccept.show(!disclaimer.isAccepted())
            }
            ProgressState.Error -> {
                root.beginDelayedTransition()
                webView.hide()
                groupAccept.hide()
                groupLoading.hide()
                groupError.show()
            }
            else -> {}
        }
    }

    override fun handleOnBackPressed() {
        store.dispatch(DisclaimerAction.OnBackPressed)
    }
}
