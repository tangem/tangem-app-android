package com.tangem.feature.wallet.child.managetokens.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.models.account.AccountId
import com.tangem.feature.wallet.child.managetokens.AddAndManageBottomSheetComponent
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class AddAndManageModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val portfolioFetcherFactory: PortfolioFetcher.Factory,
    val portfolioSelectorController: PortfolioSelectorController,
) : Model() {

    private val params = paramsContainer.require<AddAndManageBottomSheetComponent.Params>()

    val portfolioSelectorNavigation: SlotNavigation<Unit> = SlotNavigation()

    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.Wallet(params.userWalletId),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { portfolioSelectorNavigation.dismiss() }
        override val onBack: () -> Unit = { portfolioSelectorNavigation.dismiss() }
    }

    init {
        observeAccountSelection()
    }

    fun onAddTokensClick() {
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
}