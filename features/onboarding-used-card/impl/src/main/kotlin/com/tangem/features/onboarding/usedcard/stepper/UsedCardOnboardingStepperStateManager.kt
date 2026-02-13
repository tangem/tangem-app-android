package com.tangem.features.onboarding.usedcard.stepper

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.usedcard.routing.UsedCardOnboardingRoute
import com.tangem.features.onboarding.usedcard.impl.R
import javax.inject.Inject

internal class UsedCardOnboardingStepperStateManager @Inject constructor() {

    fun getStepperState(route: UsedCardOnboardingRoute): UsedCardStepperUM {
        return when (route) {
            is UsedCardOnboardingRoute.AlreadyActivated -> UsedCardStepperUM(
                currentStep = STEP_ALREADY_ACTIVATED,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.auth_info_add_wallet_title),
                shouldShowBackButton = true,
            )
            is UsedCardOnboardingRoute.AskBiometry -> UsedCardStepperUM(
                currentStep = STEP_BIOMETRY,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.onboarding_navbar_upgrade_wallet_biometrics),
                shouldShowBackButton = false,
            )
            is UsedCardOnboardingRoute.PushNotifications -> UsedCardStepperUM(
                currentStep = STEP_PUSH_NOTIFICATIONS,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.onboarding_title_notifications),
                shouldShowBackButton = false,
            )
            is UsedCardOnboardingRoute.SyncWallet -> UsedCardStepperUM(
                currentStep = STEP_SYNC_WALLET,
                steps = STEPS_COUNT,
                title = resourceReference(R.string.onboarding_used_card_last_step),
                shouldShowBackButton = false,
            )
        }
    }

    companion object {
        private const val STEPS_COUNT = 4

        private const val STEP_ALREADY_ACTIVATED = 1
        private const val STEP_BIOMETRY = 2
        private const val STEP_PUSH_NOTIFICATIONS = 3
        private const val STEP_SYNC_WALLET = 4
    }
}