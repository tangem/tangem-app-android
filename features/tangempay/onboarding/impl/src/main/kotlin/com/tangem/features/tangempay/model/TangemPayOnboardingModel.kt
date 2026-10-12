package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent
import com.tangem.features.tangempay.model.transformers.TangemPayOnboardingButtonLoadingTransformer
import com.tangem.features.tangempay.ui.TangemPayOnboardingNavigation
import com.tangem.features.tangempay.ui.TangemPayOnboardingScreenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.utils.transformer.update as transformerUpdate

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class TangemPayOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    private val router: Router,
    private val repository: OnboardingRepository,
    private val urlOpener: UrlOpener,
    private val eligibilityManager: TangemPayEligibilityManager,
    private val onboardingStarter: TangemPayOnboardingStarter,
    private val featureToggles: TangemPayFeatureToggles,
    private val userWalletsListRepository: UserWalletsListRepository,
) : Model(), TangemPayWalletSelectorComponent.Listener {

    private val params = paramsContainer.require<TangemPayOnboardingComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<TangemPayOnboardingNavigation> = SlotNavigation()

    val uiState: StateFlow<TangemPayOnboardingScreenState>
        field = MutableStateFlow(getInitialState())

    private val starterMode = when (params) {
        is TangemPayOnboardingComponent.Params.ContinueOnboarding,
        is TangemPayOnboardingComponent.Params.HotWalletOnboarding,
        -> TangemPayOnboardingStarter.Mode.ContinueToKyc
        is TangemPayOnboardingComponent.Params.Deeplink,
        is TangemPayOnboardingComponent.Params.FromBannerInSettings,
        is TangemPayOnboardingComponent.Params.FromBannerOnMain,
        is TangemPayOnboardingComponent.Params.MobileOnboardingDeeplink,
        -> TangemPayOnboardingStarter.Mode.StartOnboarding
    }

    private val starterCallbacks = TangemPayOnboardingStarter.Callbacks(
        onLoadingChange = { isLoading ->
            uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = isLoading))
        },
        onExit = ::back,
        onWalletSelectorRequested = ::openWalletSelector,
    )

    init {
        analytics.send(TangemPayAnalyticsEvents.ActivationScreenOpened())
        init()
    }

    private fun init() {
        modelScope.launch {
            when (params) {
                is TangemPayOnboardingComponent.Params.Deeplink -> {
                    repository.validateDeeplink(params.deeplink)
                        .onRight { isValid -> if (isValid) onDeeplinkValidated() else showNotAvailable() }
                        .onLeft { back() }
                }
                is TangemPayOnboardingComponent.Params.ContinueOnboarding -> {
                    onboardingStarter.openKyc(userWalletId = params.userWalletId)
                }
                is TangemPayOnboardingComponent.Params.HotWalletOnboarding -> {
                    onboardingStarter.startOnboarding(
                        userWalletId = params.userWalletId,
                        campaignKey = null,
                        callbacks = starterCallbacks,
                    )
                }
                // FromBanner* and mobile-onboard skip the backend validation that Params.Deeplink performs.
                is TangemPayOnboardingComponent.Params.FromBannerInSettings,
                is TangemPayOnboardingComponent.Params.FromBannerOnMain,
                -> showOnboarding()
                is TangemPayOnboardingComponent.Params.MobileOnboardingDeeplink -> showOnboardingIfOfferAvailable()
            }
        }
    }

    private fun showOnboarding() {
        uiState.update { state ->
            TangemPayOnboardingScreenState.Content(
                onBack = state.onBack,
                onTermsClick = ::onTermsClick,
                buttonConfig = TangemPayOnboardingScreenState.Content.ButtonConfig(
                    isLoading = false,
                    onClick = ::onGetCardClick,
                ),
            )
        }
    }

    private fun showNotAvailable() {
        uiState.update { state -> TangemPayOnboardingScreenState.NotAvailable(onBack = state.onBack) }
    }

    private suspend fun onDeeplinkValidated() {
        if (userWalletsListRepository.userWalletsSync().isEmpty()) {
            router.replaceAll(AppRoute.TangemPayHotWalletOnboarding)
            return
        }
        showOnboardingIfOfferAvailable()
    }

    private suspend fun showOnboardingIfOfferAvailable() {
        val offerWalletsIds = eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true)
        if (offerWalletsIds.isEmpty()) {
            TangemLogger.i("[TangemPay] Onboarding deeplink dropped: no wallet can take the offer")
            back()
        } else {
            showOnboarding()
        }
    }

    private fun onTermsClick() {
        analytics.send(TangemPayAnalyticsEvents.ViewTermsClicked())
        urlOpener.openUrl(TangemPayConstants.termsAndLimitsLink())
    }

    private fun onGetCardClick() {
        analytics.send(TangemPayAnalyticsEvents.GetCardClicked())
        if (featureToggles.isResidencyStepEnabled) {
            router.push(
                AppRoute.TangemPayEligibility(
                    product = TangemPayProduct.PAYMENT_ACCOUNT,
                    userWalletId = null,
                ),
            )
        } else if (params is TangemPayOnboardingComponent.Params.Deeplink ||
            params is TangemPayOnboardingComponent.Params.MobileOnboardingDeeplink
        ) {
            modelScope.launch {
                proceedWithWallets(
                    walletsIds = eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true),
                )
            }
        } else {
            // if user came from banner in settings and already is a paera customer -> exclude this wallet
            val shouldExcludePaeraCustomers = params is TangemPayOnboardingComponent.Params.FromBannerInSettings
            modelScope.launch {
                val eligibleWalletsIds = eligibilityManager
                    .getEligibleWallets(
                        shouldExcludePaeraCustomers = shouldExcludePaeraCustomers,
                        entryPoint = params.toEntryPointOrNull(),
                    )
                    .map { it.walletId }
                if (eligibleWalletsIds.isEmpty()) {
                    back()
                    return@launch
                }
                when (params) {
                    is TangemPayOnboardingComponent.Params.ContinueOnboarding -> {
                        checkCustomerInfo(params.userWalletId)
                    }
                    else -> {
                        proceedWithWallets(eligibleWalletsIds)
                    }
                }
            }
        }
    }

    private suspend fun proceedWithWallets(walletsIds: List<UserWalletId>) {
        onboardingStarter.proceedWithWallets(
            walletsIds = walletsIds,
            mode = starterMode,
            campaignKey = null,
            callbacks = starterCallbacks,
        )
    }

    private suspend fun checkCustomerInfo(userWalletId: UserWalletId) {
        onboardingStarter.checkCustomerInfo(
            userWalletId = userWalletId,
            mode = starterMode,
            campaignKey = null,
            callbacks = starterCallbacks,
        )
    }

    private fun openWalletSelector(walletsIds: List<UserWalletId>) {
        analytics.send(TangemPayAnalyticsEvents.ChooseWalletPopup())
        bottomSheetNavigation.activate(TangemPayOnboardingNavigation.WalletSelector(walletsIds))
    }

    private fun back() {
        router.pop()
    }

    private fun getInitialState(): TangemPayOnboardingScreenState {
        return TangemPayOnboardingScreenState.Loading(onBack = ::back)
    }

    override fun onWalletSelected(userWalletId: UserWalletId) {
        bottomSheetNavigation.dismiss()
        modelScope.launch { checkCustomerInfo(userWalletId) }
    }

    override fun onWalletSelectorDismiss() {
        bottomSheetNavigation.dismiss()
    }

    private fun TangemPayOnboardingComponent.Params.toEntryPointOrNull() = when (this) {
        is TangemPayOnboardingComponent.Params.FromBannerInSettings -> TangemPayEntryPoint.DETAILS
        is TangemPayOnboardingComponent.Params.FromBannerOnMain -> TangemPayEntryPoint.BANNER

        is TangemPayOnboardingComponent.Params.Deeplink,
        is TangemPayOnboardingComponent.Params.ContinueOnboarding,
        is TangemPayOnboardingComponent.Params.HotWalletOnboarding,
        is TangemPayOnboardingComponent.Params.MobileOnboardingDeeplink,
        -> null
    }
}