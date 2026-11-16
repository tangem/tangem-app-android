package com.tangem.features.details.model

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.wallets.analytics.Settings
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.*
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class DetailsModel @Inject constructor(
    socialsBuilder: SocialsBuilder,
    paramsContainer: ParamsContainer,
    private val itemsBuilder: ItemsBuilder,
    private val appInfoProvider: AppInfoProvider,
    private val checkIsWalletConnectAvailableUseCase: CheckIsWalletConnectAvailableUseCase,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val tangemPayEligibilityManager: TangemPayEligibilityManager,
) : Model() {

    private val params: DetailsComponent.Params = paramsContainer.require()

    private val items: MutableStateFlow<ImmutableList<DetailsItemUM>>

    val state: MutableStateFlow<DetailsUM>

    init {
        analyticsEventHandler.send(Settings.ScreenOpened())

        val isWalletConnectAvailable = runBlocking {
            // danger region, this works immediately, but will be refactored later with WC
            checkIsWalletConnectAvailableUseCase(params.userWalletId).getOrElse { throwable ->
                TangemLogger.w("Unable to check WalletConnect availability: $throwable")

                false
            }
        }

        items = MutableStateFlow(
            itemsBuilder.buildAll(
                isWalletConnectAvailable = isWalletConnectAvailable,
                hasAnyMobileWallet = getWalletsUseCase.invokeSync().any { it is UserWallet.Hot },
                userWalletId = params.userWalletId,
                onSupportClick = ::onContactSupportClick,
                onBuyClick = ::onBuyClick,
            ),
        )

        state = MutableStateFlow(
            value = DetailsUM(
                items = items.value,
                footer = DetailsFooterUM(
                    socials = socialsBuilder.buildAll(),
                    appVersion = getAppVersion(),
                ),
                selectContactSupportTypeBSConfig = TangemBottomSheetConfig.Empty,
                popBack = router::pop,
            ),
        )

        items
            .onEach(::updateState)
            .launchIn(modelScope)
    }

    private fun sendFeedback() {
        modelScope.launch {
            val selectedUserWallet = getSelectedWalletSyncUseCase().getOrNull()
                ?: error("Selected wallet is null")

            val metaInfo = getWalletMetaInfoUseCase(selectedUserWallet.walletId).getOrNull() ?: return@launch

            analyticsEventHandler.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.Settings))
            sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }

    private fun onContactSupportClick() {
        showContactSupportChooserBS()
    }

    private fun showContactSupportChooserBS() {
        state.update { current ->
            current.copy(
                selectContactSupportTypeBSConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = ::hideContactSupportChooserBS,
                    content = SelectContactSupportTypeBS(
                        onOptionClick = { option ->
                            hideContactSupportChooserBS()
                            when (option) {
                                SelectContactSupportTypeBS.Option.Mail -> sendFeedback()
                                SelectContactSupportTypeBS.Option.Chat -> {
                                    analyticsEventHandler.send(Settings.ButtonOpenChat())
                                    openUseDesk()
                                }
                            }
                        },
                    ),
                ),
            )
        }
    }

    private fun hideContactSupportChooserBS() {
        state.update { current ->
            current.copy(
                selectContactSupportTypeBSConfig = current.selectContactSupportTypeBSConfig.copy(isShown = false),
            )
        }
    }

    private fun openUseDesk() {
        modelScope.launch {
            val userWallet = getSelectedWalletSyncUseCase().getOrNull() ?: error("Selected wallet is null")
            val metaInfo = getWalletMetaInfoUseCase.invoke(userWallet.walletId).getOrNull() ?: return@launch
            router.push(AppRoute.Usedesk(metaInfo))
        }
    }

    private fun onBuyClick() {
        modelScope.launch {
            analyticsEventHandler.send(Basic.ButtonBuy(source = AnalyticsParam.ScreensSources.Settings))

            val url = generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Settings)
            urlOpener.openUrl(url)
        }
    }

    private fun updateState(items: ImmutableList<DetailsItemUM>) {
        state.update { prevState ->
            prevState.copy(items = items)
        }
    }

    fun onResume() {
        updateTangemPayItem()
    }

    private fun updateTangemPayItem() {
        modelScope.launch {
            val isEligible =
                tangemPayEligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true).isNotEmpty()
            if (isEligible) {
                if (!itemsBuilder.hasTangemPayItem(items.value)) {
                    analyticsEventHandler.send(TangemPayAnalyticsEvents.PermanentButtonShowed())
                }
                items.update { itemsBuilder.addTangemPayItem(items = it, onClick = ::onTangemPayItemClicked) }
            } else {
                items.update { itemsBuilder.removeTangemPayItem(items = it) }
            }
        }
    }

    private fun onTangemPayItemClicked() {
        analyticsEventHandler.send(TangemPayAnalyticsEvents.DetailsVisaPermanentButtonClicked())
        router.push(AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.FromBannerInSettings))
    }

    private fun getAppVersion(): String = "${appInfoProvider.appVersion} (${appInfoProvider.appVersionCode})"
}