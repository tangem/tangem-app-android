package com.tangem.tap.features.onboarding.products.twins.ui

import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.squareup.picasso.Picasso
import com.tangem.Message
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.leapfrogWidget.LeapfrogWidget
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.redux.navigation.ShareElement
import com.tangem.tap.common.toggleWidget.RefreshBalanceWidget
import com.tangem.tap.common.transitions.InternalNoteLayoutTransition
import com.tangem.tap.domain.twins.AssetReader
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.tap.domain.twins.TwinsCardWidget
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.onboarding.products.BaseOnboardingFragment
import com.tangem.tap.features.onboarding.products.twins.redux.CreateTwinWalletMode
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsState
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import com.tangem.tap.features.wallet.redux.Artwork
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_onboarding_main.*
import kotlinx.android.synthetic.main.layout_onboarding_container_bottom.*
import kotlinx.android.synthetic.main.layout_onboarding_container_top.*
import kotlinx.android.synthetic.main.view_bg_twins_welcome.*
import kotlinx.android.synthetic.main.view_onboarding_progress.*
import kotlinx.android.synthetic.main.view_onboarding_tv_balance.*
import org.rekotlin.Action

class TwinsCardsFragment : BaseOnboardingFragment<TwinCardsState>() {

    private var previousStep = TwinCardsStep.None

    private lateinit var twinsWidget: TwinsCardWidget
    private lateinit var btnRefreshBalanceWidget: RefreshBalanceWidget

    private val assetReader: AssetReader by lazy {
        object : AssetReader {
            override fun readAssetAsString(name: String): String = requireContext().readAssetAsString(name)
        }
    }

