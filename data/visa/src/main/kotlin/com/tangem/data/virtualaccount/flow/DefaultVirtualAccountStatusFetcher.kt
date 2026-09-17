package com.tangem.data.virtualaccount.flow

import arrow.core.Either
import arrow.core.computations.ResultEffect.bind
import arrow.core.right
import com.tangem.data.virtualaccount.store.VirtualAccountStatusesStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

internal class DefaultVirtualAccountStatusFetcher @Inject constructor(
    private val virtualAccountStatusesStore: VirtualAccountStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : VirtualAccountStatusFetcher {

    override suspend fun invoke(params: VirtualAccountStatusFetcher.Params) = Either.catchOn(dispatchers.default) {
        val status = fetch().bind()
        virtualAccountStatusesStore.store(
            userWalletId = params.userWalletId,
            status = AccountStatus.Virtual(
                account = Account.Virtual(userWalletId = params.userWalletId),
                value = status,
            ),
        )
    }.onLeft {
        virtualAccountStatusesStore.updateStatusSource(
            userWalletId = params.userWalletId,
            source = StatusSource.ONLY_CACHE,
        )
    }

    private fun fetch(): Either<Throwable, VirtualAccountStatusValue> {
        return VirtualAccountStatusValue.NotCreated.right()
    }
}