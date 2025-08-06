package com.tangem.features.hotwallet.walletactivation.entry

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.features.hotwallet.walletactivation.entry.routing.WalletActivationRoute
import javax.inject.Inject

internal class WalletActivationStepperStateManager @Inject constructor() {

    fun getStepperState(route: WalletActivationRoute): HotWalletStepperComponent.StepperUM? {
        return when (route) {
            is WalletActivationRoute.ManualBackupStart -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                showBackButton = true,
                showSkipButton = false,
                showFeedbackButton = true,
            )
            is WalletActivationRoute.ManualBackupPhrase -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP_PHRASE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                showBackButton = true,
                showSkipButton = false,
                showFeedbackButton = true,
            )
            is WalletActivationRoute.ManualBackupCheck -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP_CHECK,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                showBackButton = true,
                showSkipButton = false,
                showFeedbackButton = true,
            )
            is WalletActivationRoute.ManualBackupCompleted -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP_COMPLETED,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                showBackButton = false,
                showSkipButton = false,
                showFeedbackButton = false,
            )
            is WalletActivationRoute.SetAccessCode -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_ACCESS_CODE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.access_code_navtitle),
                showBackButton = false,
                showSkipButton = true,
                showFeedbackButton = false,
            )
            is WalletActivationRoute.ConfirmAccessCode -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_ACCESS_CODE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.access_code_navtitle),
                showBackButton = true,
                showSkipButton = false,
                showFeedbackButton = false,
            )
            is WalletActivationRoute.PushNotifications -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_NOTIFICATIONS,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.onboarding_title_notifications),
                showBackButton = false,
                showSkipButton = false,
                showFeedbackButton = false,
            )
            is WalletActivationRoute.SetupFinished -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_DONE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_done),
                showBackButton = false,
                showSkipButton = false,
                showFeedbackButton = false,
            )
        }
    }

    companion object {
        private const val STEPS_COUNT = 7

        private const val STEP_BACKUP = 1
        private const val STEP_BACKUP_PHRASE = 2
        private const val STEP_BACKUP_CHECK = 3
        private const val STEP_BACKUP_COMPLETED = 4
        private const val STEP_ACCESS_CODE = 5
        private const val STEP_NOTIFICATIONS = 6
        private const val STEP_DONE = 7
    }
}