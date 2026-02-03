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
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.CustomerInfo.KycStatus
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.components.TangemPayOnboardingComponent
import com.tangem.features.tangempay.components.WalletSelectorListener
import com.tangem.features.tangempay.model.transformers.TangemPayOnboardingButtonLoadingTransformer
import com.tangem.features.tangempay.ui.TangemPayOnboardingNavigation
import com.tangem.features.tangempay.ui.TangemPayOnboardingScreenState
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val produceInitialDataUseCase: ProduceTangemPayInitialDataUseCase,
    private val urlOpener: UrlOpener,
    private val eligibilityManager: TangemPayEligibilityManager,
) : Model(), WalletSelectorListener {

    private val params = paramsContainer.require<TangemPayOnboardingComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<TangemPayOnboardingNavigation> = SlotNavigation()

    val uiState: StateFlow<TangemPayOnboardingScreenState>
        field = MutableStateFlow(getInitialState())

    init {
        analytics.send(TangemPayAnalyticsEvents.ActivationScreenOpened())
        init()
    }

    private fun init() {
        modelScope.launch {
            when (params) {
                is TangemPayOnboardingComponent.Params.Deeplink -> {
                    repository.validateDeeplink(params.deeplink)
                        .onRight { isValid -> if (isValid) showOnboarding() else back() }
                        .onLeft { back() }
                }
                is TangemPayOnboardingComponent.Params.ContinueOnboarding -> {
                    openKyc(userWalletId = params.userWalletId)
                }
                is TangemPayOnboardingComponent.Params.FromBannerInSettings,
                is TangemPayOnboardingComponent.Params.FromBannerOnMain,
                -> showOnboarding()
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

    private fun checkCustomerInfo(userWalletId: UserWalletId) {
        modelScope.launch {
            uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = true))
            repository.getCustomerInfo(userWalletId = userWalletId)
                .onRight { customerInfo ->
                    uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = false))
                    when {
                        customerInfo.kycStatus != KycStatus.APPROVED -> {
                            when (params) {
                                is TangemPayOnboardingComponent.Params.ContinueOnboarding -> openKyc(userWalletId)
                                else -> startOnboarding(userWalletId)
                            }
                        }
                        else -> back()
                    }
                }
                .onLeft { startOnboarding(userWalletId) }
        }
    }

    private fun onTermsClick() {
        analytics.send(TangemPayAnalyticsEvents.ViewTermsClicked())
        urlOpener.openUrl(TangemPayConstants.TERMS_AND_LIMITS_LINK)
    }

    private fun onGetCardClick() {
        analytics.send(TangemPayAnalyticsEvents.GetCardClicked())
        // if user came from deeplink or banner in settings and already is a paera customer -> exclude this wallet
        val shouldExcludePaeraCustomers = params is TangemPayOnboardingComponent.Params.FromBannerInSettings ||
            params is TangemPayOnboardingComponent.Params.Deeplink
        modelScope.launch {
            val eligibleWalletsIds = eligibilityManager
                .getEligibleWallets(shouldExcludePaeraCustomers = shouldExcludePaeraCustomers)
                .map { it.walletId }
            if (eligibleWalletsIds.isEmpty()) {
                back()
                return@launch
            }
            when (params) {
                is TangemPayOnboardingComponent.Params.ContinueOnboarding -> {
                    checkCustomerInfo(params.userWalletId)
                }
                is TangemPayOnboardingComponent.Params.FromBannerOnMain,
                is TangemPayOnboardingComponent.Params.Deeplink,
                is TangemPayOnboardingComponent.Params.FromBannerInSettings,
                -> {
                    openWalletSelectorIfNeeds(eligibleWalletsIds)
                }
            }
        }
    }

    private fun openWalletSelectorIfNeeds(eligibleWalletsIds: List<UserWalletId>) {
        if (eligibleWalletsIds.size == 1) {
            checkCustomerInfo(userWalletId = eligibleWalletsIds[0])
        } else {
            analytics.send(TangemPayAnalyticsEvents.ChooseWalletPopup())
            bottomSheetNavigation.activate(TangemPayOnboardingNavigation.WalletSelector(eligibleWalletsIds))
        }
    }

    private fun startOnboarding(userWalletId: UserWalletId) {
        uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = true))
        modelScope.launch {
            val result = produceInitialDataUseCase(userWalletId)
            if (result.isLeft()) {
                val errorMessage = result.leftOrNull()?.message ?: "Unknown error"
                Timber.e("Error producing initial data: $errorMessage")
                uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = false))
                return@launch
            }
            repository.getCustomerInfo(userWalletId = userWalletId)
                .fold(
                    ifLeft = { error ->
                        Timber.e("Error getCustomerInfo: ${error.errorCode}")
                        uiState.transformerUpdate(TangemPayOnboardingButtonLoadingTransformer(isLoading = false))
                    },
                    ifRight = { customerInfo ->
                        if (customerInfo.kycStatus == KycStatus.APPROVED) {
                            back()
                        } else {
                            openKyc(userWalletId)
                        }
                    },
                )
        }
    }

    private fun openKyc(userWalletId: UserWalletId) {
        router.replaceAll(
            AppRoute.Wallet,
            AppRoute.Kyc(userWalletId = userWalletId),
        )
    }

    private fun back() {
        router.pop()
    }

    private fun getInitialState(): TangemPayOnboardingScreenState {
        return TangemPayOnboardingScreenState.Loading(onBack = ::back)
    }

    override fun onWalletSelected(userWalletId: UserWalletId) {
        bottomSheetNavigation.dismiss()
        checkCustomerInfo(userWalletId)
    }

    override fun onWalletSelectorDismiss() {
        bottomSheetNavigation.dismiss()
    }
}