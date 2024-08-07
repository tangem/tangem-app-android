package com.tangem.tap.features.disclaimer.ui

import android.os.Bundle
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.webkit.WebView
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionInflater
import by.kirich1409.viewbindingdelegate.viewBinding

import com.tangem.core.ui.extensions.setStatusBarColor
import com.tangem.tap.common.entities.ProgressState
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.BaseFragment
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.disclaimer.Disclaimer
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerSource
import com.tangem.tap.features.disclaimer.redux.DisclaimerState
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentDisclaimerBinding
import kotlinx.coroutines.launch
import org.rekotlin.StoreSubscriber

class DisclaimerFragment : BaseFragment(R.layout.fragment_disclaimer), StoreSubscriber<DisclaimerState> {

    private val binding: FragmentDisclaimerBinding by viewBinding(FragmentDisclaimerBinding::bind)
    private val webViewClient = DisclaimerWebViewClient()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addBackPressHandler(handler = this)

        binding.apply {
            toolbar.setNavigationOnClickListener { handleOnBackPressed() }
            webView.apply {
                settings.allowFileAccess = false
                settings.javaScriptEnabled = false
                overScrollMode = OVER_SCROLL_NEVER
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
        setStatusBarColor(R.color.background_secondary)

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

    override fun configureTransitions() {
        val inflater = TransitionInflater.from(requireContext())
        when (store.state.disclaimerState.showedFrom) {
            DisclaimerSource.Home -> {
                enterTransition = inflater.inflateTransition(android.R.transition.slide_bottom)
                exitTransition = inflater.inflateTransition(android.R.transition.slide_top)
            }
            DisclaimerSource.Details -> {
                super.configureTransitions()
            }
        }
    }

    override fun handleOnBackPressed() {
        store.dispatch(DisclaimerAction.OnBackPressed)
    }

    override fun newState(state: DisclaimerState) {
        return with(binding) {
            if (activity == null || view == null) return

            updateUiVisibility(state.disclaimer, state.progressState)
        }
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
                lifecycleScope.launch {
                    groupAccept.show(!disclaimer.isAccepted())
                }
            }
            ProgressState.Error -> {
                root.beginDelayedTransition()
                webView.show()
                groupAccept.show()
                groupLoading.hide()
                groupError.hide()
                webView.loadLocalTermsOfServices()
            }
            else -> {
                webView.setBackgroundColor(resources.getColor(R.color.transparent, null))
                webView.loadUrl(disclaimer.getUri().toString())
            }
        }
    }

    private fun WebView.loadLocalTermsOfServices() {
        loadData(localTermsOfServices, "text/html", "UTF-8")
    }
}