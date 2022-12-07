package com.tangem.tap.features.onboarding.products.wallet.saltPay.ui

import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ProgressBar
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.children
import androidx.transition.TransitionManager
import coil.load
import coil.size.Scale
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.tangem.common.extensions.guard
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.Onboarding
import com.tangem.tap.common.compose.PinCodeWidget
import com.tangem.tap.common.extensions.configureSettings
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.getDrawableCompat
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.setDrawable
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.extensions.stop
import com.tangem.tap.common.extensions.toFormattedCurrencyString
import com.tangem.tap.common.feedback.SupportInfo
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.toggleWidget.IndeterminateProgressButtonWidget
import com.tangem.tap.common.toggleWidget.RefreshBalanceWidget
import com.tangem.tap.common.transitions.InternalNoteLayoutTransition
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
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.SaltPayActivationStep
import com.tangem.tap.features.onboarding.products.wallet.ui.OnboardingWalletFragment
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutOnboardingContainerBottomBinding
import com.tangem.wallet.databinding.LayoutOnboardingContainerTopBinding
import com.tangem.wallet.databinding.LayoutOnboardingMainBinding

/**
 * Created by Anton Zhilenkov on 20.10.2022.
 */
internal class OnboardingSaltPayView(
    private val walletFragment: OnboardingWalletFragment,
) {

    private val toolbar: MaterialToolbar by lazy { walletFragment.binding.toolbar }
    private val claimBinding: LayoutOnboardingMainBinding by lazy { walletFragment.bindingSaltPay.claim }
    private val topContainer: LayoutOnboardingContainerTopBinding by lazy { claimBinding.onboardingTopContainer }
    private val actionContainer: LayoutOnboardingContainerBottomBinding by lazy { claimBinding.onboardingActionContainer }

    private val btnRefreshBalanceWidget by lazy {
        RefreshBalanceWidget(claimBinding.onboardingTopContainer.onboardingWalletContainer)
    }

    private var progressButton: SaltPayProgressButton? = null
    private var previousStep: SaltPayActivationStep = SaltPayActivationStep.None

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
        //TODO: SaltPay: remove hardCode
        if (state.onboardingSaltPayState?.saltPayCardArtworkUrl == null) {
            imvFrontCard.load(R.drawable.img_salt_pay_visa) {
                scale(Scale.FILL)
                crossfade(enable = true)
            }
        } else {
            walletFragment.loadImageIntoImageView(state.onboardingSaltPayState.saltPayCardArtworkUrl, imvFrontCard)
        }
        imvFirstBackupCard.load(R.drawable.card_placeholder_wallet)

        // if (state.onboardingSaltPayState?.saltPayCardArtworkUrl == null) {
        //     //if saltPay url not loaded -> load from resource
        //     val bitmap = BitmapFactory.decodeResource(walletFragment.resources, R.drawable.img_salt_pay_visa)
        //     imvFrontCard.setImageBitmap(bitmap)
        // } else {
        //     walletFragment.loadImageIntoImageView(state.onboardingSaltPayState.saltPayCardArtworkUrl, imvFrontCard)
        // }
        // walletFragment.loadImageIntoImageView(state.cardArtworkUrl, imvFirstBackupCard)
    }

    fun showAddBackupCards(state: BackupState) = with(walletFragment.binding) {
        when (state.backupCardsNumber) {
            0 -> {
                tvHeader.text = walletFragment.getText(R.string.onboarding_saltpay_title_no_backup_card)
                tvBody.text = walletFragment.getText(R.string.onboarding_saltpay_subtitle_no_backup_cards)
            }
            1 -> {
                tvHeader.text = walletFragment.getText(R.string.onboarding_saltpay_title_one_backup_card)
                tvBody.text = walletFragment.getText(R.string.onboarding_saltpay_subtitle_one_backup_card)
            }
            else -> {}
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
            SaltPayActivationStep.NoGas -> handleNoGas()
            SaltPayActivationStep.NeedPin -> handleNeedPin()
            SaltPayActivationStep.CardRegistration -> handleCardRegistration()
            SaltPayActivationStep.KycIntro -> handleKycIntro()
            SaltPayActivationStep.KycStart -> handleKycStart(state)
            SaltPayActivationStep.KycWaiting -> handleKycWaiting()
            SaltPayActivationStep.KycReject -> handleKycReject()
            SaltPayActivationStep.Claim -> handleClaim(state)
            SaltPayActivationStep.ClaimInProgress -> handleClaim(state)
            SaltPayActivationStep.ClaimSuccess -> handleClaim(state)
            SaltPayActivationStep.Success -> handleSuccess(state)
        }
        progressButton?.changeState(state.mainButtonState)
    }

    private fun handleNoGas() {
        // normally this shouldn't happen
        store.dispatch(NavigationAction.PopBackTo(AppScreen.Home))
        store.dispatchDialogShow(SaltPayDialog.Activation.NoGas)
    }

    private fun handleNeedPin() = with(walletFragment.bindingSaltPay) {
        toolbar.title = getText(R.string.onboarding_navbar_pin)
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
        toolbar.title = getText(R.string.onboarding_navbar_register_wallet)
        showOnlyView(connectCard.root) {
            progressButton = SaltPayProgressButton(connectCard.root)
            connectCard.btnConnect.setOnClickListener {
                Analytics.send(Onboarding.ButtonConnect())
                store.dispatch(OnboardingSaltPayAction.RegisterCard)
            }
        }
    }

    private fun handleKycIntro() = with(walletFragment.bindingSaltPay) {
        toolbar.title = getText(R.string.onboarding_navbar_kyc_start)
        showOnlyView(verifyIdentity.root) {
            progressButton = SaltPayProgressButton(verifyIdentity.root)
            verifyIdentity.btnVerify.setOnClickListener {
                store.dispatch(OnboardingSaltPayAction.OpenUtorgKYC)
            }
        }
    }

    private fun handleKycStart(state: OnboardingSaltPayState) = with(walletFragment.bindingSaltPay) {
        toolbar.title = getText(R.string.onboarding_navbar_kyc_start)
        showOnlyView(webvVerifyIdentity) {
            progressButton = null
            webvVerifyIdentity.configureSettings()
            webvVerifyIdentity.webViewClient = UtorgWebViewClient(
                successUrl = state.saltPayManager.kycUrlProvider.doneUrl,
                onSuccess = {
                    webvVerifyIdentity.stop()
                    store.dispatch(OnboardingSaltPayAction.UtorgKYCRedirectSuccess)
                },
            )
            webvVerifyIdentity.webChromeClient = UtorgWebChromeClient(walletFragment.requireActivity())
            webvVerifyIdentity.loadUrl(state.saltPayManager.kycUrlProvider.requestUrl)
        }
    }

    private fun handleKycWaiting() = with(walletFragment.bindingSaltPay.kycInProgress) {
        toolbar.title = getText(R.string.onboarding_navbar_kyc_progress)
        showOnlyView(root)

        imvInProgress.setDrawable(R.drawable.ic_in_progress)
        tvHeader.text = getText(R.string.onboarding_title_kyc_waiting)
        tvBody.text = getText(R.string.onboarding_subtitle_kyc_waiting)

        btnOpenSupportChat.hide()
        btnOpenSupportChat.setOnClickListener {
            store.dispatch(GlobalAction.OpenChat(SupportInfo()))
        }

        btnKycAction.text = getText(R.string.onboarding_button_kyc_waiting)
        btnKycAction.setOnClickListener {
            store.dispatch(OnboardingSaltPayAction.Update)
        }
        progressButton = SaltPayProgressButton(root)
    }

    private fun handleKycReject() = with(walletFragment.bindingSaltPay.kycInProgress) {
        toolbar.title = getText(R.string.onboarding_navbar_kyc_progress)
        showOnlyView(root)

        imvInProgress.setDrawable(R.drawable.ic_reject)
        tvHeader.text = getText(R.string.onboarding_title_kyc_retry)
        tvBody.text = getText(R.string.onboarding_subtitle_kyc_retry)

        btnOpenSupportChat.hide()
        btnKycAction.text = getText(R.string.onboarding_button_kyc_start)
        btnKycAction.setOnClickListener {
            store.dispatch(OnboardingSaltPayAction.Update)
        }
        progressButton = SaltPayProgressButton(root)
    }

    private fun handleClaim(state: OnboardingSaltPayState) = with(walletFragment.bindingSaltPay) {
        toolbar.title = getText(R.string.onboarding_getting_started)
        val btnMain = actionContainer.btnContainer.findViewById<MaterialButton>(R.id.btn_main_action)
        val tvHeader = actionContainer.tvHeader
        val tvBody = actionContainer.tvBody

        showOnlyView(claimBinding.root) {
            // topContainer.progress.pbState.hide()
            progressButton = SaltPayProgressButton(actionContainer.root)
            topContainer.imvCardBackground.apply {
                setBackgroundDrawable(context.getDrawableCompat(R.drawable.shape_rectangle_rounded_8))
            }
            topContainer.imvFrontCard.load(R.drawable.img_salt_pay_visa) {
                scale(Scale.FILL)
                crossfade(enable = true)
            }
            actionContainer.btnAlternativeAction.hide()
            updateConstraints(state.step, R.layout.lp_onboarding_topup_wallet)
        }

        btnRefreshBalanceWidget.changeState(if (state.claimInProgress) ProgressState.Loading else ProgressState.Done)
        if (btnRefreshBalanceWidget.isShowing != true) {
            btnRefreshBalanceWidget.mainView.setOnClickListener {
                store.dispatch(OnboardingSaltPayAction.RefreshClaim)
            }
        }
        val tokenAmountString = tokenAmountString(state)
        topContainer.onboardingTvBalance.tvBalanceValue.text = tokenAmountString ?: ""
        topContainer.onboardingTvBalance.tvBalanceCurrency.text = ""

        when (state.step) {
            SaltPayActivationStep.Claim -> {
                claimValueString(state)?.let {
                    tvHeader.text = getText(R.string.onboarding_title_claim, it)
                }
                tvBody.text = getText(R.string.onboarding_subtitle_claim)

                btnMain.text = getText(R.string.onboarding_button_claim)
                btnMain.isEnabled
                btnMain.setOnClickListener {
                    Analytics.send(Onboarding.ButtonClaim())
                    store.dispatch(OnboardingSaltPayAction.Claim)
                }
                progressButton = SaltPayProgressButton(actionContainer.root)
                progressButton?.isEnabled = true
            }
            SaltPayActivationStep.ClaimInProgress -> {
                tvHeader.text = getText(R.string.onboarding_title_claim_progress)
                tvBody.text = getText(R.string.onboarding_subtitle_claim_progress)
                btnMain.text = getText(R.string.onboarding_button_claim)
                progressButton = SaltPayProgressButton(actionContainer.root)
                progressButton?.isEnabled = false
            }
            SaltPayActivationStep.ClaimSuccess -> {
                tvHeader.setText(R.string.common_success)
                tvBody.setText(R.string.onboarding_subtitle_success_claim)

                btnMain.setText(R.string.onboarding_button_continue_wallet)
                btnMain.setOnClickListener {
                    walletFragment.showConfetti(false)
                    store.dispatch(OnboardingWalletAction.FinishOnboarding)
                }

                btnRefreshBalanceWidget.mainView.setOnClickListener(null)
                updateConstraints(state.step, R.layout.lp_onboarding_done_activation)
                walletFragment.showConfetti(true)
                progressButton = SaltPayProgressButton(actionContainer.root)
                progressButton?.isEnabled = true
            }
            else -> {}
        }
    }

    private fun handleSuccess(state: OnboardingSaltPayState) = with(walletFragment.binding) {
        walletFragment.bindingSaltPay.onboardingSaltpayContainer.hide()
        onboardingWalletContainer.show()
        walletFragment.showSuccess()
        tvBody.text = getText(R.string.onboarding_subtitle_success_claim)
        layoutButtonsCommon.btnWalletMainAction.text = getText(R.string.onboarding_button_continue_wallet)
    }

    private fun tokenAmountString(state: OnboardingSaltPayState): String? {
        return state.tokenAmount.value?.toFormattedCurrencyString(
            decimals = state.tokenAmount.decimals,
            currency = state.tokenAmount.currencySymbol,
        )
    }

    private fun claimValueString(state: OnboardingSaltPayState): String? {
        return state.amountToClaim?.value?.toFormattedCurrencyString(
            decimals = state.amountToClaim.decimals,
            currency = state.amountToClaim.currencySymbol,
        )
    }

    private fun updateConstraints(currentStep: SaltPayActivationStep, @LayoutRes layoutId: Int) {
        if (this.previousStep == currentStep) return

        with(claimBinding.onboardingTopContainer) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(root.context, layoutId)
            constraintSet.applyTo(onboardingWalletContainer)
            val transition = InternalNoteLayoutTransition()
            transition.interpolator = OvershootInterpolator()
            TransitionManager.beginDelayedTransition(onboardingWalletContainer, transition)
        }
    }

    private fun showOnlyView(view: View, onShowListener: (() -> Unit)? = null) {
        walletFragment.bindingSaltPay.onboardingSaltpayContainer.children
            .filter { it.id != view.id }
            .forEach { it.hide() }
        view.show { onShowListener?.invoke() }
    }

    private fun getText(@StringRes resId: Int): String {
        return walletFragment.getString(resId)
    }

    private fun getText(@StringRes resId: Int, vararg formatArgs: Any?): String {
        return walletFragment.getString(resId, *formatArgs)
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
