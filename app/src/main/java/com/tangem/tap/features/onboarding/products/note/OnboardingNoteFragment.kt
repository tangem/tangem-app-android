package com.tangem.tap.features.onboarding.products.note

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.transition.TransitionInflater
import com.tangem.tap.common.extensions.addOnBackPressedDispatcher
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteState
import com.tangem.tap.features.onboarding.redux.OnboardingAction
import com.tangem.tap.features.onboarding.redux.OnboardingNoteStep
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_onboarding.*
import org.rekotlin.StoreSubscriber

/**
[REDACTED_AUTHOR]
 */
class OnboardingNoteFragment : BaseStoreFragment(R.layout.fragment_onboarding), StoreSubscriber<OnboardingNoteState> {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activity?.addOnBackPressedDispatcher {
            store.dispatch(NavigationAction.PopBackTo())
            store.dispatch(OnboardingAction.SetInitialStepOfScreen(OnboardingNoteStep.None))
        }

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        store.dispatch(OnboardingNoteAction.Init)

        imv_front_card.setImageBitmap(store.state.onboardingState.onboardingData?.cardArtwork?.artwork)
    }

    override fun subscribeToStore() {
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.onboardingNoteState == newState.onboardingNoteState
            }.select { it.onboardingNoteState }
        }
    }

    override fun newState(state: OnboardingNoteState) {
        if (activity == null) return

        pb_state.max = state.steps.size - 1

        onboarding_main_container.beginDelayedTransition()
        when (state.currentStep) {
            OnboardingNoteStep.None -> setupNoneState(state)
            OnboardingNoteStep.CreateWallet -> setupCreateWalletState(state)
            OnboardingNoteStep.TopUpWallet -> setupTopUpWalletState(state)
            OnboardingNoteStep.Done -> setupSuccessState(state)
        }
    }

    private fun setupNoneState(state: OnboardingNoteState) {
        pb_state.progress = state.progress

        btn_main_action.isVisible = true
        btn_alternative_action.isVisible = true
        btn_check_balance.isVisible = true
        tv_header.isVisible = true
        tv_body.isVisible = true

        btn_main_action.text = "NULL"
        btn_alternative_action.text = "NULL"
        btn_check_balance.text = "NULL balance"
        tv_header.text = "NULL"
        tv_body.text = "NULL"
    }

    private fun setupCreateWalletState(state: OnboardingNoteState) {
        pb_state.progress = state.progress

        btn_main_action.setText(R.string.onboarding_create_wallet_button_create_wallet)
        btn_main_action.setOnClickListener { store.dispatch(OnboardingNoteAction.CreateWallet) }

        btn_alternative_action.setText(R.string.onboarding_button_what_does_it_mean)
        btn_alternative_action.setOnClickListener { }

        btn_check_balance.isVisible = false
        btn_check_balance.setOnClickListener { }

        tv_header.setText(R.string.onboarding_create_wallet_header)
        tv_body.setText(R.string.onboarding_create_wallet_body)
    }

    private fun setupTopUpWalletState(state: OnboardingNoteState) {
        pb_state.progress = state.progress

        btn_main_action.setText(R.string.onboarding_top_up_button_but_crypto)
        btn_main_action.setOnClickListener { store.dispatch(OnboardingNoteAction.TopUp) }

        btn_alternative_action.setText(R.string.onboarding_top_up_button_show_wallet_address)
        btn_alternative_action.setOnClickListener { }

        btn_check_balance.isVisible = false
        btn_check_balance.setOnClickListener { }

        tv_header.setText(R.string.onboarding_top_up_header)
        tv_body.setText(R.string.onboarding_top_up_body)
    }

    private fun setupSuccessState(state: OnboardingNoteState) {
        pb_state.progress = state.progress

        btn_main_action.setText(R.string.onboarding_done_button_continue)
        btn_main_action.setOnClickListener { store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet)) }

        btn_alternative_action.isVisible = false
        btn_alternative_action.setText("")
        btn_alternative_action.setOnClickListener { }

        btn_check_balance.isVisible = false
        btn_check_balance.setOnClickListener { }

        tv_header.setText(R.string.onboarding_done_header)
        tv_body.setText(R.string.onboarding_done_body)
    }
}