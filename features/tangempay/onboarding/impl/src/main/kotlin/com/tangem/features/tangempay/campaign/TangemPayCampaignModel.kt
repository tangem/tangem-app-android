package com.tangem.features.tangempay.campaign

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst
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
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.repository.TangemPayCampaignRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.features.tangempay.common.TangemPayCampaignMessages
import com.tangem.features.tangempay.components.TangemPayCampaignComponent
import com.tangem.features.tangempay.components.WalletSelectorListener
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
    private val onboardingRepository: OnboardingRepository,
    private val campaignRepository: TangemPayCampaignRepository,
    private val eligibilityManager: TangemPayEligibilityManager,
    private val getCustomerOffersUseCase: GetCustomerOffersUseCase,
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase,
    private val onboardingStarter: TangemPayOnboardingStarter,
) : Model(), WalletSelectorListener {

    private val params = paramsContainer.require<TangemPayCampaignComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<TangemPayOnboardingNavigation> = SlotNavigation()

    val uiState: StateFlow<TangemPayCampaignUM>
        field = MutableStateFlow<TangemPayCampaignUM>(TangemPayCampaignUM.Loading)

    private val starterCallbacks = TangemPayOnboardingStarter.Callbacks(
        onLoadingChange = ::setCtaLoading,
        onExit = ::onClose,
        onWalletSelectorRequested = { walletsIds ->
            bottomSheetNavigation.activate(TangemPayOnboardingNavigation.WalletSelector(walletsIds))
        },
    )

    init {
        TangemLogger.i("[TangemPay][Campaign] Opened campaign container: ${params.campaignKey}")
        resolveCampaign()
    }

    fun onClose() {
        modelScope.launch {
            clearAppsFlyerDeeplinkUseCase()
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
            val isValid = onboardingRepository.validateDeeplink(campaignDeeplink())
                .getOrElse { error -> return@launch showError(step = "validateDeeplink", error = error) }
            if (!isValid) {
                TangemLogger.i("[TangemPay][Campaign] Campaign is not valid: ${params.campaignKey}")
                return@launch showUnavailableDialog()
            }
            val campaign = campaignRepository.getCampaign(params.campaignKey)
                .getOrElse { error -> return@launch showError(step = "getCampaign", error = error) }
            val payWalletId = findWalletWithPay()
                .getOrElse { error -> return@launch showError(step = "hasTangemPayInWallet", error = error) }

            if (payWalletId != null) {
                resolveIssueCard(userWalletId = payWalletId, campaign = campaign)
            } else {
                showLanding(campaign)
            }
        }
    }

    private fun campaignDeeplink(): String = "${DeeplinkConst.TANGEM_SCHEME}://marketing/${params.campaignKey}"

    private suspend fun findWalletWithPay(): Either<VisaApiError, UserWalletId?> = either {
        eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = false)
            .firstOrNull { walletId -> onboardingRepository.hasTangemPayInWallet(walletId).bind() }
    }

    private suspend fun resolveIssueCard(userWalletId: UserWalletId, campaign: TangemPayCampaign) {
        getCustomerOffersUseCase.campaignOffer(userWalletId = userWalletId, deeplinkValue = params.campaignKey).fold(
            ifLeft = { error -> showError(step = "getOffers", error = error) },
            ifRight = { offer ->
                if (offer == null) {
                    TangemLogger.i("[TangemPay][Campaign] No campaign offer for key=${params.campaignKey}")
                    showUnavailableDialog()
                } else {
                    showIssueCard(userWalletId = userWalletId, campaign = campaign, offer = offer)
                }
            },
        )
    }

    @Suppress("UnusedParameter")
    private fun showIssueCard(userWalletId: UserWalletId, campaign: TangemPayCampaign, offer: Offer) {
        TangemLogger.i("[TangemPay][Campaign] Campaign offer resolved for a Payment account, issue screen pending")
    }

    private suspend fun showLanding(campaign: TangemPayCampaign) {
        val mode = if (userWalletsListRepository.userWalletsSync().isEmpty()) {
            TangemPayCampaignUM.Landing.Mode.NoWallet
        } else {
            TangemPayCampaignUM.Landing.Mode.NoPay
        }
        uiState.value = TangemPayCampaignUM.Landing(
            cardArtUrl = campaign.cardArtUrl,
            mode = mode,
            isCtaLoading = false,
            onGetCardClick = ::onGetCardClick,
            onTermsClick = ::onTermsClick,
            onCloseClick = ::onClose,
        )
    }

    private fun showError(step: String, error: Any) {
        TangemLogger.e("[TangemPay][Campaign] Failed to resolve campaign at $step: $error")
        uiState.value = TangemPayCampaignUM.Error(onRetry = ::resolveCampaign, onClose = ::onClose)
    }

    private fun showUnavailableDialog() {
        uiMessageSender.send(TangemPayCampaignMessages.unavailableDialog(onDismiss = ::onClose))
    }

    private fun onGetCardClick() {
        val landing = uiState.value as? TangemPayCampaignUM.Landing ?: return
        if (landing.isCtaLoading) return

        modelScope.launch {
            when (landing.mode) {
                TangemPayCampaignUM.Landing.Mode.NoWallet -> {
                    setCtaLoading(true)
                    val userWallet = onboardingStarter.createHotWallet(
                        campaignKey = params.campaignKey,
                        onLoadingChange = ::setCtaLoading,
                    ) ?: return@launch
                    onboardingStarter.startOnboarding(
                        userWalletId = userWallet.walletId,
                        campaignKey = params.campaignKey,
                        callbacks = starterCallbacks,
                    )
                }
                TangemPayCampaignUM.Landing.Mode.NoPay -> onboardingStarter.proceedWithWallets(
                    walletsIds = eligibilityManager.getPossibleWalletsIds(shouldExcludePaeraCustomers = true),
                    mode = TangemPayOnboardingStarter.Mode.StartOnboarding,
                    campaignKey = params.campaignKey,
                    callbacks = starterCallbacks,
                )
            }
        }
    }

    private fun onTermsClick() {
        urlOpener.openUrl(TangemPayConstants.termsAndLimitsLink())
    }

    private fun setCtaLoading(isLoading: Boolean) {
        uiState.update { state ->
            if (state is TangemPayCampaignUM.Landing) state.copy(isCtaLoading = isLoading) else state
        }
    }
}