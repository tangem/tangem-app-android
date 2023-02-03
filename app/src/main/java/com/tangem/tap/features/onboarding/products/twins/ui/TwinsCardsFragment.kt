package com.tangem.tap.features.onboarding.products.twins.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import coil.load
import com.tangem.Message
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.fragments.setStatusBarColor
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TwinCardNumber
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidget
import com.tangem.tap.common.AndroidAssetReader
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.extensions.stripZeroPlainString
import com.tangem.tap.common.redux.navigation.ShareElement
import com.tangem.tap.common.toggleWidget.RefreshBalanceWidget
import com.tangem.tap.common.transitions.InternalNoteLayoutTransition
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
import com.tangem.wallet.databinding.LayoutOnboardingContainerTopBinding

@Suppress("LargeClass")
class TwinsCardsFragment : BaseOnboardingFragment<TwinCardsState>() {

    private val mainBinding by lazy { binding.vMain }
    private var previousStep: TwinCardsStep = TwinCardsStep.None

    private lateinit var twinsWidget: TwinsCardWidget
    private lateinit var btnRefreshBalanceWidget: RefreshBalanceWidget

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    @Suppress("MagicNumber")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addBackPressHandler(this)

        reconfigureLayoutForTwins(mainBinding.onboardingTopContainer)

        val typedValue = TypedValue()
        resources.getValue(R.dimen.device_scale_factor_for_twins_welcome, typedValue, true)
        val deviceScaleFactorForWelcomeState = typedValue.float

        twinsWidget = TwinsCardWidget(
            LeapfrogWidget(mainBinding.onboardingTopContainer.cardsContainer),
            deviceScaleFactorForWelcomeState,
        ) {
            285f * deviceScaleFactorForWelcomeState
        }
        btnRefreshBalanceWidget =
            RefreshBalanceWidget(mainBinding.onboardingTopContainer.onboardingWalletContainer)

        binding.toolbar.title = getText(R.string.twins_recreate_toolbar)

        mainBinding.onboardingTopContainer.imvTwinFrontCard.load(Artwork.TWIN_CARD_1) {
            placeholder(R.drawable.card_placeholder_black)
            error(R.drawable.card_placeholder_black)
            fallback(R.drawable.card_placeholder_black)
        }

