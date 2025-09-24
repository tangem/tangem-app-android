package com.tangem.domain.account.status.producer

import arrow.core.Option
import arrow.core.some
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.core.wallets.UserWalletsListRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

/**
[REDACTED_AUTHOR]
 */
// TODO: Finalize [REDACTED_JIRA]
internal class DefaultMultiAccountStatusListProducer @AssistedInject constructor(
    @Assisted val params: Unit,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) : MultiAccountStatusListProducer {

    override val fallback: Option<List<AccountStatusList>> = emptyList<AccountStatusList>().some()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun produce(): Flow<List<AccountStatusList>> {
        return userWalletsListRepository.userWallets
            .filterNotNull()
            .flatMapLatest { userWallets ->
                val flows = userWallets.map {
                    singleAccountStatusListSupplier(
                        params = SingleAccountStatusListProducer.Params(it.walletId),
                    )
                }

                combine(flows) { it.toList() }
            }
    }

    @AssistedFactory
    interface Factory : MultiAccountStatusListProducer.Factory {
        override fun create(params: Unit): DefaultMultiAccountStatusListProducer
    }
}