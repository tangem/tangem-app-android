package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.some
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.MultiAccountListProducer
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
 * @property userWalletsStore             store that provides user wallets
 * @property walletAccountListFlowFactory builder to create flows of [AccountList] for each wallet
 * @property dispatchers                  coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiAccountListProducer @AssistedInject constructor(
    @Assisted val params: Unit,
    private val userWalletsStore: UserWalletsStore,
    private val walletAccountListFlowFactory: WalletAccountListFlowFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiAccountListProducer {

    override val fallback: Option<List<AccountList>> = emptyList<AccountList>().some()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun produce(): Flow<List<AccountList>> {
        return userWalletsStore.userWallets
            .distinctUntilChanged()
            .flatMapLatest { userWallets ->
                combine(
                    flows = userWallets.map(walletAccountListFlowFactory::create),
                    transform = ::listOf,
                )
            }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiAccountListProducer.Factory {
        override fun create(params: Unit): DefaultMultiAccountListProducer
    }
}