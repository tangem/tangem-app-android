package com.tangem.feature.wallet.child.managetokens.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.models.hasMultiCurrencyAccount
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountId
import com.tangem.feature.wallet.child.managetokens.AddAndManageBottomSheetComponent
import com.tangem.feature.wallet.child.managetokens.analytics.PortfolioAnalyticsEvent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class AddAndManageModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val portfolioFetcherFactory: PortfolioFetcher.Factory,
    private val analyticsEventHandler: AnalyticsEventHandler,
    val portfolioSelectorController: PortfolioSelectorController,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) : Model() {

    private val params = paramsContainer.require<AddAndManageBottomSheetComponent.Params>()

    val portfolioSelectorNavigation: SlotNavigation<Unit> = SlotNavigation()
    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.Wallet(params.userWalletId),
            scope = modelScope,
        )
    }
    val state: StateFlow<AddAndManageState>
        field = MutableStateFlow(AddAndManageState(shouldShowOrganize = true))

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { portfolioSelectorNavigation.dismiss() }
        override val onBack: () -> Unit = { portfolioSelectorNavigation.dismiss() }
    }

    init {
        observeAccountSelection()
        updateShouldShowOrganizeButtonState()
    }

    fun onAddTokensClick() {
        analyticsEventHandler.send(PortfolioAnalyticsEvent.ButtonAddTokens())
        modelScope.launch {
            val data = portfolioFetcher.data.first()
            val isSingleAccount = data.isSingleChoice(params.userWalletId)

            if (isSingleAccount) {
                val mainAccountId = data.balances[params.userWalletId]
                    ?.accountsBalance
                    ?.mainAccount
                    ?.accountId
                    ?: AccountId.forMainCryptoPortfolio(params.userWalletId)

                params.onDismiss()
                params.onManageTokensClick(mainAccountId)
            } else {
                portfolioSelectorNavigation.activate(Unit)
            }
        }
    }

    fun onOrganizeTokensClick() {
        analyticsEventHandler.send(PortfolioAnalyticsEvent.ButtonOrganizeTokens())
        params.onDismiss()
        params.onOrganizeTokensClick()
    }

    private fun observeAccountSelection() {
        modelScope.launch {
            portfolioSelectorController.selectedAccount.collect { accountId ->
                if (accountId != null) {
                    portfolioSelectorNavigation.dismiss()
                    params.onDismiss()
                    params.onManageTokensClick(accountId)
                }
            }
        }
    }

    private fun updateShouldShowOrganizeButtonState() {
        modelScope.launch {
            val accountStatusesList = singleAccountStatusListSupplier.getSyncOrNull(params.userWalletId)
            val hasMultiCurrencyAccount = accountStatusesList?.hasMultiCurrencyAccount() == true
            state.update { it.copy(shouldShowOrganize = hasMultiCurrencyAccount) }
        }
    }
}