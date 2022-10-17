package com.tangem.tap.features.onboarding.products.wallet.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import by.kirich1409.viewbindingdelegate.viewBinding
import coil.load
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayoutMediator
import com.tangem.common.CardIdFormatter
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.extensions.guard
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidget
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.PropertyCalculator
import com.tangem.tap.common.compose.PinCodeWidget
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.configureSettings
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.extensions.stop
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.toggleWidget.IndeterminateProgressButtonWidget
import com.tangem.tap.features.FragmentOnBackPressedHandler
import com.tangem.tap.features.addBackPressHandler
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupState
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupStep
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletAction
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletState
import com.tangem.tap.features.onboarding.products.wallet.redux.OnboardingWalletStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.UtorgWebChromeClient
import com.tangem.tap.features.onboarding.products.wallet.saltPay.UtorgWebViewClient
import com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog.SaltPayDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayState
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.SaltPayRegistrationStep
import com.tangem.tap.features.onboarding.products.wallet.ui.dialogs.AccessCodeDialog
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.FragmentOnboardingWalletBinding
import com.tangem.wallet.databinding.LayoutOnboardingSaltpayBinding
import com.tangem.wallet.databinding.ViewOnboardingProgressBinding
import org.rekotlin.StoreSubscriber

class OnboardingWalletFragment : Fragment(R.layout.fragment_onboarding_wallet),
    StoreSubscriber<OnboardingWalletState>, FragmentOnBackPressedHandler {

    private val pbBinding: ViewOnboardingProgressBinding by viewBinding(ViewOnboardingProgressBinding::bind)
    private val binding: FragmentOnboardingWalletBinding by viewBinding(FragmentOnboardingWalletBinding::bind)
    private val bindingSaltPay: LayoutOnboardingSaltpayBinding by lazy { binding.onboardingSaltpayContainer }

    private val saltPayView: SaltPayView = SaltPayView(this)

    private lateinit var cardsWidget: WalletCardsWidget
    private var accessCodeDialog: AccessCodeDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
        setHasOptionsMenu(true)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade)
        exitTransition = inflater.inflateTransition(R.transition.fade)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val typedValue = TypedValue()
        resources.getValue(R.dimen.device_scale_factor_for_twins_welcome, typedValue, true)
        val deviceScaleFactor = typedValue.float

        val leapfrogCalculator = PropertyCalculator(
            yTranslationFactor = 25f * deviceScaleFactor,
        )
        val leapfrog = LeapfrogWidget(binding.flCardsContainer, leapfrogCalculator)
        cardsWidget = WalletCardsWidget(leapfrog, deviceScaleFactor) { 200f * deviceScaleFactor }
        startPostponedEnterTransition()

        binding.viewPagerBackupInfo.adapter = BackupInfoAdapter()
        TabLayoutMediator(
            binding.tabLayoutBackupInfo,
            binding.viewPagerBackupInfo,
        ) { tab, position ->
            //Some implementation
        }.attach()

        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)

        binding.toolbar.setNavigationOnClickListener { activity?.onBackPressed() }
        addBackPressHandler(this)

        store.dispatch(OnboardingWalletAction.Init)
        store.dispatch(OnboardingWalletAction.StartSaltPayCardActivation)
        store.dispatch(OnboardingWalletAction.LoadArtwork)
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.onboardingWalletState == newState.onboardingWalletState
            }.select { it.onboardingWalletState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: OnboardingWalletState) {
        if (activity == null || view == null) return

        requireActivity().invalidateOptionsMenu()

        pbBinding.pbState.max = state.getMaxProgress()
        pbBinding.pbState.progress = state.getProgressStep()

        if (state.isSaltPay) {
            saltPayView.newState(state)
        } else {
            loadImageIntoImageView(state.cardArtworkUrl, binding.imvFrontCard)
            loadImageIntoImageView(state.cardArtworkUrl, binding.imvFirstBackupCard)
            loadImageIntoImageView(state.cardArtworkUrl, binding.imvSecondBackupCard)
            handleOnboardingStep(state)
        }
    }

    private fun loadImageIntoImageView(url: String?, view: ImageView) {
        view.load(url) {
            placeholder(R.drawable.card_placeholder_black)
            error(R.drawable.card_placeholder_black)
            fallback(R.drawable.card_placeholder_black)
        }
    }

    private fun handleOnboardingStep(state: OnboardingWalletState) {
        when (state.step) {
            OnboardingWalletStep.CreateWallet -> setupCreateWalletState()
            OnboardingWalletStep.Backup -> setBackupState(state.backupState, state.isSaltPay)
            else -> {}
        }
    }

    private fun setupCreateWalletState() = with(binding) {
        layoutButtonsCommon.btnMainAction.setText(R.string.onboarding_create_wallet_button_create_wallet)
        layoutButtonsCommon.btnMainAction.setOnClickListener { store.dispatch(OnboardingWalletAction.CreateWallet) }
        layoutButtonsCommon.btnAlternativeAction.hide()

        toolbar.title = getText(R.string.onboarding_getting_started)

        tvHeader.setText(R.string.onboarding_create_wallet_header)
        tvBody.setText(R.string.onboarding_create_wallet_body)

        cardsWidget.toFolded(false) { startPostponedEnterTransition() }
    }

    private fun setBackupState(state: BackupState, isSaltPay: Boolean) {
        when (state.backupStep) {
            BackupStep.InitBackup -> showBackupIntro(state, isSaltPay)
            BackupStep.ScanOriginCard -> showScanOriginCard()
            BackupStep.AddBackupCards -> showAddBackupCards(state, isSaltPay)
            BackupStep.SetAccessCode -> showSetAccessCode()
            BackupStep.EnterAccessCode -> showEnterAccessCode(state)
            BackupStep.ReenterAccessCode -> showReenterAccessCode(state)
            is BackupStep.WritePrimaryCard -> showWritePrimaryCard(state, isSaltPay)
            is BackupStep.WriteBackupCard -> showWriteBackupCard(state, isSaltPay)
            BackupStep.Finished -> showSuccess()
        }
    }

    private fun showBackupIntro(state: BackupState, isSaltPay: Boolean) = with(binding) {
        imvFirstBackupCard.show()
        imvSecondBackupCard.show()

        cardsWidget.toWelcome()

        tvHeader.hide()
        tvBody.hide()
        viewPagerBackupInfo.show()
        tabLayoutBackupInfo.show()

        with(layoutButtonsCommon) {
            btnMainAction.text = getText(R.string.onboarding_button_backup_now)
            btnMainAction.setOnClickListener { store.dispatch(BackupAction.StartBackup) }

            btnAlternativeAction.text = getText(R.string.onboarding_button_skip_backup)
            btnAlternativeAction.setOnClickListener { store.dispatch(BackupAction.DismissBackup) }
            btnAlternativeAction.show(state.canSkipBackup && !isSaltPay)
        }

        startPostponedEnterTransition()
    }

    private fun showScanOriginCard() = with(binding) {
        prepareBackupView()

        cardsWidget.toFolded()

        tvHeader.text = getText(R.string.onboarding_title_scan_origin_card)
        tvBody.text = getString(
            R.string.onboarding_subtitle_scan_origin_card,
        )

        with(layoutButtonsCommon) {
            btnMainAction.text = getString(R.string.onboarding_button_scan_origin_card)
            btnAlternativeAction.hide()
            btnMainAction.setOnClickListener { store.dispatch(BackupAction.ScanPrimaryCard) }
        }
    }

    private fun prepareBackupView() = with(binding) {
        toolbar.title = getText(R.string.onboarding_navbar_title_creating_backup)

        tvHeader.show()
        tvBody.show()
        viewPagerBackupInfo.hide()
        tabLayoutBackupInfo.hide()

        imvFirstBackupCard.show()
        imvSecondBackupCard.show()
    }

    private fun showAddBackupCards(state: BackupState, isSaltPay: Boolean) = with(binding) {
        prepareBackupView()

        accessCodeDialog?.dismiss()
        accessCodeDialog = null

        layoutButtonsAddCards.root.show()
        layoutButtonsCommon.root.hide()
        layoutButtonsAddCards.btnAddCard.text = getText(R.string.onboarding_button_add_backup_card)
        if (state.backupCardsNumber < state.maxBackupCards) {
            layoutButtonsAddCards.btnAddCard.setOnClickListener { store.dispatch(BackupAction.AddBackupCard) }
        } else {
            layoutButtonsAddCards.btnAddCard.isEnabled = false
        }

        layoutButtonsAddCards.btnContinue.text = getText(R.string.onboarding_button_finalize_backup)
        layoutButtonsAddCards.btnContinue.setOnClickListener { store.dispatch(BackupAction.FinishAddingBackupCards) }
        layoutButtonsAddCards.btnContinue.isEnabled = state.backupCardsNumber != 0

        if (isSaltPay) {
            saltPayView.showAddBackupCards(state)
            return@with
        }

        when (state.backupCardsNumber) {
            0 -> {
                cardsWidget.toFan {
                    cardsWidget.getFirstBackupCardView().animate().alpha(0.6f).duration = 200
                    cardsWidget.getSecondBackupCardView().animate().alpha(0.2f).duration = 200
                }
                tvHeader.text = getText(R.string.onboarding_title_no_backup_cards)
                tvBody.text = getText(R.string.onboarding_subtitle_no_backup_cards)
            }
            1 -> {
                tvHeader.text = getText(R.string.onboarding_title_one_backup_card)
                tvBody.text = getText(R.string.onboarding_subtitle_one_backup_card)

                cardsWidget.toFan(false)
                cardsWidget.getFirstBackupCardView().animate().alpha(1f).duration = 400
                cardsWidget.getSecondBackupCardView().alpha = 0.2f
            }
            2 -> {
                tvHeader.text = getText(R.string.onboarding_title_two_backup_cards)
                tvBody.text = getText(R.string.onboarding_subtitle_two_backup_cards)

                cardsWidget.toFan(false)
                cardsWidget.getFirstBackupCardView().alpha = 1f
                cardsWidget.getSecondBackupCardView().animate().alpha(1f).duration = 400
            }
        }
    }

    private fun showSetAccessCode() {
        accessCodeDialog = AccessCodeDialog(requireContext()).apply {
            dismissWithAnimation = true
            create()
            setOnCancelListener {
                store.dispatch(BackupAction.OnAccessCodeDialogClosed)
            }
            show()
            showInfoScreen()
            val view =
                delegate.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            (view?.parent as? ViewGroup)?.let { TransitionManager.beginDelayedTransition(it) }
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            view?.let { it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT }
        }
    }

    private fun showEnterAccessCode(state: BackupState) {
        accessCodeDialog?.showEnterAccessCode()
        accessCodeDialog?.showError(state.accessCodeError)
    }

    private fun showReenterAccessCode(state: BackupState) {
        accessCodeDialog?.showReenterAccessCode()
        accessCodeDialog?.showError(state.accessCodeError)
    }

    private fun showWritePrimaryCard(state: BackupState, isSaltPay: Boolean) = with(binding) {
        accessCodeDialog?.dismiss()

        prepareViewForFinalizeStep()
        cardsWidget.getSecondBackupCardView().show(state.backupCardsNumber == 2)

        cardsWidget.toLeapfrog {
            cardsWidget.getFirstBackupCardView().alpha = 0.6f
            cardsWidget.getSecondBackupCardView().alpha = 0.2f
        }

        val cardIdFormatter = CardIdFormatter(CardIdDisplayFormat.LastMasked(4))
        if (isSaltPay) {
            tvHeader.text = getText(R.string.onboarding_saltpay_title_prepare_origin)
            tvBody.text = getString(R.string.onboarding_twins_interrupt_warning)
            layoutButtonsCommon.btnMainAction.text = getText(R.string.onboarding_saltpay_button_backup_origin)
        } else {
            tvHeader.text = getText(R.string.onboarding_title_prepare_origin)
            tvBody.text = getString(
                R.string.onboarding_subtitle_scan_primary_card_format,
                state.primaryCardId?.let { cardIdFormatter.getFormattedCardId(it) },
            )
            layoutButtonsCommon.btnMainAction.text = getText(R.string.onboarding_button_backup_origin)
        }

        layoutButtonsCommon.btnMainAction.setOnClickListener { store.dispatch(BackupAction.WritePrimaryCard) }

    }

    private fun prepareViewForFinalizeStep() = with(binding) {
        layoutButtonsAddCards.root.hide()
        layoutButtonsCommon.root.show()

        imvFirstBackupCard.show()
        imvSecondBackupCard.show()

        toolbar.title = getText(R.string.onboarding_button_finalize_backup)

        imvCardBackground.hide()

        cardsWidget.toLeapfrog()

        layoutButtonsCommon.btnAlternativeAction.hide()
    }

    private fun showWriteBackupCard(state: BackupState, isSaltPay: Boolean) = with(binding) {
        prepareViewForFinalizeStep()

        cardsWidget.getSecondBackupCardView().show(state.backupCardsNumber == 2)

        val cardNumber = (state.backupStep as? BackupStep.WriteBackupCard)?.cardNumber ?: 1
        when (cardNumber) {
            1 -> cardsWidget.leapfrogWidget.leap {
                cardsWidget.getOriginCardView().alpha = 0.4f
                cardsWidget.getFirstBackupCardView().alpha = 0.2f
                cardsWidget.getSecondBackupCardView().alpha = 1f
            }
            2 -> cardsWidget.leapfrogWidget.leap {
                cardsWidget.getOriginCardView().alpha = 0.4f
                cardsWidget.getFirstBackupCardView().alpha = 0.2f
                cardsWidget.getSecondBackupCardView().alpha = 1f
            }
        }

        val cardIdFormatter = CardIdFormatter(CardIdDisplayFormat.LastMasked(4))
        if (isSaltPay) {
            tvHeader.text = getString(R.string.onboarding_saltpay_title_backup_card)
            tvBody.text = getString(R.string.onboarding_twins_interrupt_warning)
        } else {
            tvHeader.text = getString(R.string.onboarding_title_backup_card_format, cardNumber)
            tvBody.text = getString(
                R.string.onboarding_subtitle_scan_backup_card_format,
                cardIdFormatter.getFormattedCardId(state.backupCardIds[cardNumber - 1]),
            )
        }
        layoutButtonsCommon.btnMainAction.text = getString(
            R.string.onboarding_button_backup_card_format,
            cardNumber,
        )
        layoutButtonsCommon.btnMainAction.setOnClickListener {
            store.dispatch(BackupAction.WriteBackupCard(cardNumber))
        }
    }

    private fun showSuccess() = with(binding) {
        tvHeader.text = getText(R.string.onboarding_done_header)

        tvHeader.show()
        tvBody.show()
        viewPagerBackupInfo.hide()
        tabLayoutBackupInfo.hide()

        tvBody.text = getText(R.string.onboarding_subtitle_success_tangem_wallet_onboarding)
        layoutButtonsCommon.btnMainAction.text = getText(R.string.onboarding_button_continue_wallet)
        layoutButtonsCommon.btnAlternativeAction.hide()
        layoutButtonsCommon.btnMainAction.setOnClickListener {
            vConfetti.lavConfetti.cancelAnimation()
            vConfetti.lavConfetti.hide()
            store.dispatch(OnboardingWalletAction.FinishOnboarding)
        }

        cardsWidget.leapfrogWidget.fold {
            flCardsContainer.hide()
            imvCardBackground.hide()
            vConfetti.lavConfetti.show()
            vConfetti.lavConfetti.playAnimation()
            imvSuccess.alpha = 0f
            imvSuccess.show()

            imvSuccess.animate()
                ?.alpha(1f)?.duration = 400
        }
    }

    override fun handleOnBackPressed() {
        store.dispatch(OnboardingWalletAction.OnBackPressed)
    }

    private class SaltPayView(
        private val walletFragment: OnboardingWalletFragment,
    ) {

        private val toolbar: MaterialToolbar by lazy { walletFragment.binding.toolbar }

        private var progressButton: SaltPayProgressButton? = null

        fun newState(state: OnboardingWalletState) {
            handleCardArtworks(state)
            when (state.step) {
                OnboardingWalletStep.SaltPay -> setSaltPayStep(state.onboardingSaltPayState)
                OnboardingWalletStep.Backup -> {
                    if (state.backupState.backupStep == BackupStep.Finished) {
                        // if backup finished -> switch OnboardingWalletStep to the SaltPay
                        store.dispatch(OnboardingWalletAction.GetToSaltPayStep)
                    } else {
                        // if not -> back to the standard backup process
                        walletFragment.handleOnboardingStep(state)
                    }
                }
                else -> walletFragment.handleOnboardingStep(state)
            }
        }

        private fun handleCardArtworks(state: OnboardingWalletState) = with(walletFragment.binding) {
            if (state.onboardingSaltPayState?.saltPayCardArtworkUrl == null) {
                // if saltPay url not loaded -> load from resource
                val bitmap = BitmapFactory.decodeResource(walletFragment.resources, R.drawable.img_salt_pay_visa)
                imvFrontCard.setImageBitmap(bitmap)
            } else {
                walletFragment.loadImageIntoImageView(state.onboardingSaltPayState.saltPayCardArtworkUrl, imvFrontCard)
            }
            walletFragment.loadImageIntoImageView(state.cardArtworkUrl, imvFirstBackupCard)

            //TODO: at now we can hide the image only by changing alpha channel to 0, because
            // the OnboardingWalletFragment and WalletCardsWidget manipulate it visibility through changing
            // View.VISIBILITY states.
            imvSecondBackupCard.alpha = 0f
        }

        fun showAddBackupCards(state: BackupState) = with(walletFragment.binding) {
            walletFragment.cardsWidget.getSecondBackupCardView().alpha = 0f
            when (state.backupCardsNumber) {
                0 -> {
                    walletFragment.cardsWidget.toFan {
                        walletFragment.cardsWidget.getFirstBackupCardView().animate().alpha(0.6f).duration = 200
                    }
                    tvHeader.text = walletFragment.getText(R.string.onboarding_saltpay_title_no_backup_card)
                    tvBody.text = walletFragment.getText(R.string.onboarding_saltpay_subtitle_no_backup_cards)
                }
                1 -> {
                    tvHeader.text = walletFragment.getText(R.string.onboarding_saltpay_title_one_backup_card)
                    tvBody.text = walletFragment.getText(R.string.onboarding_saltpay_subtitle_one_backup_card)
                    walletFragment.cardsWidget.toFan(false)
                    walletFragment.cardsWidget.getFirstBackupCardView().animate().alpha(1f).duration = 400
                }
            }
        }

        private fun setSaltPayStep(onboardingSaltPayState: OnboardingSaltPayState?) {
            val state = onboardingSaltPayState.guard {
                store.dispatchDebugErrorNotification("SaltPayView: setSaltPayStep: onboardingSaltPayState = null")
                return
            }

            walletFragment.binding.onboardingWalletContainer.hide()
            walletFragment.bindingSaltPay.onboardingSaltpayContainer.show()

            when (state.step) {
                SaltPayRegistrationStep.NoGas -> handleNoGas()
                SaltPayRegistrationStep.NeedPin -> handleNeedPin()
                SaltPayRegistrationStep.CardRegistration -> handleCardRegistration()
                SaltPayRegistrationStep.KycIntro -> handleKycIntro()
                SaltPayRegistrationStep.KycStart -> handleKycStart(state)
                SaltPayRegistrationStep.KycWaiting -> handleKycWaiting()
                SaltPayRegistrationStep.Finished -> handleFinished()
            }
            progressButton?.changeState(state.mainButtonState)
        }

        private fun handleNoGas() {
            // normally this shouldn't happen
            store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
            store.dispatchDialogShow(SaltPayDialog.NoFundsForActivation)
        }

        private fun handleNeedPin() = with(walletFragment.bindingSaltPay) {
            toolbar.title = walletFragment.getString(R.string.onboarding_navbar_pin)
            showOnlyView(pinCode.root) {
                progressButton = SaltPayProgressButton(pinCode.root)
                progressButton?.mainView?.isEnabled = false

                var enteredPinCode = ""
                pinCode.flPinCodeContainer.setContent {
                    PinCodeWidget { code, isLastSymbolEntered ->
                        enteredPinCode = code
                        progressButton?.mainView?.isEnabled = isLastSymbolEntered
                    }
                }
                pinCode.btnSetPin.setOnClickListener {
                    store.dispatch(OnboardingSaltPayAction.TrySetPin(enteredPinCode))
                }
            }
        }

        private fun handleCardRegistration() = with(walletFragment.bindingSaltPay) {
            toolbar.title = walletFragment.getString(R.string.onboarding_navbar_register_wallet)
            showOnlyView(connectCard.root) {
                progressButton = SaltPayProgressButton(connectCard.root)
                connectCard.btnConnect.setOnClickListener {
                    store.dispatch(OnboardingSaltPayAction.RegisterCard)
                }
            }
        }

        private fun handleKycIntro() = with(walletFragment.bindingSaltPay) {
            toolbar.title = walletFragment.getString(R.string.onboarding_navbar_kyc_start)
            showOnlyView(verifyIdentity.root) {
                progressButton = SaltPayProgressButton(verifyIdentity.root)
                verifyIdentity.btnVerify.setOnClickListener {
                    store.dispatch(OnboardingSaltPayAction.KYCStart)
                }
            }
        }

        private fun handleKycStart(state: OnboardingSaltPayState) = with(walletFragment.bindingSaltPay) {
            toolbar.title = walletFragment.getString(R.string.onboarding_navbar_kyc_start)
            showOnlyView(webvVerifyIdentity) {
                progressButton = null
                webvVerifyIdentity.configureSettings()
                webvVerifyIdentity.webViewClient = UtorgWebViewClient(
                    successUrl = state.saltPayManager.kycUrlProvider.doneUrl,
                    onSuccess = {
                        webvVerifyIdentity.stop()
                        store.dispatch(OnboardingSaltPayAction.OnFinishKYC)
                    },
                )
                webvVerifyIdentity.webChromeClient = UtorgWebChromeClient(walletFragment.requireActivity())
                webvVerifyIdentity.loadUrl(state.saltPayManager.kycUrlProvider.requestUrl)
            }
        }

        private fun handleKycWaiting() = with(walletFragment.bindingSaltPay) {
            toolbar.title = walletFragment.getString(R.string.onboarding_navbar_kyc_waiting)
            showOnlyView(verifyIdentityInProgress.root) {
                progressButton = SaltPayProgressButton(verifyIdentityInProgress.root)
                verifyIdentityInProgress.btnOpenSupportChat.setOnClickListener {
                    store.dispatch(GlobalAction.OpenChat(SupportInfo()))
                }
                verifyIdentityInProgress.btnRefresh.setOnClickListener {
                    store.dispatch(OnboardingSaltPayAction.Update)
                }
            }
        }

        private fun handleFinished() {
            walletFragment.binding.onboardingRoot.beginDelayedTransition()
            walletFragment.bindingSaltPay.onboardingSaltpayContainer.hide()
            walletFragment.binding.onboardingWalletContainer.show()
            walletFragment.showSuccess()
        }

        private fun showOnlyView(view: View, onShowListener: (() -> Unit)? = null) {
            // onboardingSaltpayContainer.beginDelayedTransition()
            walletFragment.bindingSaltPay.onboardingSaltpayContainer.children
                .filter { it.id != view.id }
                .forEach { it.hide() }
            view.show { onShowListener?.invoke() }
        }

        private class SaltPayProgressButton(
            root: ViewGroup,
        ) : IndeterminateProgressButtonWidget(findButton(root), findProgress(root)) {
            companion object {
                fun findButton(view: ViewGroup): MaterialButton {
                    return findContainer(view).children.firstOrNull { it is MaterialButton } as MaterialButton
                }

                fun findProgress(view: ViewGroup): View {
                    return findContainer(view).children.firstOrNull { it is ProgressBar }!!
                }

                fun findContainer(view: ViewGroup): ViewGroup {
                    return view.children.first { it.id == R.id.btn_container } as ViewGroup
                }
            }
        }
    }
}