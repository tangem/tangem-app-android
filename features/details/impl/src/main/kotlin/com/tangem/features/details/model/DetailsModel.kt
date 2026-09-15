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
import com.tangem.domain.card.common.TapWorkarounds.isVisa
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.TangemPayEntryPoint
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.domain.moneyaccount.model.MoneyAccountEntryPoint
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountEligibilityUseCase
import com.tangem.domain.walletconnect.CheckIsWalletConnectAvailableUseCase
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.domain.wallets.analytics.Settings
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.details.component.DetailsComponent
import com.tangem.features.details.entity.DetailsFooterUM
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.entity.DetailsUM
import com.tangem.features.details.entity.SelectContactSupportTypeBS
import com.tangem.features.details.entity.SelectEmailFeedbackTypeBS
import com.tangem.features.details.utils.ItemsBuilder
import com.tangem.features.details.utils.SocialsBuilder
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
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
    private val getTangemPayCustomerIdUseCase: GetTangemPayCustomerIdUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
    private val tangemPayEligibilityManager: TangemPayEligibilityManager,
    private val getMoneyAccountEligibilityUseCase: GetMoneyAccountEligibilityUseCase,
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

        addTangemPayItemIfEligible()
        addMoneyAccountItemIfEligible()

        state = MutableStateFlow(
            value = DetailsUM(
                items = items.value,
                footer = DetailsFooterUM(
                    socials = socialsBuilder.buildAll(),
                    appVersion = getAppVersion(),
                ),
                selectFeedbackEmailTypeBSConfig = TangemBottomSheetConfig.Empty,
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
            val userWallets = getWalletsUseCase.invokeSync()

            val selectedUserWallet = getSelectedWalletSyncUseCase().getOrNull()
                ?: error("Selected wallet is null")

            val metaInfo = getWalletMetaInfoUseCase(selectedUserWallet.walletId).getOrNull() ?: return@launch
            val visaCustomerId = getTangemPayCustomerIdUseCase(selectedUserWallet.walletId).getOrNull()

            val coldVisaPredicate = { userWallet: UserWallet ->
                userWallet is UserWallet.Cold && userWallet.scanResponse.card.isVisa && !visaCustomerId.isNullOrEmpty()
            }
            val hotWalletOrNotVisaPredicate = { userWallet: UserWallet ->
                userWallet !is UserWallet.Cold || userWallet.scanResponse.card.isVisa.not()
            }
            val feedbackType = when {
                userWallets.all(coldVisaPredicate) -> {
                    FeedbackEmailType.Visa.DirectUserRequest(
                        walletMetaInfo = metaInfo,
                        customerId = requireNotNull(visaCustomerId),
                    )
                }
                userWallets.all(hotWalletOrNotVisaPredicate) -> {
                    FeedbackEmailType.DirectUserRequest(
                        walletMetaInfo = metaInfo,
                    )
                }
                else -> {
                    showFeedbackEmailTypeOptionBS(
                        selectedWalletMetaInfo = metaInfo,
                        visaCustomerId = visaCustomerId,
                    )
                    return@launch
                }
            }

            analyticsEventHandler.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.Settings))
            sendFeedbackEmailUseCase(feedbackType)
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

    private fun showFeedbackEmailTypeOptionBS(selectedWalletMetaInfo: WalletMetaInfo, visaCustomerId: String?) {
        state.update { current ->
            current.copy(
                selectFeedbackEmailTypeBSConfig = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {
                        state.update {
                            current.copy(
                                selectFeedbackEmailTypeBSConfig = current.selectFeedbackEmailTypeBSConfig.copy(
                                    isShown = false,
                                ),
                            )
                        }
                    },
                    content = SelectEmailFeedbackTypeBS(
                        onOptionClick = { option ->
                            onEmailFeedbackTypeOptionSelected(
                                selectedWalletMetaInfo = selectedWalletMetaInfo,
                                option = option,
                                visaCustomerId = visaCustomerId,
                            )

                            state.update { details ->
                                val hiddenConfig = details.selectFeedbackEmailTypeBSConfig.copy(
                                    isShown = false,
                                )
                                details.copy(
                                    selectFeedbackEmailTypeBSConfig = hiddenConfig,
                                )
                            }
                        },
                    ),
                ),
            )
        }
    }

    private fun onEmailFeedbackTypeOptionSelected(
        selectedWalletMetaInfo: WalletMetaInfo,
        option: SelectEmailFeedbackTypeBS.Option,
        visaCustomerId: String?,
    ) {
        modelScope.launch {
            val feedbackType = when (option) {
                SelectEmailFeedbackTypeBS.Option.General -> {
                    if (selectedWalletMetaInfo.isVisa == false) {
                        FeedbackEmailType.DirectUserRequest(selectedWalletMetaInfo)
                    } else {
                        val userWallet = getWalletsUseCase.invokeSync()
                            .firstOrNull {
                                it is UserWallet.Hot || it is UserWallet.Cold && it.scanResponse.card.isVisa.not()
                            } ?: return@launch

                        val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
                        FeedbackEmailType.DirectUserRequest(metaInfo)
                    }
                }
                SelectEmailFeedbackTypeBS.Option.Visa -> {
                    if (selectedWalletMetaInfo.isVisa == true && !visaCustomerId.isNullOrEmpty()) {
                        FeedbackEmailType.Visa.DirectUserRequest(selectedWalletMetaInfo, visaCustomerId)
                    } else {
                        val userWallet = getWalletsUseCase.invokeSync()
                            .firstOrNull { it is UserWallet.Cold && it.scanResponse.card.isVisa }
                            ?: return@launch
                        val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
                        val customerId = getTangemPayCustomerIdUseCase(userWallet.walletId).getOrNull() ?: return@launch
                        FeedbackEmailType.Visa.DirectUserRequest(metaInfo, customerId)
                    }
                }
            }

            analyticsEventHandler.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.Settings))
            sendFeedbackEmailUseCase(feedbackType)
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

    private fun addTangemPayItemIfEligible() {
        modelScope.launch {
            val isEligible = if (tangemPayFeatureToggles.isResidencyStepEnabled) {
                tangemPayEligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true).isNotEmpty()
            } else {
                tangemPayEligibilityManager
                    .getEligibleWallets(
                        shouldExcludePaeraCustomers = true,
                        entryPoint = TangemPayEntryPoint.DETAILS,
                    )
                    .isNotEmpty()
            }
            if (isEligible) {
                analyticsEventHandler.send(TangemPayAnalyticsEvents.PermanentButtonShowed())
                items.update { itemsBuilder.addTangemPayItem(items = it, onClick = ::onTangemPayItemClicked) }
            }
        }
    }

    private fun onTangemPayItemClicked() {
        modelScope.launch {
            analyticsEventHandler.send(TangemPayAnalyticsEvents.DetailsVisaPermanentButtonClicked())
            if (tangemPayFeatureToggles.isResidencyStepEnabled) {
                router.push(AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.FromBannerInSettings))
                return@launch
            }
            val isEligible = tangemPayEligibilityManager.getTangemPayAvailability(TangemPayEntryPoint.DETAILS)
            if (isEligible) {
                router.push(AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.FromBannerInSettings))
            } else {
                items.update { itemsBuilder.removeTangemPayItem(it) }
            }
        }
    }

    private fun addMoneyAccountItemIfEligible() {
        modelScope.launch {
            val isMoneyAccountEnabled = moneyAccountFeatureToggles.isMoneyAccountsEnabled
            val eligibility = getMoneyAccountEligibilityUseCase(MoneyAccountEntryPoint.DETAILS)
            if (eligibility is MoneyAccountEligibility.Available && isMoneyAccountEnabled) {
                items.update { items ->
                    itemsBuilder.addMoneyAccountItem(
                        items = items,
                        onClick = ::onMoneyAccountItemClicked,
                    )
                }
            }
        }
    }

    private fun onMoneyAccountItemClicked() {
        modelScope.launch {
            when (val eligibility = getMoneyAccountEligibilityUseCase(MoneyAccountEntryPoint.DETAILS)) {
                is MoneyAccountEligibility.Available -> router.push(
                    AppRoute.MoneyAccountOnboarding(
                        AppRoute.MoneyAccountOnboarding.Mode.FromDetailsScreen(eligibility.wallets.first().walletId),
                    ),
                )
                MoneyAccountEligibility.NotAvailable -> items.update { itemsBuilder.removeMoneyAccountItem(it) }
            }
        }
    }

    private fun getAppVersion(): String = "${appInfoProvider.appVersion} (${appInfoProvider.appVersionCode})"
}