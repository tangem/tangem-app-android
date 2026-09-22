package com.tangem.data.jointaccount.fetcher

import arrow.core.Either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.jointaccount.converter.JointAccountDtoConverter
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.store.datasource.jointaccount.JointAccountApi
import com.tangem.store.datasource.jointaccount.models.GetJointAccountsResponse
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultSingleJointAccountListFetcher @Inject constructor(
    private val jointAccountApi: JointAccountApi,
    private val store: JointAccountsStore,
    private val converter: JointAccountDtoConverter,
    private val eTagsStore: ETagsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleJointAccountListFetcher {

    override suspend fun invoke(params: SingleJointAccountListFetcher.Params): Either<Throwable, Unit> {
        val userWalletId = params.userWalletId

        return Either.catchOn(dispatchers.default) {
            val response = jointAccountApi.getJointAccounts(
                walletId = userWalletId.stringValue,
                eTag = eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETAG_KEY),
            )

            when {
                response is ApiResponse.Success -> storeAccounts(userWalletId = userWalletId, response = response)
                // Nothing in the list moved since the stored ETag, so the cache is the current state
                response.isNotModified() && store.contains(userWalletId) -> {
                    store.updateStatusSource(userWalletId = userWalletId, source = StatusSource.ACTUAL)
                }
                // The tag outlives the cache, so a 304 with nothing cached would read as a wallet without accounts
                response.isNotModified() -> {
                    TangemLogger.i("The joint accounts ETag of $userWalletId outlived the cache, refetching without it")
                    eTagsStore.clear(userWalletId = userWalletId, key = ETAG_KEY)

                    storeAccounts(
                        userWalletId = userWalletId,
                        response = jointAccountApi.getJointAccounts(walletId = userWalletId.stringValue),
                    )
                }
                else -> throw (response as ApiResponse.Error).cause
            }
        }.onLeft {
            store.updateStatusSource(userWalletId = userWalletId, source = StatusSource.ONLY_CACHE)
        }
    }

    private suspend fun storeAccounts(userWalletId: UserWalletId, response: ApiResponse<GetJointAccountsResponse>) {
        val accounts = response.getOrThrow()

        storeETag(userWalletId = userWalletId, response = response)
        store.store(userWalletId = userWalletId, accounts = accounts.jointAccounts.map(converter::convert))
    }

    /** A response without the header keeps the stored value: a read invalidates nothing */
    private suspend fun storeETag(userWalletId: UserWalletId, response: ApiResponse<*>) {
        val eTag = response.headers[ETAG_HEADER]?.firstOrNull() ?: return

        eTagsStore.store(userWalletId = userWalletId, key = ETAG_KEY, value = eTag)
    }

    private fun ApiResponse<*>.isNotModified(): Boolean {
        val httpException = (this as? ApiResponse.Error)?.cause as? ApiResponseError.HttpException ?: return false

        return httpException.code == ApiResponseError.HttpException.Code.NOT_MODIFIED
    }

    private companion object {
        val ETAG_KEY = ETagsStore.Key.JointAccounts
    }
}