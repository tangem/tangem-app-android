package com.tangem.data.jointaccount.fetcher

import arrow.core.Either
import com.tangem.data.jointaccount.converter.JointAccountDtoConverter
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.jointaccount.JointAccountApi
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.models.StatusSource
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

internal class DefaultSingleJointAccountListFetcher @Inject constructor(
    private val jointAccountApi: JointAccountApi,
    private val store: JointAccountsStore,
    private val converter: JointAccountDtoConverter,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleJointAccountListFetcher {

    override suspend fun invoke(params: SingleJointAccountListFetcher.Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.default) {
            val response = jointAccountApi.getJointAccounts(walletId = params.userWalletId.stringValue).getOrThrow()

            store.store(
                userWalletId = params.userWalletId,
                accounts = response.jointAccounts.map(converter::convert),
            )
        }.onLeft {
            store.updateStatusSource(userWalletId = params.userWalletId, source = StatusSource.ONLY_CACHE)
        }
    }
}