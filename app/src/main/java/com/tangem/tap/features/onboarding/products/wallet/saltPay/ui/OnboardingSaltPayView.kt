package com.tangem.tap.features.onboarding.products.wallet.saltPay.ui

import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.children
import coil.load
import coil.size.Scale
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.tangem.common.extensions.guard
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
import com.tangem.tap.features.onboarding.products.wallet.ui.OnboardingWalletFragment
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
internal class OnboardingSaltPayView(
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

        //TODO: at now we can hide the image only by changing alpha channel to 0, because
        // the OnboardingWalletFragment and WalletCardsWidget manipulate it visibility through changing
        // View.VISIBILITY states.
        // imvSecondBackupCard.alpha = 0f
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
            SaltPayRegistrationStep.NoGas -> handleNoGas()
            SaltPayRegistrationStep.NeedPin -> handleNeedPin()
            SaltPayRegistrationStep.CardRegistration -> handleCardRegistration()
            SaltPayRegistrationStep.KycIntro -> handleKycIntro()
            SaltPayRegistrationStep.KycStart -> handleKycStart(state)
            SaltPayRegistrationStep.KycWaiting -> handleKycWaiting()
            // SaltPayRegistrationStep.Claim -> handleKycWaiting()
            // SaltPayRegistrationStep.ClaimInProgress -> handleKycWaiting()
            // SaltPayRegistrationStep.ClaimSuccess -> handleKycWaiting()
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

    // private fun setupTopUpWalletState(state: OnboardingNoteState) = with(binding.onboardingActionContainer) {
    //     if (state.isBuyAllowed) {
    //         btnMainAction.setText(com.tangem.wallet.R.string.onboarding_top_up_button_but_crypto)
    //         btnMainAction.setOnClickListener {
    //             com.tangem.tap.store.dispatch(com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction.TopUp)
    //         }
    //
    //         btnAlternativeAction.isVisible = true
    //         btnAlternativeAction.setText(com.tangem.wallet.R.string.onboarding_top_up_button_show_wallet_address)
    //         btnAlternativeAction.setOnClickListener {
    //             com.tangem.tap.store.dispatch(com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction.ShowAddressInfoDialog)
    //         }
    //     } else {
    //         btnMainAction.setText(com.tangem.wallet.R.string.onboarding_button_receive_crypto)
    //         btnMainAction.setOnClickListener {
    //             com.tangem.tap.store.dispatch(com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction.ShowAddressInfoDialog)
    //         }
    //
    //         btnAlternativeAction.isVisible = false
    //     }
    //
    //     tvHeader.setText(com.tangem.wallet.R.string.onboarding_top_up_header)
    //     if (state.balanceNonCriticalError == null) {
    //         tvBody.setText(com.tangem.wallet.R.string.onboarding_top_up_body)
    //     } else {
    //         state.walletBalance.amountToCreateAccount?.let { amount ->
    //             val tvBodyMessage = getString(
    //                 com.tangem.wallet.R.string.onboarding_top_up_body_no_account_error,
    //                 amount, state.walletBalance.currency.currencySymbol,
    //             )
    //             tvBody.text = tvBodyMessage
    //         }
    //     }
    //
    //     btnRefreshBalanceWidget.changeState(state.walletBalance.state)
    //     if (btnRefreshBalanceWidget.isShowing != true) {
    //         btnRefreshBalanceWidget.mainView.setOnClickListener {
    //             com.tangem.tap.store.dispatch(com.tangem.tap.features.onboarding.products.note.redux.OnboardingNoteAction.Balance.Update)
    //         }
    //     }
    //     binding.onboardingTopContainer.imvCardBackground
    //         .setBackgroundDrawable(requireContext().getDrawableCompat(com.tangem.wallet.R.drawable.shape_rectangle_rounded_8))
    //     updateConstraints(state.currentStep, com.tangem.wallet.R.layout.lp_onboarding_topup_wallet)
    // }

    private fun handleFinished() {
        walletFragment.binding.onboardingRoot.beginDelayedTransition()
        walletFragment.bindingSaltPay.onboardingSaltpayContainer.hide()
        walletFragment.binding.onboardingWalletContainer.show()
        walletFragment.showSuccess()
    }

    private fun showOnlyView(view: View, onShowListener: (() -> Unit)? = null) {
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