    override fun getOnboardingTopContainerId(): Int = R.layout.layout_onboarding_container_top

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        store.dispatch(TwinCardsAction.Init)
    }

    override fun configureTransitions() {
        when (store.state.twinCardsState.mode) {
            CreateTwinWalletMode.CreateWallet -> super.configureTransitions()
            CreateTwinWalletMode.RecreateWallet -> {
                val inflater = TransitionInflater.from(requireContext())
                enterTransition = inflater.inflateTransition(R.transition.slide_right)
                exitTransition = inflater.inflateTransition(R.transition.fade)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addBackPressHandler(this)

        reconfigureLayoutForTwins()
        twinsWidget = TwinsCardWidget(LeapfrogWidget(cards_container)) { 285f }
        btnRefreshBalanceWidget = RefreshBalanceWidget(onboarding_main_container)

        toolbar.title = getText(R.string.twins_recreate_toolbar)

        Picasso.get()
                .load(Artwork.TWIN_CARD_1)
                .error(R.drawable.card_placeholder_black)
                .placeholder(R.drawable.card_placeholder_black)
                ?.into(imv_twin_front_card)

        Picasso.get()
                .load(Artwork.TWIN_CARD_2)
                .error(R.drawable.card_placeholder_white)
                .placeholder(R.drawable.card_placeholder_white)
                ?.into(imv_twin_back_card)
    }

    private fun reconfigureLayoutForTwins() {
        imv_front_card.hide()
        imv_card_background.hide()
        cards_container.show()

        imv_twin_front_card.transitionName = ShareElement.imvFrontCard
        imv_twin_back_card.transitionName = ShareElement.imvBackCard

        // if don't this, the bg_circle_... is overflow the app_bar
        app_bar.bringToFront()
    }

    override fun subscribeToStore() {
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.twinCardsState == newState.twinCardsState
            }.select { it.twinCardsState }
        }
        storeSubscribersList.add(this)
    }

    override fun newState(state: TwinCardsState) {
        if (activity == null) return

        pb_state.max = state.steps.size - 1
        pb_state.progress = state.progress

        when (state.currentStep) {
            TwinCardsStep.WelcomeOnly -> setupWelcomeOnlyState(state)
            TwinCardsStep.Welcome -> setupWelcomeState(state)
            TwinCardsStep.Warning -> setupWarningState(state)
            TwinCardsStep.CreateFirstWallet -> setupCreateFirstWalletState(state)
            TwinCardsStep.CreateSecondWallet -> setupCreateSecondWalletState(state)
            TwinCardsStep.CreateThirdWallet -> setupCreateThirdWalletState(state)
            TwinCardsStep.TopUpWallet -> setupTopUpWalletState(state)
            TwinCardsStep.Done -> setupDoneState(state)
        }

        setBalance(state)
        showConfetti(state.showConfetti)
        previousStep = state.currentStep
    }

    private fun setBalance(state: TwinCardsState) {
        if (state.walletBalance.currency.blockchain == Blockchain.Unknown) return

        if (state.balanceCriticalError == null) {
            val balanceValue = state.walletBalance.value.stripZeroPlainString()
            val currency = state.walletBalance.currency.currencySymbol
            tv_balance_value.text = "$balanceValue $currency"
        } else {
            tv_balance_value.text = "–"
        }
    }

    private fun setupWelcomeOnlyState(state: TwinCardsState) {
        setupWelcomeState(state, NavigationAction.NavigateTo(AppScreen.Wallet))
    }

    private fun setupWelcomeState(state: TwinCardsState) {
        setupWelcomeState(state, TwinCardsAction.SetStepOfScreen(TwinCardsStep.CreateFirstWallet))
    }

    private fun setupWelcomeState(state: TwinCardsState, mainAction: Action) {
        twinsWidget.toWelcome(false) { startPostponedEnterTransition() }

        onboarding_twins_welcome_bg.show()
        pb_state.hide()

        tv_header.setText(R.string.twins_onboarding_subtitle)
        tv_body.text = getString(R.string.twins_onboarding_description_format, state.cardNumber?.pairIndexNumber())

        btn_main_action.setText(R.string.common_continue)
        btn_main_action.setOnClickListener { store.dispatch(mainAction) }
    }

    private fun setupWarningState(state: TwinCardsState) {
        twinsWidget.toWelcome(false) { startPostponedEnterTransition() }

        onboarding_twins_welcome_bg.hide()
        pb_state.hide()
        chb_understand.show()

        tv_header.setText(R.string.common_warning)
        tv_body.setText(R.string.twins_recreate_warning)

        chb_understand.setOnCheckedChangeListener { buttonView, isChecked ->
            store.dispatch(TwinCardsAction.SetUserUnderstand(isChecked))
        }
        btn_main_action.isEnabled = state.userWasUnderstandIfWalletRecreate
        btn_main_action.setText(R.string.common_continue)
        btn_main_action.setOnClickListener {
            store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.CreateFirstWallet))
        }
    }

    private fun setupCreateFirstWalletState(state: TwinCardsState) {
        onboarding_twins_welcome_bg.hide()
        bg_circle_large.hide()
        bg_circle_medium.hide()
        bg_circle_min.hide()
        chb_understand.hide()
        pb_state.show()

        onboarding_main_container.beginDelayedTransition()

        when (previousStep) {
            TwinCardsStep.None -> {
                twinsWidget.toLeapfrog(false) {
                    when (state.cardNumber) {
                        TwinCardNumber.First -> startPostponedEnterTransition()
                        TwinCardNumber.Second -> {
                            switchToCard(state.cardNumber, false) { startPostponedEnterTransition() }
                        }
                    }
                }
            }
            TwinCardsStep.Welcome, TwinCardsStep.Warning -> {
                twinsWidget.toLeapfrog(onEnd = { switchToCard(state.cardNumber) })
            }
        }

        val twinIndexNumber = state.cardNumber?.indexNumber()
        tv_header.text = getString(R.string.twins_recreate_title_format, twinIndexNumber)
        tv_body.setText(R.string.onboarding_twins_interrupt_warning)

        btn_main_action.text = getString(R.string.twins_recreate_button_format, twinIndexNumber)
        btn_main_action.setOnClickListener {
            store.dispatch(TwinCardsAction.Wallet.LaunchFirstStep(
                Message(getString(R.string.twins_recreate_title_format, twinIndexNumber)),
                assetReader
            ))
        }

        btn_alternative_action.isVisible = false
        btn_alternative_action.isClickable = false
    }

    private fun setupCreateSecondWalletState(state: TwinCardsState) {
        switchToCard(state.cardNumber?.pairNumber())

        val twinPairIndexNumber = state.cardNumber?.pairIndexNumber()
        tv_header.text = getString(R.string.twins_recreate_title_format, twinPairIndexNumber)
        tv_body.setText(R.string.onboarding_twins_interrupt_warning)

        btn_main_action.text = getString(R.string.twins_recreate_button_format, twinPairIndexNumber)
        btn_main_action.setOnClickListener {
            store.dispatch(TwinCardsAction.Wallet.LaunchSecondStep(
                Message(getString(R.string.twins_recreate_title_format, twinPairIndexNumber)),
                Message(getString(R.string.twins_recreate_title_preparing)),
                Message(getString(R.string.twins_recreate_title_creating_wallet)),
            ))
        }
    }

    private fun setupCreateThirdWalletState(state: TwinCardsState) {
        switchToCard(state.cardNumber)

        val twinIndexNumber = state.cardNumber?.indexNumber()
        tv_header.text = getString(R.string.twins_recreate_title_format, twinIndexNumber)
        tv_body.setText(R.string.onboarding_twins_interrupt_warning)

        btn_main_action.text = getString(R.string.twins_recreate_button_format, twinIndexNumber)
        btn_main_action.setOnClickListener {
            store.dispatch(TwinCardsAction.Wallet.LaunchThirdStep(
                Message(getString(R.string.twins_recreate_title_format, twinIndexNumber))
            ))
        }
    }


    private fun setupTopUpWalletState(state: TwinCardsState) {
        when (previousStep) {
            TwinCardsStep.None -> {
                when (state.cardNumber) {
                    TwinCardNumber.First -> {
                        twinsWidget.leapfrogWidget.unfold(false) {
                            twinsWidget.toActivate(false) {
                                startPostponedEnterTransition()
                            }
                        }
                    }
                    TwinCardNumber.Second -> {
                        switchToCard(state.cardNumber, false) {
                            twinsWidget.toActivate(false)
                            startPostponedEnterTransition()
                        }
                    }
                }
            }
            TwinCardsStep.CreateThirdWallet -> {
                twinsWidget.toActivate()
            }
        }

        btn_main_action.setText(R.string.onboarding_top_up_button_but_crypto)
        btn_main_action.setOnClickListener {
            store.dispatch(TwinCardsAction.TopUp)
        }

        btn_alternative_action.isVisible = true
        btn_alternative_action.setText(R.string.onboarding_top_up_button_show_wallet_address)
        btn_alternative_action.setOnClickListener {
            store.dispatch(TwinCardsAction.ShowAddressInfoDialog)
        }

        tv_header.setText(R.string.onboarding_top_up_header)
        tv_body.setText(R.string.onboarding_top_up_body)

        btnRefreshBalanceWidget.changeState(state.walletBalance.state)
        if (btnRefreshBalanceWidget.isShowing != true) {
            postUi(300) {
                btnRefreshBalanceWidget.mainView.setOnClickListener {
                    store.dispatch(TwinCardsAction.Balance.Update)
                }
            }
        }

        imv_card_background.setBackgroundDrawable(requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8))
        updateConstraints(state.currentStep, R.layout.lp_onboarding_topup_wallet_twins)
    }

    private fun setupDoneState(state: TwinCardsState) {
        btn_main_action.setText(R.string.onboarding_done_button_continue)
        btn_main_action.setOnClickListener {
            store.dispatch(TwinCardsAction.Confetti.Hide)
            store.dispatch(TwinCardsAction.Done)
        }

        btn_alternative_action.isVisible = false
        btn_alternative_action.isClickable = false

        tv_header.setText(R.string.onboarding_done_header)
        tv_body.setText(R.string.onboarding_done_body)

        val layout = when (state.mode) {
            CreateTwinWalletMode.CreateWallet -> R.layout.lp_onboarding_done_activation_twins
            CreateTwinWalletMode.RecreateWallet -> R.layout.lp_onboarding_done
        }
        updateConstraints(state.currentStep, layout)
    }

    private fun updateConstraints(currentStep: TwinCardsStep, @LayoutRes layoutId: Int) {
        if (this.previousStep == currentStep) return

        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), layoutId)
        constraintSet.applyTo(onboarding_main_container)
        val transition = InternalNoteLayoutTransition()
        transition.interpolator = OvershootInterpolator()
        TransitionManager.beginDelayedTransition(onboarding_main_container, transition)
    }

    private fun switchToCard(cardNumber: TwinCardNumber?, animate: Boolean = true, onEnd: VoidCallback = {}) {
        val position = cardNumber?.number ?: return

        if (twinsWidget.leapfrogWidget.getViewPositionByIndex(1) != position - 1) {
            twinsWidget.leapfrogWidget.leap(animate, onEnd)
        } else {
            onEnd()
        }
    }

    override fun handleOnBackPressed() {
        store.dispatch(TwinCardsAction.Wallet.HandleOnBackPressed { should, popAction ->
            store.dispatch(TwinCardsAction.Confetti.Hide)
            showConfetti(false)
            if (should) switchToCard(TwinCardNumber.First, true, popAction)
            else popAction()
        })
    }
}

private fun TwinCardNumber?.indexNumber(): String {
    return this?.number?.toString() ?: ""
}

private fun TwinCardNumber?.pairIndexNumber(): String {
    return this?.pairNumber()?.number?.toString() ?: ""
}