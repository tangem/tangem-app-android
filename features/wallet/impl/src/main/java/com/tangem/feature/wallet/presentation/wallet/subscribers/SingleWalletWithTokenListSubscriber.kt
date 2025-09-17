package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.RunPolkadotAccountHealthCheckUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.MultiWalletTokenListStore
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Suppress("LongParameterList")
internal class SingleWalletWithTokenListSubscriber(
    override val userWallet: UserWallet.Cold,
    private val tokenListStore: MultiWalletTokenListStore,
    override val stateHolder: WalletStateController,
    override val clickIntents: WalletClickIntents,
    override val tokenListAnalyticsSender: TokenListAnalyticsSender,
    override val walletWithFundsChecker: WalletWithFundsChecker,
    override val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    override val runPolkadotAccountHealthCheckUseCase: RunPolkadotAccountHealthCheckUseCase,
    override val accountDependencies: AccountDependencies,
) : BasicTokenListSubscriber() {

    override fun tokenListFlow(coroutineScope: CoroutineScope): LceFlow<TokenListError, TokenList> {
        tokenListStore.addIfNot(userWallet.walletId, coroutineScope)

        return tokenListStore.getOrThrow(userWallet.walletId)
    }

    override fun accountListFlow(coroutineScope: CoroutineScope): Flow<AccountStatusList> {
        // todo account load
        return emptyFlow()
    }

    override suspend fun onTokenListReceived(maybeTokenList: Lce<TokenListError, TokenList>) = Unit
}