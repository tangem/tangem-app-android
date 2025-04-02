package com.tangem.features.onboarding.v2.visa.impl.route

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onboarding.v2.impl.R

internal fun OnboardingVisaRoute.screenTitle(): TextReference = when (this) {
    OnboardingVisaRoute.AccessCode ->
        resourceReference(R.string.visa_onboarding_access_code_navigation_title)
    is OnboardingVisaRoute.ChooseWallet ->
        resourceReference(R.string.visa_onboarding_account_activation_navigation_title)
    is OnboardingVisaRoute.InProgress ->
        resourceReference(R.string.common_in_progress)
    is OnboardingVisaRoute.OtherWalletApproveOption,
    is OnboardingVisaRoute.TangemWalletApproveOption,
    -> resourceReference(R.string.visa_onboarding_wallet_connection_navigation_title)
    is OnboardingVisaRoute.PinCode ->
        resourceReference(R.string.visa_onboarding_pin_code_navigation_title)
    is OnboardingVisaRoute.Welcome,
    is OnboardingVisaRoute.WelcomeBack,
    -> resourceReference(R.string.onboarding_getting_started)
}