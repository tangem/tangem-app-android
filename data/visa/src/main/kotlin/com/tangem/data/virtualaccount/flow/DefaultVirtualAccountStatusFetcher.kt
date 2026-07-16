package com.tangem.data.virtualaccount.flow

import arrow.core.Either
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
        val account = Account.Virtual(userWalletId = params.userWalletId)
        // TODO([REDACTED_TASK_KEY]): Replace with the real VA status fetch (provisioning state, balance and banking
        //  details) from the backend once Virtual Account status endpoints are available. Until then the
        //  account is surfaced as NotCreated so the entity flows through the app end-to-end.
        virtualAccountStatusesStore.store(
            userWalletId = params.userWalletId,
            status = AccountStatus.Virtual(account = account, value = VirtualAccountStatusValue.NotCreated),
        )
    }.onLeft {
        virtualAccountStatusesStore.updateStatusSource(
            userWalletId = params.userWalletId,
            source = StatusSource.ONLY_CACHE,
        )
    }
}