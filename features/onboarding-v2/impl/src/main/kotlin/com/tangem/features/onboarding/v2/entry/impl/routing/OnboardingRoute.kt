package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.onboarding.v2.TitleProvider
import com.tangem.features.onboarding.v2.done.api.OnboardingDoneComponent
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent

sealed class OnboardingRoute : Route {

    data object None : OnboardingRoute()

    data class Note(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
        val onDone: () -> Unit,
    ) : OnboardingRoute()

    data class MultiWallet(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
        val withSeedPhraseFlow: Boolean,
        val mode: OnboardingMultiWalletComponent.Mode,
        val onDone: (UserWallet.Cold) -> Unit,
    ) : OnboardingRoute()

    data class Visa(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
        val onDone: () -> Unit,
    ) : OnboardingRoute()

    data class Twins(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
        val mode: OnboardingTwinComponent.Params.Mode,
    ) : OnboardingRoute()

    data class ManageTokens(
        val userWallet: UserWallet,
    ) : OnboardingRoute()

    data class AskBiometry(
        val modelCallbacks: AskBiometryComponent.ModelCallbacks,
    ) : OnboardingRoute()

    data class Done(
        val mode: OnboardingDoneComponent.Mode,
        val onDone: () -> Unit,
    ) : OnboardingRoute()
}