package com.tangem.features.onboarding.usedcard.routing

import com.tangem.core.decompose.navigation.Route

internal sealed class UsedCardOnboardingRoute : Route {

    data object AlreadyActivated : UsedCardOnboardingRoute()

    data object AskBiometry : UsedCardOnboardingRoute()

    data object PushNotifications : UsedCardOnboardingRoute()

    data object SyncWallet : UsedCardOnboardingRoute()
}