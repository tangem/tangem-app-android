package com.tangem.domain.account.status.producer

import arrow.core.Option
import arrow.core.some
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn

/**
 * Produces a flow of [AccountStatusList] for multiple user wallets.
 *
 * @property params Parameters for the producer (currently unused).
 * @property accountsCRUDRepository Repository to get the list of user wallets.
 * @property singleAccountStatusListSupplier Supplier to get the account status list for a single user wallet.
 * @property dispatchers Coroutine dispatcher provider for managing threading.
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiAccountStatusListProducer @AssistedInject constructor(
    @Assisted val params: Unit,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiAccountStatusListProducer {

    override val fallback: Option<List<AccountStatusList>> = emptyList<AccountStatusList>().some()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun produce(): Flow<List<AccountStatusList>> {
        return accountsCRUDRepository.getUserWallets()
            .flatMapLatest { userWallets ->
                val flows = userWallets.map {
                    singleAccountStatusListSupplier(
                        params = SingleAccountStatusListProducer.Params(it.walletId),
                    )
                }

                combine(flows) { it.toList() }
            }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiAccountStatusListProducer.Factory {
        override fun create(params: Unit): DefaultMultiAccountStatusListProducer
    }
}