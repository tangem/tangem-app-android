package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.status.usecase.GetCryptoCurrencyActionsUseCaseV2
import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.feature.wallet.child.wallet.model.ModelScopeDependencies
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountsSharedFlowHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetCryptoCurrencyActionsTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

internal class SingleWalletButtonsSubscriberV2(
    override val userWallet: UserWallet,
    val modelScopeDependencies: ModelScopeDependencies,
    override val accountsSharedFlowHolder: AccountsSharedFlowHolder = modelScopeDependencies.accountsSharedFlowHolder,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val getCryptoCurrencyActionsUseCaseV2: GetCryptoCurrencyActionsUseCaseV2,
) : BasicSingleWalletSubscriber() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun create(coroutineScope: CoroutineScope): Flow<TokenActionsState> {
        return getPrimaryCurrencyStatusFlow()
            .flatMapLatest {
                getCryptoCurrencyActionsUseCaseV2(accountId = accountId, currency = it.currency)
            }
            .onEach {
                updateContent(tokenActionsState = it, portfolioId = PortfolioId(userWallet.walletId))
            }
    }

    private fun updateContent(tokenActionsState: TokenActionsState, portfolioId: PortfolioId) {
        stateController.update(
            SetCryptoCurrencyActionsTransformer(
                tokenActionsState = tokenActionsState,
                userWallet = userWallet,
                clickIntents = clickIntents,
                portfolioId = portfolioId,
            ),
        )
    }
}