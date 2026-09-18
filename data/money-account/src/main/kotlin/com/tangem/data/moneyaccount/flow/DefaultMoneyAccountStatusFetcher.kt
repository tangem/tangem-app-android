package com.tangem.data.moneyaccount.flow

import arrow.core.Either
import arrow.core.computations.ResultEffect.bind
import arrow.core.right
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

internal class DefaultMoneyAccountStatusFetcher @Inject constructor(
    private val moneyAccountStatusesStore: MoneyAccountStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MoneyAccountStatusFetcher {

    override suspend fun invoke(params: MoneyAccountStatusFetcher.Params) = Either.catchOn(dispatchers.default) {
        val status = fetch().bind()
        moneyAccountStatusesStore.store(
            userWalletId = params.userWalletId,
            status = AccountStatus.Money(
                account = Account.Money(userWalletId = params.userWalletId),
                value = status,
            ),
        )
    }.onLeft {
        moneyAccountStatusesStore.updateStatusSource(
            userWalletId = params.userWalletId,
            source = StatusSource.ONLY_CACHE,
        )
    }

    private fun fetch(): Either<Throwable, MoneyAccountStatusValue> {
        return MoneyAccountStatusValue.NotCreated.right()
    }
}