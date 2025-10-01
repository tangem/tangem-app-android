package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.none
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

/**
 * Default implementation of [SingleAccountListProducer].
 * Produces a list of [AccountList] for a specific user wallet.
 *
 * @property params                       params containing the user wallet ID
 * @property userWalletsStore             store that provides user wallets
 * @property walletAccountListFlowFactory builder to create flows of [AccountList] for each wallet
 * @property dispatchers                  coroutine dispatchers provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleAccountListProducer @AssistedInject constructor(
    @Assisted val params: SingleAccountListProducer.Params,
    private val userWalletsStore: UserWalletsStore,
    private val walletAccountListFlowFactory: WalletAccountListFlowFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleAccountListProducer {

    override val fallback: Option<AccountList> = none()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun produce(): Flow<AccountList> {
        return userWalletsStore.userWallets
            .mapNotNull { userWallets ->
                userWallets.firstOrNull { it.walletId == params.userWalletId }
            }
            .flatMapLatest(walletAccountListFlowFactory::create)
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : SingleAccountListProducer.Factory {
        override fun create(params: SingleAccountListProducer.Params): DefaultSingleAccountListProducer
    }
}