package com.tangem.features.promobanners.impl.campaigns.model

import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenResult
import com.tangem.features.promobanners.impl.R
import com.tangem.features.promobanners.impl.campaigns.entity.ActivateCampaignUM
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import com.tangem.features.promobanners.impl.campaigns.entity.FooterUM
import com.tangem.features.promobanners.impl.campaigns.entity.SelectedAccountUM
import com.tangem.features.promobanners.impl.campaigns.entity.TermsUM
import com.tangem.features.promobanners.impl.campaigns.entity.campaignName
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class ActivateCampaignsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    chooseTokenBridgeFactory: ChooseTokenBridge.Factory,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val urlOpener: UrlOpener,
) : Model() {

    private val params = paramsContainer.require<Params>()
    private val accountIconConverter = AccountIconItemStateConverter(size = AccountIconSize.ExtraSmall)
    private var appCurrency: AppCurrency = AppCurrency.Default
    private var selectedAccount: Account? = null
    private var selectedCurrency: CryptoCurrencyStatus? = null

    val uiState: StateFlow<ActivateCampaignUM>
        field = MutableStateFlow(getInitialState())

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

    private fun onSelectTokenClick() {
        uiState.update { it.copy(isChoosingToken = true) }
    }

    private fun onChooseTokenDismiss() {
        uiState.update { it.copy(isChoosingToken = false) }
    }

    private fun onEnrollClick() {
        modelScope.launch {
            // TODO([REDACTED_TASK_KEY]): call the real campaign enrollment use case; its result decides the next sheet.
            //  Success -> the "enrolled" sheet; "already activated" error -> hand the chosen token/account
            //  over to the "already activated" sheet.
            if (enrollInCampaign()) {
                params.onActivated(params.campaignType)
            } else {
                selectedCurrency?.let {
                    params.onAlreadyActivated(params.campaignType, appCurrency, selectedAccount, it)
                }
            }
        }
    }

    @Suppress("FunctionOnlyReturningConstant") // TODO([REDACTED_TASK_KEY]): stub until the real enrollment use case exists.
    private fun enrollInCampaign(): Boolean {
        // TODO([REDACTED_TASK_KEY]): replace with the real enrollment use case call; `true` = enrolled, `false` = already active.
        return true
    }

    private fun onTermsClick() {
        urlOpener.openUrl(CAMPAIGN_TERMS_URL)
    }

    private fun onLearnMoreClick() {
        urlOpener.openUrl(CAMPAIGN_TERMS_URL)
    }

    private fun onTokenChosen(result: ChooseTokenResult) {
        modelScope.launch {
            val selectedAccountUM = if (isAccountsModeEnabledUseCase.invokeSync()) {
                val account = result.account.account
                selectedAccount = account

                when (account) {
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

            selectedCurrency = result.currency
            val tokenItem = TokenItemStateConverter(appCurrency = appCurrency).convert(result.currency)

            uiState.update { state ->
                state.copy(
                    isChoosingToken = false,
                    selectedToken = tokenItem,
                    selectedAccount = selectedAccountUM,
                    footerUM = FooterUM(
                        label = stringReference("Enroll"),
                        onPrimaryButtonClick = ::onEnrollClick,
                        terms = TermsUM(
                            // TODO([REDACTED_TASK_KEY]): localize
                            text = stringReference("I agree with"),
                            linkText = stringReference("${params.campaignType.campaignName()} Terms"),
                            onTermsClick = ::onTermsClick,
                        ),
                    ),
                )
            }
        }
    }

    private fun getInitialState(): ActivateCampaignUM {
        return ActivateCampaignUM(
            // TODO([REDACTED_TASK_KEY]): source real campaign copy.
            title = stringReference("Enroll in ${params.campaignType.campaignName()}"),
            description = stringReference(
                "Earn cashback on every swap from \$10K until the end of July.\n\n" +
                    "Rates step up with size: 0.10% from \$10K, 0.20% from \$20K, 0.50% from \$100K.",
            ),
            selectedToken = null,
            selectedAccount = null,
            isChoosingToken = false,
            footerUM = FooterUM(
                label = stringReference("Select token"),
                onPrimaryButtonClick = ::onSelectTokenClick,
            ),
            onChooseTokenDismiss = ::onChooseTokenDismiss,
            onLearnMoreClick = ::onLearnMoreClick,
        )
    }

    data class Params(
        val campaignType: CampaignType,
        val onDismiss: () -> Unit,
        val onActivated: (CampaignType) -> Unit,
        val onAlreadyActivated: (CampaignType, AppCurrency, Account?, CryptoCurrencyStatus) -> Unit,
    )

    private companion object {
        // TODO([REDACTED_TASK_KEY]): replace with the real campaign terms URL.
        const val CAMPAIGN_TERMS_URL = "https://tangem.com/en/"
    }
}