package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.ModelScopeDependencies
import com.tangem.feature.wallet.presentation.account.AccountsSharedFlowHolder
import com.tangem.feature.wallet.presentation.wallet.domain.WalletWithFundsChecker
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/**
 * Subscriber that checks if the wallet has funds and notifies the [WalletWithFundsChecker].
 *
 * @property userWallet The user wallet.
 * @property singleAccountStatusListSupplier Supplier for account status list.
 * @property walletWithFundsChecker The checker to notify when funds are detected.
 * @property dispatchers Coroutine dispatcher provider.
 *
[REDACTED_AUTHOR]
 */
internal class CheckWalletWithFundsSubscriber @AssistedInject constructor(
    @Assisted override val userWallet: UserWallet,
    @Assisted val modelScopeDependencies: ModelScopeDependencies,
    override val accountsSharedFlowHolder: AccountsSharedFlowHolder = modelScopeDependencies.accountsSharedFlowHolder,
    private val walletWithFundsChecker: WalletWithFundsChecker,
    private val dispatchers: CoroutineDispatcherProvider,
) : BasicWalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return getAccountStatusListFlow()
            .mapNotNull { it.flattenCurrencies() }
            .distinctUntilChanged()
            .onEach { walletWithFundsChecker.check(userWalletId = userWallet.walletId, currencies = it) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            userWallet: UserWallet,
            modelScopeDependencies: ModelScopeDependencies,
        ): CheckWalletWithFundsSubscriber
    }
}