package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.PortfolioId
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.domain.collectLatest
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetCryptoCurrencyActionsTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class SingleWalletButtonsSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase,
    private val accountDependencies: AccountDependencies,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<TokenActionsState> {
        return channelFlow {
            getSingleCryptoCurrencyStatusUseCase.collectLatest(userWalletId = userWallet.walletId) { status ->
                getCryptoCurrencyActionsUseCase(userWallet = userWallet, status = status)
                    ?.let { send(it) }
            }
        }
            .onEach { actions ->
                val portfolioId = when (accountDependencies.accountsFeatureToggles.isFeatureEnabled) {
                    true -> TODO("account") // get main account id for single wallet
                    false -> PortfolioId(userWallet.walletId)
                }
                updateContent(actions, portfolioId)
            }
    }

    private fun updateContent(tokenActionsState: TokenActionsState, portfolioId: PortfolioId) {
        stateHolder.update(
            SetCryptoCurrencyActionsTransformer(
                tokenActionsState = tokenActionsState,
                userWallet = userWallet,
                clickIntents = clickIntents,
                portfolioId = portfolioId,
            ),
        )
    }

    private suspend fun getCryptoCurrencyActionsUseCase(userWallet: UserWallet, status: CryptoCurrencyStatus) =
        this.getCryptoCurrencyActionsUseCase(userWallet = userWallet, cryptoCurrencyStatus = status)
            .conflate()
            .distinctUntilChanged()
            .firstOrNull()
}