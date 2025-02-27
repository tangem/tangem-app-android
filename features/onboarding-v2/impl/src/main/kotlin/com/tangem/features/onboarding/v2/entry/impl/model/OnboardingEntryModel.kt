package com.tangem.features.onboarding.v2.entry.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.navigate
import com.tangem.common.routing.AppRoute
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
import com.tangem.features.onboarding.v2.TitleProvider
import com.tangem.features.onboarding.v2.entry.OnboardingEntryComponent
import com.tangem.features.onboarding.v2.entry.impl.routing.OnboardingRoute
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class OnboardingEntryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val tangemSdkManager: TangemSdkManager,
    private val settingsRepository: SettingsRepository,
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
            else -> error("Unsupported")
        }
    }

    private fun onMultiWalletOnboardingDone(userWallet: UserWallet) {
        if (userWallet.scanResponse.cardTypesResolver.isMultiwalletAllowed()) {
            stackNavigation.navigate {
                listOf(OnboardingRoute.ManageTokens(userWallet))
            }
        } else {
            stackNavigation.navigate {
                listOf(OnboardingRoute.Done(onDone = ::navigateToWalletScreen))
            }
        }
    }

    fun onManageTokensDone() {
        stackNavigation.navigate {
            listOf(OnboardingRoute.Done(onDone = ::navigateToWalletScreen))
        }
    }

    private fun onVisaOnboardingDone() {
        stackNavigation.navigate {
            listOf(OnboardingRoute.Done(onDone = ::navigateToWalletScreen))
        }
    }

    private fun navigateToWalletScreen() {
        modelScope.launch(NonCancellable) {
            router.replaceAll(AppRoute.Wallet)
            if (tangemSdkManager.checkCanUseBiometry() && settingsRepository.shouldShowSaveUserWalletScreen()) {
                delay(timeMillis = 1_800)
                router.push(AppRoute.SaveWallet)
            }
        }
    }
}