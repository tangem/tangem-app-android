package com.tangem.features.onboarding.v2.entry.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.biometry.BiometryFeatureToggles
import com.tangem.features.onboarding.v2.TitleProvider
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.analytics.OnboardingEntryEvent
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class OnboardingEntryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val tangemSdkManager: TangemSdkManager,
    private val settingsRepository: SettingsRepository,
    private val askBiometryFeatureToggles: BiometryFeatureToggles,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<OnboardingEntryComponent.Params>()

    val stackNavigation = StackNavigation<OnboardingRoute>()
    val titleProvider = object : TitleProvider {
        override val currentTitle = MutableStateFlow(stringReference(""))
        override fun changeTitle(text: TextReference) {
            currentTitle.value = text
        }
    }

    val startRoute = routeByProductType(params.scanResponse)

    private fun routeByProductType(scanResponse: ScanResponse): OnboardingRoute {
        val multiWalletNavigationMode = when (params.multiWalletMode) {
            OnboardingEntryComponent.MultiWalletMode.Onboarding -> OnboardingMultiWalletComponent.Mode.Onboarding
            OnboardingEntryComponent.MultiWalletMode.AddBackup -> OnboardingMultiWalletComponent.Mode.AddBackup
        }

        return when (scanResponse.productType) {
            ProductType.Wallet -> OnboardingRoute.MultiWallet(
                scanResponse = scanResponse,
                withSeedPhraseFlow = false,
                titleProvider = titleProvider,
                onDone = ::onMultiWalletOnboardingDone,
                mode = multiWalletNavigationMode,
            )
            ProductType.Ring,
            ProductType.Wallet2,
            -> OnboardingRoute.MultiWallet(
                scanResponse = scanResponse,
                withSeedPhraseFlow = true,
                titleProvider = titleProvider,
                onDone = ::onMultiWalletOnboardingDone,
                mode = multiWalletNavigationMode,
            )
            ProductType.Visa -> OnboardingRoute.Visa(
                scanResponse = scanResponse,
                titleProvider = titleProvider,
                onDone = ::onVisaOnboardingDone,
            )
            ProductType.Note -> OnboardingRoute.Note(
                scanResponse = scanResponse,
                titleProvider = titleProvider,
                onDone = ::navigateToFinalScreenFlow,
            )
            else -> error("Unsupported")
        }
    }

    private fun onMultiWalletOnboardingDone(userWallet: UserWallet) {
        when {
            params.multiWalletMode == OnboardingEntryComponent.MultiWalletMode.AddBackup -> {
                stackNavigation.replaceAll(OnboardingRoute.Done(onDone = ::navigateToWalletScreen))
            }
            userWallet.scanResponse.cardTypesResolver.isMultiwalletAllowed() -> {
                stackNavigation.replaceAll(OnboardingRoute.ManageTokens(userWallet))
            }
            else -> {
                navigateToFinalScreenFlow()
            }
        }
    }

    fun onManageTokensDone() {
        navigateToFinalScreenFlow()
    }

    private fun onVisaOnboardingDone() {
        navigateToFinalScreenFlow()
    }

    private fun navigateToFinalScreenFlow() {
        if (askBiometryFeatureToggles.isAskForBiometryEnabled) {
            modelScope.launch {
                if (tangemSdkManager.checkCanUseBiometry() && settingsRepository.shouldShowSaveUserWalletScreen()) {
                    stackNavigation.replaceAll(
                        OnboardingRoute.AskBiometry(modelCallbacks = AskBiometryModelCallbacks()),
                    )
                } else {
                    stackNavigation.replaceAll(OnboardingRoute.Done(onDone = ::navigateToWalletScreen))
                }
            }
        } else {
            stackNavigation.replaceAll(OnboardingRoute.Done(onDone = ::navigateToWalletScreen))
        }
    }

    inner class AskBiometryModelCallbacks : AskBiometryComponent.ModelCallbacks {
        override fun onAllowed() {
            analyticsEventHandler.send(OnboardingEntryEvent.Biometric(OnboardingEntryEvent.Biometric.State.On))
            stackNavigation.replaceAll(OnboardingRoute.Done(onDone = ::navigateToWalletScreen))
        }

        override fun onDenied() {
            analyticsEventHandler.send(OnboardingEntryEvent.Biometric(OnboardingEntryEvent.Biometric.State.Off))
            stackNavigation.replaceAll(OnboardingRoute.Done(onDone = ::navigateToWalletScreen))
        }
    }

    private fun navigateToWalletScreen() {
        if (askBiometryFeatureToggles.isAskForBiometryEnabled) {
            router.replaceAll(AppRoute.Wallet)
        } else {
            modelScope.launch(NonCancellable) {
                router.replaceAll(AppRoute.Wallet)
                if (tangemSdkManager.checkCanUseBiometry() && settingsRepository.shouldShowSaveUserWalletScreen()) {
                    delay(timeMillis = 1_800)
                    router.push(AppRoute.SaveWallet)
                }
            }
        }
    }
}