package com.tangem.tap.features.disclaimer.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerAction
import com.tangem.tap.features.disclaimer.redux.DisclaimerState
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_disclaimer.*
import org.rekotlin.StoreSubscriber

class DisclaimerFragment : Fragment(R.layout.fragment_disclaimer),
        StoreSubscriber<DisclaimerState> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(android.R.transition.slide_bottom)
        exitTransition = inflater.inflateTransition(android.R.transition.slide_top)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.disclaimerState == newState.disclaimerState
            }.select { it.disclaimerState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            store.dispatch(NavigationAction.PopBackTo())
        }
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        btn_accept.setOnClickListener { store.dispatch(DisclaimerAction.AcceptDisclaimer) }
    }

    override fun newState(state: DisclaimerState) {
        if (activity == null) return

        if (state.accepted) {
            half_transparent_overlay.hide()
            btn_accept.hide()
        }
    }
}