        mainBinding.onboardingTopContainer.imvTwinBackCard.load(Artwork.TWIN_CARD_2) {
            placeholder(R.drawable.card_placeholder_white)
            error(R.drawable.card_placeholder_white)
            fallback(R.drawable.card_placeholder_white)
        }
    }

    override fun onStart() {
        super.onStart()
        setStatusBarColor(R.color.backgroundWhite)
    }

    private fun reconfigureLayoutForTwins(containerBinding: LayoutOnboardingContainerTopBinding) =
        with(containerBinding) {
            imvFrontCard.hide()
            imvCardBackground.hide()
            cardsContainer.show()

            imvTwinFrontCard.transitionName = ShareElement.imvFrontCard
            imvTwinBackCard.transitionName = ShareElement.imvBackCard

            // if don't this, the bg_circle_... is overflow the app_bar
            binding.appBar.bringToFront()
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
        if (activity == null || view == null) return

        pbBinding.pbState.max = state.steps.size - 1
        pbBinding.pbState.progress = state.progress

        when (state.currentStep) {
            is TwinCardsStep.WelcomeOnly -> setupWelcomeOnlyState(state, state.currentStep.scanResponse)
            is TwinCardsStep.Welcome -> setupWelcomeState(state)
            is TwinCardsStep.Warning -> setupWarningState(state)
            is TwinCardsStep.CreateFirstWallet -> setupCreateFirstWalletState(state)
            is TwinCardsStep.CreateSecondWallet -> setupCreateSecondWalletState(state)
            is TwinCardsStep.CreateThirdWallet -> setupCreateThirdWalletState(state)
            is TwinCardsStep.TopUpWallet -> setupTopUpWalletState(state)
            is TwinCardsStep.Done -> setupDoneState(state)
            else -> {}
        }

        setBalance(state)
        showConfetti(state.showConfetti)
        previousStep = state.currentStep
    }

    private fun setBalance(state: TwinCardsState) {
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

    private fun setupWelcomeOnlyState(state: TwinCardsState, scanResponse: ScanResponse) {
        setupWelcomeState(state) {
            store.dispatch(TwinCardsAction.SaveScannedTwinCardAndNavigateToWallet(scanResponse))
            store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.None))
        }
    }

    private fun setupWelcomeState(state: TwinCardsState) {
        setupWelcomeState(state) { store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.CreateFirstWallet)) }
    }

    private fun setupWelcomeState(state: TwinCardsState, mainAction: VoidCallback) =
        with(mainBinding.onboardingActionContainer) {
            twinsWidget.toWelcome(false)

            mainBinding.onboardingTopContainer.onboardingTwinsWelcomeBg.root.show()
            pbBinding.pbState.hide()

            tvHeader.setText(R.string.twins_onboarding_subtitle)
            tvBody.text = getString(
                R.string.twins_onboarding_description_format,
                state.cardNumber?.pairIndexNumber(),
            )

            btnMainAction.setText(R.string.common_continue)
            btnMainAction.setOnClickListener { mainAction() }
        }

    private fun setupWarningState(state: TwinCardsState) = with(mainBinding.onboardingActionContainer) {
        twinsWidget.toWelcome(false)

        mainBinding.onboardingTopContainer.onboardingTwinsWelcomeBg.root.hide()
        pbBinding.pbState.hide()
        chbUnderstand.show()

        tvHeader.setText(R.string.common_warning)
        tvBody.setText(R.string.twins_recreate_warning)

        chbUnderstand.setOnCheckedChangeListener { buttonView, isChecked ->
            store.dispatch(TwinCardsAction.SetUserUnderstand(isChecked))
        }
        btnMainAction.isEnabled = state.userWasUnderstandIfWalletRecreate
        btnMainAction.setText(R.string.common_continue)
        btnMainAction.setOnClickListener {
            store.dispatch(TwinCardsAction.SetStepOfScreen(TwinCardsStep.CreateFirstWallet))
        }
    }

    private fun setupCreateFirstWalletState(state: TwinCardsState) = with(mainBinding.onboardingActionContainer) {
        mainBinding.onboardingTopContainer.onboardingTwinsWelcomeBg.root.hide()
        mainBinding.onboardingTopContainer.onboardingTwinsWelcomeBg.bgCircleLarge.hide()
        mainBinding.onboardingTopContainer.onboardingTwinsWelcomeBg.bgCircleMedium.hide()
        mainBinding.onboardingTopContainer.onboardingTwinsWelcomeBg.bgCircleMin.hide()
        chbUnderstand.hide()
        pbBinding.pbState.show()

        mainBinding.onboardingTopContainer.onboardingWalletContainer.beginDelayedTransition()

        when (previousStep) {
            TwinCardsStep.None -> {
                twinsWidget.toLeapfrog(false) {
                    when (state.cardNumber) {
                        TwinCardNumber.First -> {}
                        TwinCardNumber.Second -> {
                            switchToCard(
                                state.cardNumber,
                                false,
                            )
                        }
                        else -> {}
                    }
                }
            }
            TwinCardsStep.Welcome, TwinCardsStep.Warning -> {
                twinsWidget.toLeapfrog(onEnd = { switchToCard(state.cardNumber) })
            }
            else -> {}
        }

        val twinIndexNumber = state.cardNumber?.indexNumber()
        tvHeader.text = getString(R.string.twins_recreate_title_format, twinIndexNumber)
        tvBody.setText(R.string.onboarding_twins_interrupt_warning)

        btnMainAction.text = getString(R.string.twins_recreate_button_format, twinIndexNumber)
        btnMainAction.setOnClickListener {
            Analytics.send(Onboarding.CreateWallet.ButtonCreateWallet())
            store.dispatch(
                TwinCardsAction.Wallet.LaunchFirstStep(
                    Message(getString(R.string.twins_recreate_title_format, twinIndexNumber)),
                    AndroidAssetReader(requireContext()),
                ),
            )
        }

        btnAlternativeAction.isVisible = false
        btnAlternativeAction.isClickable = false
    }

    private fun setupCreateSecondWalletState(state: TwinCardsState) = with(mainBinding.onboardingActionContainer) {
        switchToCard(state.cardNumber?.pairNumber())

        val twinPairIndexNumber = state.cardNumber?.pairIndexNumber()
        tvHeader.text = getString(R.string.twins_recreate_title_format, twinPairIndexNumber)
        tvBody.setText(R.string.onboarding_twins_interrupt_warning)

        btnMainAction.text =
            getString(R.string.twins_recreate_button_format, twinPairIndexNumber)
        btnMainAction.setOnClickListener {
            store.dispatch(
                TwinCardsAction.Wallet.LaunchSecondStep(
                    Message(
                        getString(
                            R.string.twins_recreate_title_format,
                            twinPairIndexNumber,
                        ),
                    ),
                    Message(getString(R.string.twins_recreate_title_preparing)),
                    Message(getString(R.string.twins_recreate_title_creating_wallet)),
                ),
            )
        }
    }

    private fun setupCreateThirdWalletState(state: TwinCardsState) = with(mainBinding.onboardingActionContainer) {
        switchToCard(state.cardNumber)

        val twinIndexNumber = state.cardNumber?.indexNumber()
        tvHeader.text = getString(R.string.twins_recreate_title_format, twinIndexNumber)
        tvBody.setText(R.string.onboarding_twins_interrupt_warning)

        btnMainAction.text = getString(R.string.twins_recreate_button_format, twinIndexNumber)
        btnMainAction.setOnClickListener {
            store.dispatch(
                TwinCardsAction.Wallet.LaunchThirdStep(
                    Message(getString(R.string.twins_recreate_title_format, twinIndexNumber)),
                ),
            )
        }
    }

    private fun setupTopUpWalletState(state: TwinCardsState) = with(mainBinding.onboardingActionContainer) {
        when (previousStep) {
            TwinCardsStep.None -> {
                when (state.cardNumber) {
                    TwinCardNumber.First -> {
                        twinsWidget.leapfrogWidget.unfold(false) {
                            twinsWidget.toActivate(false)
                        }
                    }
                    TwinCardNumber.Second -> {
                        switchToCard(state.cardNumber, false) {
                            twinsWidget.toActivate(false)
                        }
                    }
                    else -> {}
                }
            }
            TwinCardsStep.CreateThirdWallet -> {
                twinsWidget.toActivate()
            }
            else -> {}
        }

        if (state.isBuyAllowed) {
            btnMainAction.setText(R.string.onboarding_top_up_button_but_crypto)
            btnMainAction.setOnClickListener {
                store.dispatch(TwinCardsAction.TopUp)
            }

            btnAlternativeAction.isVisible = true
            btnAlternativeAction.setText(R.string.onboarding_top_up_button_show_wallet_address)
            btnAlternativeAction.setOnClickListener {
                store.dispatch(TwinCardsAction.ShowAddressInfoDialog)
            }
        } else {
            btnMainAction.setText(R.string.onboarding_button_receive_crypto)
            btnMainAction.setOnClickListener {
                store.dispatch(TwinCardsAction.ShowAddressInfoDialog)
            }

            btnAlternativeAction.isVisible = false
        }

        tvHeader.setText(R.string.onboarding_top_up_header)
        tvBody.setText(R.string.onboarding_top_up_body)

        btnRefreshBalanceWidget.changeState(state.walletBalance.state)
        if (btnRefreshBalanceWidget.isShowing != true) {
            btnRefreshBalanceWidget.mainView.setOnClickListener {
                store.dispatch(TwinCardsAction.Balance.Update)
            }
        }
        mainBinding.onboardingTopContainer.imvCardBackground.setBackgroundDrawable(
            requireContext().getDrawableCompat(R.drawable.shape_rectangle_rounded_8),
        )
        updateConstraints(state.currentStep, R.layout.lp_onboarding_topup_wallet_twins)
    }

    private fun setupDoneState(state: TwinCardsState) = with(mainBinding.onboardingActionContainer) {
        btnMainAction.setText(R.string.common_continue)
        btnMainAction.setOnClickListener {
            store.dispatch(TwinCardsAction.Confetti.Hide)
            store.dispatch(TwinCardsAction.Done)
        }

        btnAlternativeAction.isVisible = false
        btnAlternativeAction.isClickable = false

        tvHeader.setText(R.string.onboarding_done_header)
        tvBody.setText(R.string.onboarding_done_body)

        val layout = when (state.mode) {
            CreateTwinWalletMode.CreateWallet -> R.layout.lp_onboarding_done_activation_twins
            CreateTwinWalletMode.RecreateWallet -> R.layout.lp_onboarding_done
        }
        updateConstraints(state.currentStep, layout)
    }

    private fun updateConstraints(currentStep: TwinCardsStep, @LayoutRes layoutId: Int) {
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

    private fun switchToCard(cardNumber: TwinCardNumber?, animate: Boolean = true, onEnd: VoidCallback = {}) {
        val position = cardNumber?.number ?: return

        if (twinsWidget.leapfrogWidget.getViewPositionByIndex(1) != position - 1) {
            twinsWidget.leapfrogWidget.leap(animate, onEnd)
        } else {
            onEnd()
        }
    }

    override fun handleOnBackPressed() {
        store.dispatch(
            TwinCardsAction.OnBackPressed { should, popAction ->
                store.dispatch(TwinCardsAction.Confetti.Hide)
                showConfetti(false)
                if (should) switchToCard(TwinCardNumber.First, true, popAction) else popAction()
            },
        )
    }
}

private fun TwinCardNumber?.indexNumber(): String {
    return this?.number?.toString() ?: ""
}

private fun TwinCardNumber?.pairIndexNumber(): String {
    return this?.pairNumber()?.number?.toString() ?: ""
}
