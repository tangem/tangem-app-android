package com.tangem.tap.features.onboarding.products.note

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.postUi
import com.tangem.tap.common.toggleWidget.RefreshBalanceWidget
import com.tangem.tap.common.transitions.InternalNoteLayoutTransition
import com.tangem.tap.features.onboarding.products.BaseOnboardingFragment
import com.tangem.tap.features.onboarding.products.note.redux.*
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.layout_onboarding_bottom_action_views.*
import kotlinx.android.synthetic.main.layout_onboarding_note.*
import kotlinx.android.synthetic.main.view_onboarding_tv_balance.*

/**
[REDACTED_AUTHOR]
 */
class OnboardingNoteFragment : BaseOnboardingFragment<OnboardingNoteState>() {

    private lateinit var btnRefreshBalanceWidget: RefreshBalanceWidget

    override fun getOnboardingTopContainerId(): Int = R.layout.layout_onboarding_note

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnRefreshBalanceWidget = RefreshBalanceWidget(onboarding_main_container)
        val resources = AndroidResources(
                OnboardingStringResources(
                        R.string.copy_toast_msg
                )
        )

        store.dispatch(OnboardingNoteAction.SetResources(resources))
        store.dispatch(OnboardingNoteAction.DetermineStepOfScreen)
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

        state.artworkBitmap?.let { imv_front_card.setImageBitmap(it) }
        pb_state.max = state.steps.size - 1
        pb_state.progress = state.progress

        when (state.currentStep) {
            OnboardingNoteStep.None -> setupNoneState(state)
            OnboardingNoteStep.CreateWallet -> setupCreateWalletState(state)
            OnboardingNoteStep.TopUpWallet -> setupTopUpWalletState(state)
            OnboardingNoteStep.Done -> setupDoneState(state)
        }
        setBalance(state)
        showConfetti(state.showConfetti)
    }

    private fun setBalance(state: OnboardingNoteState) {
        if (state.balanceCurrency.blockchain == Blockchain.Unknown) return

        val balanceValue = state.balanceValue.stripZeroPlainString()
        val currency = state.balanceCurrency.currencySymbol
        tv_balance_value.text = "$balanceValue $currency"
    }

    private fun setupNoneState(state: OnboardingNoteState) {
        btn_main_action.isVisible = false
        btn_alternative_action.isVisible = false
        btnRefreshBalanceWidget.show(false)
        tv_header.isVisible = false
        tv_body.isVisible = false

        btn_main_action.text = ""
        btn_alternative_action.text = ""
        tv_header.text = ""
        tv_body.text = ""
    }

    private fun setupCreateWalletState(state: OnboardingNoteState) {
        btn_main_action.setText(R.string.onboarding_create_wallet_button_create_wallet)
        btn_main_action.setOnClickListener { store.dispatch(OnboardingNoteAction.CreateWallet) }
        btn_alternative_action.setText(R.string.onboarding_button_what_does_it_mean)
        btn_alternative_action.setOnClickListener { }

        btnRefreshBalanceWidget.mainView.setOnClickListener(null)

        tv_header.setText(R.string.onboarding_create_wallet_header)
        tv_body.setText(R.string.onboarding_create_wallet_body)

        imv_card_background.setBackgroundDrawable(requireContext().getDrawableCompat(R.drawable.shape_circle))
        updateConstraints(R.layout.lp_onboarding_create_wallet)
    }

    private fun setupTopUpWalletState(state: OnboardingNoteState) {
        btn_main_action.setText(R.string.onboarding_top_up_button_but_crypto)
        btn_main_action.setOnClickListener {
            store.dispatch(OnboardingNoteAction.TopUp)
        }

        btn_alternative_action.setText(R.string.onboarding_top_up_button_show_wallet_address)
        btn_alternative_action.setOnClickListener {
            store.dispatch(OnboardingNoteAction.ShowAddressInfoDialog)
        }

        tv_header.setText(R.string.onboarding_top_up_header)
        tv_body.setText(R.string.onboarding_top_up_body)

        btnRefreshBalanceWidget.changeState(state.balanceState)
        if (btnRefreshBalanceWidget.isShowing != true) {
            postUi(300) {
                btnRefreshBalanceWidget.mainView.setOnClickListener {
                    store.dispatch(OnboardingNoteAction.Balance.Update)
                }
            }
        }
        imv_card_background.setBackgroundDrawable(requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8))
        updateConstraints(R.layout.lp_onboarding_topup_wallet)
    }

    private fun setupDoneState(state: OnboardingNoteState) {
        btn_main_action.setText(R.string.onboarding_done_button_continue)
        btn_main_action.setOnClickListener {
            showConfetti(false)
            store.dispatch(OnboardingNoteAction.Done)
        }

        btn_alternative_action.isVisible = false
        btn_alternative_action.setText("")
        btn_alternative_action.setOnClickListener { }
        btnRefreshBalanceWidget.mainView.setOnClickListener(null)

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