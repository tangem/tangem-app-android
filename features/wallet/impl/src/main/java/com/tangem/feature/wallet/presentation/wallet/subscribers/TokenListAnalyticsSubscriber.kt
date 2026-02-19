package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.TokenListAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

/**
 * Subscriber that monitors account list changes and sends token list analytics
 * when the total fiat balance changes.
 *
[REDACTED_AUTHOR]
 */
internal class TokenListAnalyticsSubscriber @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet,
    override val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val stateHolder: WalletStateController,
    private val tokenListAnalyticsSender: TokenListAnalyticsSender,
) : BasicWalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> = getAccountStatusListFlow()
        .distinctUntilChanged { old, new -> old.totalFiatBalance == new.totalFiatBalance }
        .onEach(::sendTokenListAnalytics)

    private suspend fun sendTokenListAnalytics(accountStatusList: AccountStatusList) {
        val displayedState = stateHolder.getWalletStateIfSelected(userWallet.walletId)

        val flattenCurrencies = accountStatusList.flattenCurrencies()
        tokenListAnalyticsSender.send(
            displayedUiState = displayedState,
            userWallet = userWallet,
            flattenCurrencies = flattenCurrencies,
            totalFiatBalance = accountStatusList.totalFiatBalance,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): TokenListAnalyticsSubscriber
    }
}