package com.tangem.features.onboarding.v2.entry.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.replaceAll
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.scan.ProductType
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.biometry.AskBiometryComponent
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.onboarding.v2.TitleProvider
import com.tangem.features.onboarding.v2.common.ui.CantLeaveBackupDialog
import com.tangem.features.onboarding.v2.done.api.OnboardingDoneComponent
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent.Mode
import com.tangem.features.onboarding.v2.entry.impl.analytics.OnboardingEntryEvent
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics.OnboardingVisaAnalyticsEvent
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val uiMessageSender: UiMessageSender,
    private val userWalletsListManager: UserWalletsListManager,
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
    private val userWalletsListRepository: UserWalletsListRepository,
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
    val onboardingTwinModelCallbacks = OnboardingTwinModelCallbacks()

    fun onManageTokensDone() {
        navigateToFinalScreenFlow()
    }

    fun onBack() {
        uiMessageSender.send(CantLeaveBackupDialog)
    }

    private fun routeByProductType(scanResponse: ScanResponse): OnboardingRoute {
        return when (scanResponse.productType) {
            ProductType.Wallet,
            ProductType.Wallet2,
            ProductType.Ring,
            -> {
                val multiWalletNavigationMode = when (params.mode) {
                    Mode.Onboarding -> OnboardingMultiWalletComponent.Mode.Onboarding
                    Mode.AddBackupWallet1 -> OnboardingMultiWalletComponent.Mode.AddBackup
                    Mode.ContinueFinalize -> OnboardingMultiWalletComponent.Mode.ContinueFinalize
                    else -> error("Incorrect onboarding type")
                }

                OnboardingRoute.MultiWallet(
                    scanResponse = scanResponse,
                    withSeedPhraseFlow = scanResponse.productType != ProductType.Wallet,
                    titleProvider = titleProvider,
                    onDone = ::onMultiWalletOnboardingDone,
                    mode = multiWalletNavigationMode,
                )
            }
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
            ProductType.Twins -> {
                val mode = when (params.mode) {
                    Mode.Onboarding -> OnboardingTwinComponent.Params.Mode.CreateWallet
                    Mode.WelcomeOnlyTwin -> OnboardingTwinComponent.Params.Mode.WelcomeOnly
                    Mode.RecreateWalletTwin -> OnboardingTwinComponent.Params.Mode.RecreateWallet
                    else -> error("Incorrect onboarding type")
                }

                OnboardingRoute.Twins(
                    scanResponse = scanResponse,
                    titleProvider = titleProvider,
                    mode = mode,
                )
            }
            else -> error("Unsupported")
        }
    }

    private fun onMultiWalletOnboardingDone(userWallet: UserWallet.Cold) {
        when {
            params.mode == Mode.AddBackupWallet1 -> {
                stackNavigation.replaceAll(
                    OnboardingRoute.Done(
                        mode = OnboardingDoneComponent.Mode.WalletCreated,
                        onDone = ::exitComponentScreen,
                    ),
                )
            }
            userWallet.scanResponse.cardTypesResolver.isMultiwalletAllowed() -> {
                stackNavigation.replaceAll(OnboardingRoute.ManageTokens(userWallet))
            }
            else -> {
                navigateToFinalScreenFlow()
            }
        }
    }

    private fun onVisaOnboardingDone() {
        navigateToFinalScreenFlow(doneMode = OnboardingDoneComponent.Mode.GoodToGo)
    }

    private fun navigateToFinalScreenFlow(
        doneMode: OnboardingDoneComponent.Mode = OnboardingDoneComponent.Mode.WalletCreated,
    ) {
        modelScope.launch {
            if (tangemSdkManager.checkCanUseBiometry() && settingsRepository.shouldShowAskBiometry()) {
                doIfVisa {
                    analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.BiometricScreenOpened)
                }
                stackNavigation.replaceAll(
                    OnboardingRoute.AskBiometry(modelCallbacks = AskBiometryModelCallbacks(doneMode)),
                )
            } else {
                doIfVisa {
                    analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.SuccessScreenOpened)
                }
                stackNavigation.replaceAll(
                    OnboardingRoute.Done(
                        mode = doneMode,
                        onDone = ::exitComponentScreen,
                    ),
                )
            }
        }
    }

    inner class AskBiometryModelCallbacks(
        private val doneMode: OnboardingDoneComponent.Mode,
    ) : AskBiometryComponent.ModelCallbacks {
        override fun onAllowed() {
            analyticsEventHandler.send(OnboardingEntryEvent.Biometric(OnboardingEntryEvent.Biometric.State.On))
            doIfVisa {
                analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.SuccessScreenOpened)
            }
            stackNavigation.replaceAll(
                OnboardingRoute.Done(
                    mode = doneMode,
                    onDone = ::exitComponentScreen,
                ),
            )
        }

        override fun onDenied() {
            analyticsEventHandler.send(OnboardingEntryEvent.Biometric(OnboardingEntryEvent.Biometric.State.Off))
            doIfVisa {
                analyticsEventHandler.send(OnboardingVisaAnalyticsEvent.SuccessScreenOpened)
            }
            stackNavigation.replaceAll(
                OnboardingRoute.Done(
                    mode = doneMode,
                    onDone = ::exitComponentScreen,
                ),
            )
        }
    }

    private fun exitComponentScreen() {
        // new flow
        if (hotWalletFeatureToggles.isHotWalletEnabled) {
            modelScope.launch {
                if (userWalletsListRepository.userWalletsSync().isEmpty()) {
                    router.replaceAll(AppRoute.Home())
                } else {
                    router.replaceAll(AppRoute.Wallet)
                }
            }
            return
        }

        // legacy flow
        if (userWalletsListManager.hasUserWallets) {
            val isLocked = runCatching { userWalletsListManager.asLockable()?.isLockedSync!! }.getOrElse { false }

            if (isLocked) {
                router.replaceAll(AppRoute.Welcome())
            } else {
                router.replaceAll(AppRoute.Wallet)
            }
        } else {
            router.replaceAll(AppRoute.Home())
        }
    }

    private inline fun doIfVisa(function: () -> Unit) {
        if (params.scanResponse.productType == ProductType.Visa) {
            function()
        }
    }

    inner class OnboardingTwinModelCallbacks : OnboardingTwinComponent.ModelCallbacks {
        override fun onDone() {
            navigateToFinalScreenFlow()
        }

        override fun onBack() {
            router.pop()
        }
    }
}