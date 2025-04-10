package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.managetokens.component.OnboardingManageTokensComponent
import com.tangem.features.onboarding.v2.done.api.OnboardingDoneComponent
import com.tangem.features.onboarding.v2.entry.impl.model.OnboardingEntryModel
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.note.api.OnboardingNoteComponent
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.visa.api.OnboardingVisaComponent
import javax.inject.Inject

@Suppress("LongParameterList")
internal class OnboardingChildFactory @Inject constructor(
    private val multiWalletComponentFactory: OnboardingMultiWalletComponent.Factory,
    private val onboardingManageTokensComponentFactory: OnboardingManageTokensComponent.Factory,
    private val onboardingDoneComponentFactory: OnboardingDoneComponent.Factory,
    private val onBoardingVisaComponentFactory: OnboardingVisaComponent.Factory,
    private val askBiometryComponentFactory: AskBiometryComponent.Factory,
    private val onboardingNoteComponentFactory: OnboardingNoteComponent.Factory,
    private val onboardingTwinComponentFactory: OnboardingTwinComponent.Factory,
) {

    fun createChild(
        route: OnboardingRoute,
        childContext: AppComponentContext,
        model: OnboardingEntryModel,
        onManageTokensDone: () -> Unit,
    ): Any {
        return when (route) {
            is OnboardingRoute.MultiWallet -> multiWalletComponentFactory.create(
                context = childContext,
                params = OnboardingMultiWalletComponent.Params(
                    scanResponse = route.scanResponse,
                    withSeedPhraseFlow = route.withSeedPhraseFlow,
                    titleProvider = route.titleProvider,
                    onDone = route.onDone,
                    mode = route.mode,
                ),
            )
            is OnboardingRoute.Visa -> onBoardingVisaComponentFactory.create(
                context = childContext,
                params = OnboardingVisaComponent.Params(
                    scanResponse = route.scanResponse,
                    titleProvider = route.titleProvider,
                    onDone = route.onDone,
                ),
            )
            is OnboardingRoute.ManageTokens -> onboardingManageTokensComponentFactory.create(
                context = childContext,
                params = OnboardingManageTokensComponent.Params(
                    userWalletId = route.userWallet.walletId,
                ),
                onDone = onManageTokensDone,
            )
            is OnboardingRoute.Done -> onboardingDoneComponentFactory.create(
                context = childContext,
                params = OnboardingDoneComponent.Params(
                    mode = route.mode,
                    onDone = route.onDone,
                ),
            )
            is OnboardingRoute.Note -> onboardingNoteComponentFactory.create(
                context = childContext,
                params = OnboardingNoteComponent.Params(
                    scanResponse = route.scanResponse,
                    titleProvider = route.titleProvider,
                    onDone = route.onDone,
                ),
            )
            is OnboardingRoute.Twins -> onboardingTwinComponentFactory.create(
                context = childContext,
                params = OnboardingTwinComponent.Params(
                    scanResponse = route.scanResponse,
                    modelCallbacks = model.onboardingTwinModelCallbacks,
                    titleProvider = route.titleProvider,
                    mode = route.mode,
                ),
            )
            is OnboardingRoute.AskBiometry -> askBiometryComponentFactory.create(
                context = childContext,
                params = AskBiometryComponent.Params(
                    bottomSheetVariant = false,
                    modelCallbacks = route.modelCallbacks,
                ),
            )
            is OnboardingRoute.None -> Unit
        }
    }
}