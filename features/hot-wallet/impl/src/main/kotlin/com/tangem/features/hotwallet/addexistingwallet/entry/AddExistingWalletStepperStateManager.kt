package com.tangem.features.hotwallet.addexistingwallet.entry

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.hotwallet.addexistingwallet.entry.routing.AddExistingWalletRoute
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent

internal class AddExistingWalletStepperStateManager {

    fun getStepperState(route: AddExistingWalletRoute): HotWalletStepperComponent.StepperUM? {
        return when (route) {
            is AddExistingWalletRoute.Start -> null

            is AddExistingWalletRoute.Import -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_IMPORT,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.wallet_import_seed_navtitle),
                showBackButton = true,
                showSkipButton = false,
                showFeedbackButton = true,
            )

            is AddExistingWalletRoute.BackupCompleted -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                showBackButton = false,
                showSkipButton = false,
                showFeedbackButton = false,
            )

            is AddExistingWalletRoute.SetAccessCode -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_ACCESS_CODE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.access_code_navtitle),
                showBackButton = false,
                showSkipButton = true,
                showFeedbackButton = false,
            )

            is AddExistingWalletRoute.ConfirmAccessCode -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_ACCESS_CODE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.access_code_navtitle),
                showBackButton = true,
                showSkipButton = true,
                showFeedbackButton = false,
            )

            is AddExistingWalletRoute.PushNotifications -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_NOTIFICATIONS,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.onboarding_title_notifications),
                showBackButton = false,
                showSkipButton = false,
                showFeedbackButton = false,
            )

            is AddExistingWalletRoute.SetupFinished -> HotWalletStepperComponent.StepperUM(
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
        private const val STEPS_COUNT = 5

        private const val STEP_IMPORT = 1
        private const val STEP_BACKUP = 2
        private const val STEP_ACCESS_CODE = 3
        private const val STEP_NOTIFICATIONS = 4
        private const val STEP_DONE = 5
    }
}