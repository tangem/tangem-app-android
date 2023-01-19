package com.tangem.tap.features.onboarding.products.note

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import coil.load
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.Onboarding
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
 * Created by Anton Zhilenkov on 26/08/2021.
 */
class OnboardingNoteFragment : BaseOnboardingFragment<OnboardingNoteState>() {

    private var previousStep: OnboardingNoteStep = OnboardingNoteStep.None
    private val mainBinding by lazy { binding.vMain }

    private lateinit var btnRefreshBalanceWidget: RefreshBalanceWidget

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addBackPressHandler(this)

        mainBinding.onboardingTopContainer.imvFrontCard.transitionName = ShareElement.imvFrontCard

        binding.toolbar.setTitle(R.string.onboarding_title)
        btnRefreshBalanceWidget = RefreshBalanceWidget(mainBinding.onboardingTopContainer.onboardingWalletContainer)

        store.dispatch(OnboardingNoteAction.Init)
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

        mainBinding.onboardingTopContainer.imvFrontCard.load(state.cardArtworkUrl) {
            placeholder(R.drawable.card_placeholder_black)
            error(R.drawable.card_placeholder_black)
            fallback(R.drawable.card_placeholder_black)
        }

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

        with(mainBinding.onboardingTopContainer.onboardingTvBalance) {
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

    private fun setupNoneState() = with(mainBinding.onboardingActionContainer) {
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
        with(mainBinding.onboardingActionContainer) {
            btnMainAction.setText(R.string.onboarding_create_wallet_button_create_wallet)
            btnMainAction.setOnClickListener {
                Analytics.send(Onboarding.CreateWallet.ButtonCreateWallet())
                store.dispatch(OnboardingNoteAction.CreateWallet)
            }
            btnAlternativeAction.setText(R.string.onboarding_button_what_does_it_mean)
            btnAlternativeAction.setOnClickListener { }

            btnRefreshBalanceWidget.mainView.setOnClickListener(null)

            tvHeader.setText(R.string.onboarding_create_wallet_header)
            tvBody.setText(R.string.onboarding_create_wallet_body)

            mainBinding.onboardingTopContainer.imvCardBackground.setBackgroundDrawable(
                requireContext().getDrawableCompat(R.drawable.shape_circle),
            )
            updateConstraints(state.currentStep, R.layout.lp_onboarding_create_wallet)

            btnAlternativeAction.isVisible = false // temporary
        }

    private fun setupTopUpWalletState(state: OnboardingNoteState) = with(mainBinding.onboardingActionContainer) {
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
                    amount,
                    state.walletBalance.currency.currencySymbol,
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

        val drawable = requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8)
        mainBinding.onboardingTopContainer.imvCardBackground.setBackgroundDrawable(drawable)
        updateConstraints(state.currentStep, R.layout.lp_onboarding_topup_wallet)
    }

    private fun setupDoneState(state: OnboardingNoteState) = with(mainBinding.onboardingActionContainer) {
        btnMainAction.setText(R.string.common_continue)
        btnMainAction.setOnClickListener {
            showConfetti(false)
            store.dispatch(OnboardingNoteAction.Done)
        }

        btnAlternativeAction.isVisible = false
        btnAlternativeAction.text = ""
        btnAlternativeAction.setOnClickListener { }
        btnRefreshBalanceWidget.mainView.setOnClickListener(null)

        tvHeader.setText(R.string.onboarding_done_header)
        tvBody.setText(R.string.onboarding_done_body)

        mainBinding.onboardingTopContainer.imvCardBackground.setBackgroundDrawable(
            requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8),
        )
        updateConstraints(state.currentStep, R.layout.lp_onboarding_done_activation)
    }

    private fun updateConstraints(currentStep: OnboardingNoteStep, @LayoutRes layoutId: Int) {
        if (this.previousStep == currentStep) return

        with(mainBinding.onboardingTopContainer) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(requireContext(), layoutId)
            constraintSet.applyTo(onboardingWalletContainer)
            val transition = InternalNoteLayoutTransition()
            transition.interpolator = OvershootInterpolator()
            TransitionManager.beginDelayedTransition(onboardingWalletContainer, transition)
        }
    }
}
