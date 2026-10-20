package com.tangem.features.hotwallet.walletactivation.entry

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.stepper.api.HotWalletStepperComponent
import com.tangem.features.hotwallet.walletactivation.entry.routing.WalletActivationRoute
import javax.inject.Inject

internal class WalletActivationStepperStateManager @Inject constructor() {

    fun getStepperState(
        route: WalletActivationRoute,
        isAccessCodeStepRequired: Boolean,
    ): HotWalletStepperComponent.StepperUM? {
        val state = when (route) {
            is WalletActivationRoute.ManualBackupStart -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                shouldShowBackButton = true,
                shouldShowSkipButton = false,
            )
            is WalletActivationRoute.ManualBackupPhrase -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP_PHRASE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                shouldShowBackButton = true,
                shouldShowSkipButton = false,
            )
            is WalletActivationRoute.ManualBackupCheck -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP_CHECK,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                shouldShowBackButton = true,
                shouldShowSkipButton = false,
            )
            is WalletActivationRoute.ManualBackupCompleted -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_BACKUP_COMPLETED,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_backup),
                shouldShowBackButton = false,
                shouldShowSkipButton = false,
            )
            is WalletActivationRoute.SetAccessCode -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_ACCESS_CODE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.access_code_navtitle),
                shouldShowBackButton = false,
                shouldShowSkipButton = true,
            )
            is WalletActivationRoute.ConfirmAccessCode -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_ACCESS_CODE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.access_code_navtitle),
                shouldShowBackButton = true,
                shouldShowSkipButton = true,
            )
            is WalletActivationRoute.PushNotifications -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_NOTIFICATIONS,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.onboarding_title_notifications),
                shouldShowBackButton = false,
                shouldShowSkipButton = false,
            )
            is WalletActivationRoute.SetupFinished -> HotWalletStepperComponent.StepperUM(
                currentStep = STEP_DONE,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.common_done),
                shouldShowBackButton = false,
                shouldShowSkipButton = false,
            )
        }

        return if (isAccessCodeStepRequired) state else state.withoutAccessCodeStep()
    }

    private fun HotWalletStepperComponent.StepperUM.withoutAccessCodeStep() = copy(
        currentStep = if (currentStep > STEP_ACCESS_CODE) currentStep - 1 else currentStep,
        steps = steps - 1,
    )

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