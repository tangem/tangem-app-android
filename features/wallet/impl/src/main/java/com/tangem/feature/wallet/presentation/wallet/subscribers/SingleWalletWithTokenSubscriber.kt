package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class SingleWalletWithTokenSubscriber @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet.Cold,
    override val accountDependencies: AccountDependencies,
    override val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    override val stateController: WalletStateController,
    override val clickIntents: WalletClickIntents,
) : BasicAccountListSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<Unit> = combine(
        flow = getAccountStatusListFlow(),
        flow2 = getAppCurrencyFlow(),
        flow3 = accountDependencies.expandedAccountsHolder.expandedAccounts(userWallet),
        flow4 = accountDependencies.isAccountsModeEnabledUseCase(),
        transform = ::updateState,
    )

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet.Cold): SingleWalletWithTokenSubscriber
    }
}