package com.tangem.feature.onboarding.navigation

/**
 * Onboarding router
 */
// TODO: Move to onboarding api module https://tangem.atlassian.net/browse/AND-4841
interface OnboardingRouter {

    companion object {
        const val CAN_SKIP_BACKUP = "onboarding_wallet_can_skip_backup"
    }
}
