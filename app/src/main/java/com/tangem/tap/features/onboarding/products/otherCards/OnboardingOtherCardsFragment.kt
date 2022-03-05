package com.tangem.tap.features.onboarding.products.otherCards

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.squareup.picasso.Picasso
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.common.redux.navigation.ShareElement
import com.tangem.tap.common.transitions.InternalNoteLayoutTransition
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.onboarding.products.BaseOnboardingFragment
import com.tangem.tap.features.onboarding.products.otherCards.redux.OnboardingOtherCardsAction
import com.tangem.tap.features.onboarding.products.otherCards.redux.OnboardingOtherCardsState
import com.tangem.tap.features.onboarding.products.otherCards.redux.OnboardingOtherCardsStep
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class OnboardingOtherCardsFragment : BaseOnboardingFragment<OnboardingOtherCardsState>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addBackPressHandler(this)

        binding.onboardingTopContainer.imvFrontCard.transitionName = ShareElement.imvFrontCard
        startPostponedEnterTransition()

        binding.toolbar.setTitle(R.string.onboarding_title)
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
        if (activity == null || view == null) return
        if (state.currentStep == OnboardingOtherCardsStep.None) return

        Picasso.get()
            .load(state.cardArtworkUrl)
            .error(R.drawable.card_placeholder_black)
            .placeholder(R.drawable.card_placeholder_black)
            ?.into(binding.onboardingTopContainer.imvFrontCard)

        pbBinding.pbState.max = state.steps.size - 1
        pbBinding.pbState.progress = state.progress

        when (state.currentStep) {
            OnboardingOtherCardsStep.CreateWallet -> setupCreateWalletState()
            OnboardingOtherCardsStep.Done -> setupDoneState()
        }
        showConfetti(state.showConfetti)
    }

    private fun setupCreateWalletState() = with(binding) {

        with(onboardingActionContainer) {
            btnMainAction.setText(R.string.onboarding_create_wallet_button_create_wallet)
            btnMainAction.setOnClickListener { store.dispatch(OnboardingOtherCardsAction.CreateWallet) }
            btnAlternativeAction.setText(R.string.onboarding_button_what_does_it_mean)
            btnAlternativeAction.setOnClickListener { }

            tvHeader.setText(R.string.onboarding_create_wallet_header)
            tvBody.setText(R.string.onboarding_create_wallet_body)

            btnAlternativeAction.isVisible = false // temporary
        }


        binding.onboardingTopContainer.imvCardBackground.setBackgroundDrawable(
            requireContext().getDrawableCompat(R.drawable.shape_circle)
        )
        updateConstraints(R.layout.lp_onboarding_create_wallet)

    }

    private fun setupDoneState() = with(binding.onboardingActionContainer) {
        btnMainAction.setText(R.string.onboarding_done_button_continue)
        btnMainAction.setOnClickListener {
            showConfetti(false)
            store.dispatch(OnboardingOtherCardsAction.Done)
        }

        btnAlternativeAction.isVisible = false
        btnAlternativeAction.setText("")
        btnAlternativeAction.setOnClickListener { }

        tvHeader.setText(R.string.onboarding_done_header)
        tvBody.setText(R.string.onboarding_done_body)

        binding.onboardingTopContainer.imvCardBackground?.setBackgroundDrawable(
            requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8)
        )
        updateConstraints(R.layout.lp_onboarding_done)
    }

    private fun updateConstraints(@LayoutRes layoutId: Int) {
        with(binding.onboardingTopContainer) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(requireContext(), layoutId)
            constraintSet.applyTo(onboardingMainContainer)
            val transition = InternalNoteLayoutTransition()
            transition.interpolator = OvershootInterpolator()
            TransitionManager.beginDelayedTransition(onboardingMainContainer, transition)
        }
    }
}