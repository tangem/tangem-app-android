package com.tangem.tap.features.onboarding.products.note

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.squareup.picasso.Picasso
import com.tangem.blockchain.common.Blockchain
import com.tangem.tangem_sdk_new.extensions.fadeIn
import com.tangem.tangem_sdk_new.extensions.fadeOut
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.navigation.ShareElement
import com.tangem.tap.common.toggleWidget.RefreshBalanceWidget
import com.tangem.tap.common.transitions.InternalNoteLayoutTransition
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.onboarding.products.BaseOnboardingFragment
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteState
import com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteStep
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class OnboardingNoteFragment : BaseOnboardingFragment<OnboardingNoteState>() {

    private var previousStep: OnboardingNoteStep = OnboardingNoteStep.None

    private lateinit var btnRefreshBalanceWidget: RefreshBalanceWidget

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
        btnRefreshBalanceWidget = RefreshBalanceWidget(binding.onboardingTopContainer.onboardingMainContainer)

        store.dispatch(OnboardingNoteAction.LoadCardArtwork)
        store.dispatch(OnboardingNoteAction.DetermineStepOfScreen)
    }

    override fun subscribeToStore() {
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.onboardingNoteState == newState.onboardingNoteState
            }.select { it.onboardingNoteState }
        }
        storeSubscribersList.add(this)
    }

    override fun newState(state: OnboardingNoteState) {
        if (activity == null || view == null) return

        Picasso.get()
            .load(state.cardArtworkUrl)
            .error(R.drawable.card_placeholder_black)
            .placeholder(R.drawable.card_placeholder_black)
            ?.into(binding.onboardingTopContainer.imvFrontCard)

        pbBinding.pbState.max = state.steps.size - 1
        pbBinding.pbState.progress = state.progress

        when (state.currentStep) {
            OnboardingNoteStep.None -> setupNoneState()
            OnboardingNoteStep.CreateWallet -> setupCreateWalletState(state)
            OnboardingNoteStep.TopUpWallet -> setupTopUpWalletState(state)
            OnboardingNoteStep.Done -> setupDoneState(state)
        }
        setBalance(state)
        showConfetti(state.showConfetti)
        previousStep = state.currentStep
    }

    private fun setBalance(state: OnboardingNoteState) {
        if (state.walletBalance.currency.blockchain == Blockchain.Unknown) return

        with (binding.onboardingTopContainer.onboardingTvBalance) {
            if (state.balanceCriticalError == null) {
                val balanceValue = state.walletBalance.value.stripZeroPlainString()
                tvBalanceValue.text = balanceValue
                tvBalanceCurrency.text = state.walletBalance.currency.currencySymbol
            } else {
                tvBalanceValue.text = "–"
                tvBalanceCurrency.text = ""
            }
        }
    }

    private fun setupNoneState() = with(binding.onboardingActionContainer) {
        btnMainAction.isVisible = false
        btnAlternativeAction.isVisible = false
        btnRefreshBalanceWidget.show(false)
        tvHeader.isVisible = false
        tvBody.isVisible = false

        btnMainAction.text = ""
        btnAlternativeAction.text = ""
        tvHeader.text = ""
        tvBody.text = ""
    }

    private fun setupCreateWalletState(state: OnboardingNoteState) =
        with(binding.onboardingActionContainer) {
            btnMainAction.setText(R.string.onboarding_create_wallet_button_create_wallet)
            btnMainAction.setOnClickListener { store.dispatch(OnboardingNoteAction.CreateWallet) }
            btnAlternativeAction.setText(R.string.onboarding_button_what_does_it_mean)
            btnAlternativeAction.setOnClickListener { }

            btnRefreshBalanceWidget.mainView.setOnClickListener(null)

            tvHeader.setText(R.string.onboarding_create_wallet_header)
            tvBody.setText(R.string.onboarding_create_wallet_body)

            binding.onboardingTopContainer.imvCardBackground.setBackgroundDrawable(
                requireContext().getDrawableCompat(R.drawable.shape_circle)
            )
            updateConstraints(state.currentStep, R.layout.lp_onboarding_create_wallet)

            btnAlternativeAction.isVisible = false // temporary
        }

    private fun setupTopUpWalletState(state: OnboardingNoteState) = with(binding.onboardingActionContainer) {
        if (state.isBuyAllowed) {
            btnMainAction.setText(R.string.onboarding_top_up_button_but_crypto)
            btnMainAction.setOnClickListener {
                store.dispatch(OnboardingNoteAction.TopUp)
            }

            btnAlternativeAction.isVisible = true
            btnAlternativeAction.setText(R.string.onboarding_top_up_button_show_wallet_address)
            btnAlternativeAction.setOnClickListener {
                store.dispatch(OnboardingNoteAction.ShowAddressInfoDialog)
            }
        } else {
            btnMainAction.setText(R.string.onboarding_button_receive_crypto)
            btnMainAction.setOnClickListener {
                store.dispatch(OnboardingNoteAction.ShowAddressInfoDialog)
            }

            btnAlternativeAction.isVisible = false
        }

        tvHeader.setText(R.string.onboarding_top_up_header)
        if (state.balanceNonCriticalError == null) {
            tvBody.setText(R.string.onboarding_top_up_body)
        } else {
            state.walletBalance.amountToCreateAccount?.let { amount ->
                val tvBodyMessage = getString(
                    R.string.onboarding_top_up_body_no_account_error,
                    amount, state.walletBalance.currency.currencySymbol
                )
                tvBody.text = tvBodyMessage
            }
        }

        btnRefreshBalanceWidget.changeState(state.walletBalance.state)
        if (btnRefreshBalanceWidget.isShowing != true) {
            btnRefreshBalanceWidget.mainView.setOnClickListener {
                store.dispatch(OnboardingNoteAction.Balance.Update)
            }
        }
        binding.onboardingTopContainer.imvCardBackground
            .setBackgroundDrawable(requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8))
        updateConstraints(state.currentStep, R.layout.lp_onboarding_topup_wallet)
    }

    private fun setupDoneState(state: OnboardingNoteState) = with(binding.onboardingActionContainer) {
        btnMainAction.setText(R.string.onboarding_done_button_continue)
        btnMainAction.setOnClickListener {
            showConfetti(false)
            store.dispatch(OnboardingNoteAction.Done)
        }

        btnAlternativeAction.isVisible = false
        btnAlternativeAction.setText("")
        btnAlternativeAction.setOnClickListener { }
        btnRefreshBalanceWidget.mainView.setOnClickListener(null)

        tvHeader.setText(R.string.onboarding_done_header)
        tvBody.setText(R.string.onboarding_done_body)

        binding.onboardingTopContainer.imvCardBackground.setBackgroundDrawable(
            requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8)
        )
        updateConstraints(state.currentStep, R.layout.lp_onboarding_done_activation)
    }

    private fun updateConstraints(currentStep: OnboardingNoteStep, @LayoutRes layoutId: Int) {
        if (this.previousStep == currentStep) return

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

fun ImageView.swapToBitmapDrawable(bitmap: Bitmap?, duration: Long = 200) {
    if (drawable is BitmapDrawable && (drawable as BitmapDrawable).bitmap == bitmap) return

    fadeOut(duration) {
        setImageBitmap(bitmap)
        fadeIn(duration)
    }
}