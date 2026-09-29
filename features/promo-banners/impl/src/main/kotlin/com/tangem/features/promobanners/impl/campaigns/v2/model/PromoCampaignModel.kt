package com.tangem.features.promobanners.impl.campaigns.v2.model

import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.commonfeatures.api.choosetoken.ChooserBlock
import com.tangem.features.commonfeatures.api.choosetoken.PaymentAccountTokens
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.model.PredefinedTokenResolver
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignBottomSheetComponent
import com.tangem.features.promobanners.impl.campaigns.v2.component.PromoCampaignRoute
import com.tangem.features.promobanners.impl.campaigns.v2.converters.PromoCampaignUMConverter
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoCampaignUM
import com.tangem.features.promobanners.impl.campaigns.v2.entity.PromoSelectedAccountUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class PromoCampaignModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    predefinedTokenResolver: PredefinedTokenResolver,
    private val router: Router,
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val urlOpener: UrlOpener,
) : Model() {

    private val params = paramsContainer.require<PromoCampaignBottomSheetComponent.Params>()

    private val converter = PromoCampaignUMConverter(
        onLinkClick = urlOpener::openUrl,
        onActionClick = ::onActionClick,
        onChooseTokenClick = ::onChooseTokenClick,
    )

    val uiState: StateFlow<PromoCampaignUM>
        field = MutableStateFlow(converter.convert(params.campaign))

    val bridge: ChooseTokenBridge = chooseTokenBridgeFactory.create(
        modelScope = modelScope,
        settings = ChooseTokenBridge.Settings(
            title = resourceReference(R.string.common_choose_token),
            chooserBlock = ChooserBlock.Predefined(
                MutableStateFlow(predefinedTokenResolver.resolve(params.campaign.payout.tokens)),
            ),
            paymentAccountTokens = PaymentAccountTokens.Hidden,
            isShowSingleCurrencyWallets = true,
            isAppBarShown = false,
        ),
    )

    init {
        bridge.onCurrencyChosen.receiveAsFlow()
            .onEach(::onTokenChosen)
            .launchIn(modelScope)

        bridge.onClose.receiveAsFlow()
            .onEach { router.pop() }
            .launchIn(modelScope)
    }

    private fun onActionClick() {
        val tokenSelector = uiState.value.tokenSelector
        if (tokenSelector != null && tokenSelector.selectedToken == null) {
            onChooseTokenClick()
        } else {
            onEnrollClick()
        }
    }

    private fun onEnrollClick() {
        // Enrollment is delivered separately ([REDACTED_TASK_KEY]); until then the action only gates on the payout token.
    }

    private fun onChooseTokenClick() {
        router.push(PromoCampaignRoute.ChooseToken)
    }

    private fun onTokenChosen(result: ChooseTokenResult) {
        modelScope.launch {
            val selectedAccount = if (hasMultipleCryptoPortfolioAccounts()) {
                (result.account.account as? Account.CryptoPortfolio)?.let { account ->
                    PromoSelectedAccountUM(
                        iconState = AccountIconItemStateConverter(size = AccountIconSize.ExtraSmall).convert(account),
                        name = account.accountName.toUM().value,
                    )
                }
            } else {
                null
            }

            val selectedToken = TokenItemStateConverter(
                appCurrency = AppCurrency.Default,
                subtitleStateProvider = { status ->
                    TokenItemState.SubtitleState.TextContent(
                        value = resourceReference(
                            R.string.domain_receive_assets_onboarding_network_name,
                            wrappedList(status.currency.network.name),
                        ),
                    )
                },
            ).convert(result.currency)

            uiState.update { state ->
                state.copy(
                    footer = converter.convertFooter(campaign = params.campaign, isPayoutTokenPending = false),
                    tokenSelector = state.tokenSelector?.copy(
                        selectedToken = selectedToken,
                        selectedAccount = selectedAccount,
                    ),
                )
            }
            router.pop()
        }
    }

    private suspend fun hasMultipleCryptoPortfolioAccounts(): Boolean {
        return multiAccountListSupplier.invoke()
            .first()
            .any { accountList -> accountList.accounts.filterIsInstance<Account.CryptoPortfolio>().size > 1 }
    }
}