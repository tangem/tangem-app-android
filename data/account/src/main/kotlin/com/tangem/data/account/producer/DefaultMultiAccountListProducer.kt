package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.some
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.MultiAccountListProducer
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.loadAndGet
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Default implementation of [MultiAccountListProducer].
 * Produces a list of [AccountList]s for all user wallets.
 *
 * @property params                       params
 * @property flowProducerTools            tools for producing flows
 * @property userWalletsListRepository    repository for getting user wallets
 * @property singleAccountListSupplier    supplier for getting [AccountList] per wallet
 * @property dispatchers                  coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiAccountListProducer @AssistedInject constructor(
    @Assisted val params: Unit,
    override val flowProducerTools: FlowProducerTools,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiAccountListProducer {

    override val fallback: Option<List<AccountList>> = emptyList<AccountList>().some()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun produce(): Flow<List<AccountList>> {
        return userWalletsListRepository.loadAndGet()
            .map { wallets -> wallets.map { WalletKey(id = it.walletId, isLocked = it.isLocked) } }
            .distinctUntilChanged()
            .flatMapLatest { wallets ->
                combine(
                    flows = wallets.map(::accountListOrNullWhileLocked),
                    transform = { accountLists -> accountLists.filterNotNull() },
                )
            }
            .flowOn(dispatchers.default)
    }

    /**
     * A locked wallet is never fetched, so its account list may not exist yet.
     * It must not hold back the lists of the other wallets.
     */
    private fun accountListOrNullWhileLocked(wallet: WalletKey): Flow<AccountList?> {
        val accountList = singleAccountListSupplier(wallet.id)

        return if (wallet.isLocked) accountList.onStart<AccountList?> { emit(null) } else accountList
    }

    private data class WalletKey(val id: UserWalletId, val isLocked: Boolean)

    @AssistedFactory
    interface Factory : MultiAccountListProducer.Factory {
        override fun create(params: Unit): DefaultMultiAccountListProducer
    }
}