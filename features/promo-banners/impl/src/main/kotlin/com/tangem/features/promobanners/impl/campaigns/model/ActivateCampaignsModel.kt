package com.tangem.features.promobanners.impl.campaigns.model

import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.EnrollResult
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.TokenReward
import com.tangem.domain.promo.usecase.EnrollPromoCampaignUseCase
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.analytics.PromoCampaignsAnalyticsEvent
import com.tangem.features.promobanners.impl.campaigns.component.ActivateCampaignBottomSheetComponent
import com.tangem.features.promobanners.impl.campaigns.entity.ActivateCampaignUM
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignTypeToContentConverter
import com.tangem.features.promobanners.impl.campaigns.entity.FooterUM
import com.tangem.features.promobanners.impl.campaigns.entity.SelectedAccountUM
import com.tangem.features.promobanners.impl.campaigns.entity.TermsUM
import com.tangem.features.promobanners.impl.campaigns.entity.toPromoCampaignId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ActivateCampaignsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val enrollPromoCampaignUseCase: EnrollPromoCampaignUseCase,
    private val urlOpener: UrlOpener,
    @GlobalUiMessageSender private val messageSender: UiMessageSender,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<ActivateCampaignBottomSheetComponent.Params>()
    private val campaignType = params.campaignType
    private val accountIconConverter = AccountIconItemStateConverter(size = AccountIconSize.ExtraSmall)
    private var appCurrency: AppCurrency = AppCurrency.Default
    private val campaignId: PromoCampaignId = params.campaignType.toPromoCampaignId()
    private val campaignContent = CampaignTypeToContentConverter().convert(campaignType)

    val uiState: StateFlow<ActivateCampaignUM>
        field = MutableStateFlow(buildInitialModel())

    val bridge: ChooseTokenBridge = chooseTokenBridgeFactory.create(
        modelScope = modelScope,
        settings = ChooseTokenBridge.Settings(
            title = resourceReference(R.string.common_choose_token),
            isShowMarketBlock = false,
            isShowPaymentAccount = false,
            isShowSingleCurrencyWallets = true,
        ),
    )

    init {
        analyticsEventHandler.send(PromoCampaignsAnalyticsEvent.PromotionScreenOpened(campaignType))

        getSelectedAppCurrencyUseCase.invokeOrDefault()
            .onEach { appCurrency = it }
            .launchIn(modelScope)

        bridge.onCurrencyChosen.receiveAsFlow()
            .onEach { result -> onTokenChosen(result) }
            .launchIn(modelScope)

        bridge.onClose.receiveAsFlow()
            .onEach { onChooseTokenDismiss() }
            .launchIn(modelScope)
    }

    private fun buildInitialModel(): ActivateCampaignUM = ActivateCampaignUM(
        logo = campaignContent.logo,
        title = resourceReference(
            R.string.promo_campaign_summary_title,
            wrappedList(campaignContent.name),
        ),
        description = campaignContent.description,
        selectedToken = null,
        selectedAccount = null,
        isChoosingToken = false,
        footerUM = FooterUM(
            label = resourceReference(R.string.promo_campaign_select_token),
            onPrimaryButtonClick = ::onChooseTokenClick,
        ),
        onChooseTokenDismiss = ::onChooseTokenDismiss,
        onLearnMoreClick = ::onLearnMoreClick,
        onChooseTokenClick = ::onChooseTokenClick,
    )

    private fun onChooseTokenClick() {
        uiState.update { it.copy(isChoosingToken = true) }
    }

    private fun onChooseTokenDismiss() {
        uiState.update { it.copy(isChoosingToken = false) }
    }

    private fun onEnrollClick(selectedWalletId: UserWalletId, selectedToken: CryptoCurrency.Token) {
        analyticsEventHandler.send(
            PromoCampaignsAnalyticsEvent.EnrollButtonClicked(
                campaignType = campaignType,
                token = selectedToken.symbol,
                blockchain = selectedToken.network.name,
            ),
        )

        modelScope.launch {
            enrollPromoCampaignUseCase.invoke(
                campaign = campaignId,
                tokenReward = TokenReward(
                    tokenAddress = selectedToken.contractAddress,
                    networkId = selectedToken.network.rawId,
                ),
                walletIds = listOf(selectedWalletId),
            ).onLeft { error ->
                TangemLogger.e("Error enrolling campaign ${campaignType.campaignId}", error)
                messageSender.send(ToastMessage(message = resourceReference(R.string.common_unknown_error)))
            }.onRight {
                handleEnrollResponse(it)
            }
        }
    }

    private fun handleEnrollResponse(enrollResult: EnrollResult) {
        when (enrollResult) {
            is EnrollResult.AlreadyEnrolled -> params.modelCallbacks.onAlreadyActivated(campaignType)
            is EnrollResult.Success -> params.modelCallbacks.onActivated(campaignType)
        }
    }

    private fun onTermsClick() {
        urlOpener.openUrl(campaignContent.termsUrl)
    }

    private fun onLearnMoreClick() {
        urlOpener.openUrl(campaignContent.learnMoreUrl)
    }

    private fun onTokenChosen(result: ChooseTokenResult) {
        val selectedToken = result.currency.currency as? CryptoCurrency.Token ?: return

        modelScope.launch {
            val selectedAccountUM = if (isAccountsModeEnabledUseCase.invokeSync()) {
                when (val account = result.account.account) {
                    is Account.CryptoPortfolio -> SelectedAccountUM(
                        iconState = accountIconConverter.convert(account),
                        name = account.accountName.toUM().value,
                    )
                    is Account.Payment -> SelectedAccountUM(
                        iconState = CurrencyIconState.PaymentAccount(size = AccountIconSize.ExtraSmall),
                        name = account.accountName.toUM().value,
                    )
                    is Account.Virtual -> null
                }
            } else {
                null
            }

            val tokenItem = TokenItemStateConverter(appCurrency = appCurrency).convert(result.currency)

            uiState.update { state ->
                state.copy(
                    isChoosingToken = false,
                    selectedToken = tokenItem,
                    selectedAccount = selectedAccountUM,
                    footerUM = FooterUM(
                        label = resourceReference(R.string.promo_campaign_enroll),
                        onPrimaryButtonClick = {
                            onEnrollClick(
                                selectedWalletId = result.walletId,
                                selectedToken = selectedToken,
                            )
                        },
                        terms = TermsUM(
                            text = resourceReference(R.string.promo_campaign_terms_agreement_android),
                            linkText = resourceReference(
                                R.string.promo_campaign_terms_link,
                                wrappedList(campaignContent.name),
                            ),
                            onTermsClick = ::onTermsClick,
                        ),
                    ),
                )
            }
        }
    }
}