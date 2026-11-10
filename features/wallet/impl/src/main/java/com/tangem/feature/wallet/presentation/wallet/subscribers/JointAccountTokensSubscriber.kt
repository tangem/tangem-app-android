package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.utils.ScreenLifecycleProvider
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

internal class JointAccountTokensSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val screenLifecycleProvider: ScreenLifecycleProvider,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val singleAccountListFetcher: SingleAccountListFetcher,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return merge(refreshAccountsOnResume(), fetchBalancesOfAddedJointCurrencies())
    }

    private fun refreshAccountsOnResume(): Flow<*> {
        return screenLifecycleProvider.isBackgroundState
            .drop(1)
            .filterNot { isBackground -> isBackground }
            .filter { isSelectedWallet() && hasJointAccount() }
            .onEach {
                singleAccountListFetcher(SingleAccountListFetcher.Params(userWalletId = userWallet.walletId))
                    .onLeft { error -> TangemLogger.e("Failed to refresh accounts of ${userWallet.walletId}", error) }
            }
    }

    private fun fetchBalancesOfAddedJointCurrencies(): Flow<*> {
        return singleAccountListSupplier(userWallet.walletId)
            .map { accountList -> accountList.jointCurrencies() }
            .distinctUntilChanged { old, new -> old.keys == new.keys }
            .addedCurrencies()
            .onEach { added -> cryptoCurrencyBalanceFetcher(userWalletId = userWallet.walletId, currencies = added) }
    }

    private fun isSelectedWallet(): Boolean {
        return userWalletsListRepository.selectedUserWallet.value?.walletId == userWallet.walletId
    }

    private suspend fun hasJointAccount(): Boolean {
        return singleAccountListSupplier.getSyncOrNull(userWallet.walletId)
            ?.accounts
            ?.any { it is Account.Joint } == true
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): JointAccountTokensSubscriber
    }
}

private fun AccountList.jointCurrencies(): Map<CryptoCurrency.ID, CryptoCurrency> {
    return accounts
        .filterIsInstance<Account.Joint>()
        .flatMap(Account.Joint::cryptoCurrencies)
        .associateBy(CryptoCurrency::id)
}

private fun Flow<Map<CryptoCurrency.ID, CryptoCurrency>>.addedCurrencies(): Flow<List<CryptoCurrency>> = flow {
    var previous: Map<CryptoCurrency.ID, CryptoCurrency>? = null
    collect { current ->
        val added = previous?.let { previousCurrencies -> current.filterKeys { it !in previousCurrencies } }
        if (!added.isNullOrEmpty()) emit(added.values.toList())
        previous = current
    }
}