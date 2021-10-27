package com.tangem.tap.features.onboarding.products.otherCards

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.common.redux.navigation.ShareElement
import com.tangem.tap.common.transitions.InternalNoteLayoutTransition
import com.tangem.tap.features.onboarding.products.BaseOnboardingFragment
import com.tangem.tap.features.onboarding.products.note.swapToBitmapDrawable
import com.tangem.tap.features.onboarding.products.otherCards.redux.OnboardingOtherCardsAction
import com.tangem.tap.features.onboarding.products.otherCards.redux.OnboardingOtherCardsState
import com.tangem.tap.features.onboarding.products.otherCards.redux.OnboardingOtherCardsStep
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_onboarding_main.*
import kotlinx.android.synthetic.main.layout_onboarding_container_bottom.*
import kotlinx.android.synthetic.main.layout_onboarding_container_top.*
import kotlinx.android.synthetic.main.view_onboarding_progress.*

/**
[REDACTED_AUTHOR]
 */
class OnboardingOtherCardsFragment : BaseOnboardingFragment<OnboardingOtherCardsState>() {

    override fun getOnboardingTopContainerId(): Int = R.layout.layout_onboarding_container_top

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imv_front_card.transitionName = ShareElement.imvFrontCard
        startPostponedEnterTransition()

        toolbar.setTitle(R.string.onboarding_title)
        store.dispatch(OnboardingOtherCardsAction.LoadCardArtwork)
        store.dispatch(OnboardingOtherCardsAction.DetermineStepOfScreen)
    }

    override fun subscribeToStore() {
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.onboardingOtherCardsState == newState.onboardingOtherCardsState
            }.select { it.onboardingOtherCardsState }
        }
        storeSubscribersList.add(this)
    }

    override fun newState(state: OnboardingOtherCardsState) {
        if (activity == null) return
        if (state.currentStep == OnboardingOtherCardsStep.None) return

        state.cardArtwork?.let { imv_front_card.swapToBitmapDrawable(it.artwork) }
        pb_state.max = state.steps.size - 1
        pb_state.progress = state.progress

        when (state.currentStep) {
            OnboardingOtherCardsStep.CreateWallet -> setupCreateWalletState(state)
            OnboardingOtherCardsStep.Done -> setupDoneState(state)
        }
        showConfetti(state.showConfetti)
    }

    private fun setupCreateWalletState(state: OnboardingOtherCardsState) {
        btn_main_action.setText(R.string.onboarding_create_wallet_button_create_wallet)
        btn_main_action.setOnClickListener { store.dispatch(OnboardingOtherCardsAction.CreateWallet) }
        btn_alternative_action.setText(R.string.onboarding_button_what_does_it_mean)
        btn_alternative_action.setOnClickListener { }

        tv_header.setText(R.string.onboarding_create_wallet_header)
        tv_body.setText(R.string.onboarding_create_wallet_body)

        imv_card_background.setBackgroundDrawable(requireContext().getDrawableCompat(R.drawable.shape_circle))
        updateConstraints(R.layout.lp_onboarding_create_wallet)
    }

    private fun setupDoneState(state: OnboardingOtherCardsState) {
        btn_main_action.setText(R.string.onboarding_done_button_continue)
        btn_main_action.setOnClickListener {
            showConfetti(false)
            store.dispatch(OnboardingOtherCardsAction.Done)
        }

        btn_alternative_action.isVisible = false
        btn_alternative_action.setText("")
        btn_alternative_action.setOnClickListener { }

        tv_header.setText(R.string.onboarding_done_header)
        tv_body.setText(R.string.onboarding_done_body)

        imv_card_background.setBackgroundDrawable(requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8))
        updateConstraints(R.layout.lp_onboarding_done)
    }

    private fun updateConstraints(@LayoutRes layoutId: Int) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), layoutId)
        constraintSet.applyTo(onboarding_main_container)
        val transition = InternalNoteLayoutTransition()
        transition.interpolator = OvershootInterpolator()
        TransitionManager.beginDelayedTransition(onboarding_main_container, transition)
    }
}