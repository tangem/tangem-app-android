package com.tangem.features.hotwallet.createwalletbackup

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.hotwallet.createwalletbackup.routing.CreateWalletBackupRoute
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import javax.inject.Inject

internal class CreateWalletBackupStepperStateManager @Inject constructor() {

    fun getStepperState(route: CreateWalletBackupRoute): HotWalletStepperComponent.StepperUM? {
        return when (route) {
            is CreateWalletBackupRoute.RecoveryPhraseStart -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_START,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                showBackButton = true,
                showSkipButton = false,
                showFeedbackButton = true,
            )
            is CreateWalletBackupRoute.RecoveryPhrase -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_PHRASE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                showBackButton = true,
                showSkipButton = false,
                showFeedbackButton = true,
            )
            is CreateWalletBackupRoute.ConfirmBackup -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_CONFIRM,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                showBackButton = true,
                showSkipButton = false,
                showFeedbackButton = true,
            )
            is CreateWalletBackupRoute.BackupCompleted -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_COMPLETED,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_done),
                showBackButton = false,
                showSkipButton = false,
                showFeedbackButton = false,
            )
        }
    }

    companion object {
        private const val STEPS_COUNT = 4

        private const val STEP_START = 1
        private const val STEP_PHRASE = 2
        private const val STEP_CONFIRM = 3
        private const val STEP_COMPLETED = 4
    }
}