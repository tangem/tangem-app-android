package com.tangem.features.tangempay.campaign

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.TangemPayCampaign
import com.tangem.domain.pay.model.TangemPayCampaignResolution
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.pay.usecase.ResolveTangemPayCampaignUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.common.TangemPayCampaignMessages
import com.tangem.features.tangempay.components.TangemPayCampaignComponent
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent
import com.tangem.features.tangempay.model.TangemPayOnboardingStarter
import com.tangem.features.tangempay.ui.TangemPayOnboardingNavigation
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class TangemPayCampaignModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val eligibilityManager: TangemPayEligibilityManager,
    private val resolveTangemPayCampaignUseCase: ResolveTangemPayCampaignUseCase,
    private val getCustomerOffersUseCase: GetCustomerOffersUseCase,
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase,
    private val onboardingStarter: TangemPayOnboardingStarter,
    private val analytics: AnalyticsEventHandler,
) : Model(), TangemPayWalletSelectorComponent.Listener {

    private val params = paramsContainer.require<TangemPayCampaignComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<TangemPayOnboardingNavigation> = SlotNavigation()

    val childNavigation: SlotNavigation<TangemPayCampaignChild> = SlotNavigation()

    val uiState: StateFlow<TangemPayCampaignUM>
        field = MutableStateFlow<TangemPayCampaignUM>(TangemPayCampaignUM.Loading)

    private var campaignCardArtUrl: String? = null

    private var createdHotWalletId: UserWalletId? = null

    private val starterCallbacks = TangemPayOnboardingStarter.Callbacks(
        onLoadingChange = ::setCtaLoading,
        onExit = ::onClose,
        onWalletSelectorRequested = { walletsIds ->
            setCtaLoading(false)
            bottomSheetNavigation.activate(TangemPayOnboardingNavigation.WalletSelector(walletsIds))
        },
        onCustomerAlreadyApproved = ::proceedToCampaignCard,
    )

    init {
        TangemLogger.i("[TangemPay][Campaign] Opened campaign container: ${params.campaignKey}")
        modelScope.launch { clearAppsFlyerDeeplinkUseCase() }
        resolveCampaign()
    }

    fun onClose() {
        modelScope.launch {
            if (userWalletsListRepository.userWalletsSync().isEmpty()) {
                router.replaceAll(AppRoute.Home())
            } else {
                router.replaceAll(AppRoute.Wallet)
            }
        }
    }

    override fun onWalletSelected(userWalletId: UserWalletId) {
        bottomSheetNavigation.dismiss()
        modelScope.launch {
            onboardingStarter.checkCustomerInfo(
                userWalletId = userWalletId,
                mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                campaignKey = params.campaignKey,
                callbacks = starterCallbacks,
            )
        }
    }

    override fun onWalletSelectorDismiss() {
        bottomSheetNavigation.dismiss()
    }

    private fun resolveCampaign() {
        uiState.value = TangemPayCampaignUM.Loading
        modelScope.launch {
            resolveTangemPayCampaignUseCase(params.campaignKey).fold(
                ifLeft = { showError() },
                ifRight = { resolution ->
                    when (resolution) {
                        is TangemPayCampaignResolution.IssueCard -> {
                            analytics.send(TangemPayAnalyticsEvents.CobrandOnboardingExistPay())
                            campaignCardArtUrl = resolution.campaign.cardArtUrl
                            showIssueCard(resolution.userWalletId, resolution.offer)
                        }
                        is TangemPayCampaignResolution.Landing -> showLanding(resolution.campaign)
                        TangemPayCampaignResolution.Unavailable -> showUnavailableDialog()
                    }
                },
            )
        }
    }

    private suspend fun proceedToCampaignCard(userWalletId: UserWalletId) {
        val offer = getCustomerOffersUseCase
            .campaignOffer(userWalletId = userWalletId, deeplinkValue = params.campaignKey)
            .getOrElse { error ->
                TangemLogger.e("[TangemPay][Campaign] Failed to load campaign offer: $error")
                setCtaLoading(false)
                uiMessageSender.send(TangemPayCampaignMessages.retryableErrorSnackbar())
                return
            }
        setCtaLoading(false)
        if (offer == null) {
            TangemLogger.i("[TangemPay][Campaign] No campaign offer for key=${params.campaignKey}")
            showUnavailableDialog()
            return
        }
        showIssueCard(userWalletId, offer)
    }

    private fun showIssueCard(userWalletId: UserWalletId, offer: Offer) {
        childNavigation.activate(
            TangemPayCampaignChild.IssueCard(
                userWalletId = userWalletId,
                cardArtUrl = offer.mainImageUrl ?: campaignCardArtUrl,
                feeAmount = offer.fee.amount,
                feeCurrency = offer.fee.currency,
            ),
        )
    }

    private suspend fun showLanding(campaign: TangemPayCampaign) {
        campaignCardArtUrl = campaign.cardArtUrl
        val mode = if (userWalletsListRepository.userWalletsSync().isEmpty()) {
            TangemPayCampaignUM.Landing.Mode.NoWallet
        } else {
            TangemPayCampaignUM.Landing.Mode.NoPay
        }
        analytics.send(
            when (mode) {
                TangemPayCampaignUM.Landing.Mode.NoWallet -> TangemPayAnalyticsEvents.CobrandOnboardingNewHotWallet()
                TangemPayCampaignUM.Landing.Mode.NoPay -> TangemPayAnalyticsEvents.CobrandOnboardingNotInPay()
            },
        )
        uiState.value = TangemPayCampaignUM.Landing(
            cardArtUrl = campaign.cardArtUrl,
            mode = mode,
            isCtaLoading = false,
            onGetCardClick = ::onGetCardClick,
            onTermsOfServiceClick = ::onTermsOfServiceClick,
            onPrivacyPolicyClick = ::onPrivacyPolicyClick,
            onCloseClick = ::onClose,
        )
    }

    private fun showError() {
        uiState.value = TangemPayCampaignUM.Error(onRetry = ::resolveCampaign, onClose = ::onClose)
    }

    private fun showUnavailableDialog() {
        uiMessageSender.send(TangemPayCampaignMessages.unavailableDialog(onDismiss = ::onClose))
    }

    private fun onGetCardClick() {
        val landing = uiState.value as? TangemPayCampaignUM.Landing ?: return
        if (landing.isCtaLoading) return
        setCtaLoading(true)

        modelScope.launch {
            when (landing.mode) {
                TangemPayCampaignUM.Landing.Mode.NoWallet -> {
                    val userWalletId = createdHotWalletId
                        ?: onboardingStarter.createHotWallet(onLoadingChange = ::setCtaLoading)?.walletId
                        ?: return@launch
                    createdHotWalletId = userWalletId
                    onboardingStarter.startOnboarding(
                        userWalletId = userWalletId,
                        campaignKey = params.campaignKey,
                        callbacks = starterCallbacks,
                    )
                }
                TangemPayCampaignUM.Landing.Mode.NoPay -> {
                    val walletsIds = eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = false)
                    if (walletsIds.isEmpty()) {
                        TangemLogger.i("[TangemPay][Campaign] No eligible wallets for the campaign, closing")
                        setCtaLoading(false)
                        onClose()
                        return@launch
                    }
                    onboardingStarter.proceedWithWallets(
                        walletsIds = walletsIds,
                        mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                        campaignKey = params.campaignKey,
                        callbacks = starterCallbacks,
                    )
                }
            }
        }
    }

    private fun onTermsOfServiceClick() {
        router.push(AppRoute.Disclaimer(isTosAccepted = true))
    }

    private fun onPrivacyPolicyClick() {
        urlOpener.openUrl(TangemPayConstants.privacyPolicyLink())
    }

    private fun setCtaLoading(isLoading: Boolean) {
        uiState.update { state ->
            if (state is TangemPayCampaignUM.Landing) state.copy(isCtaLoading = isLoading) else state
        }
    }
}