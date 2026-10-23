package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tangem.data.pay.store.TangemPayCustomerSummaryStore
import com.tangem.data.pay.util.CustomerSummaryConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.repository.CustomerSummaryRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultCustomerSummaryRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
    private val customerSummaryStore: TangemPayCustomerSummaryStore,
) : CustomerSummaryRepository {

    private val mutexes = ConcurrentHashMap<UserWalletId, Mutex>()

    override suspend fun fetch(userWalletId: UserWalletId): Either<VisaApiError, CustomerSummary> {
        val requestedAt = System.nanoTime()
        return mutexes.computeIfAbsent(userWalletId) { Mutex() }.withLock {
            val cache = customerSummaryStore.get().value[userWalletId]
                ?.takeIf { it.fetchedAt >= requestedAt }

            cache?.summary?.right() ?: request(userWalletId).onRight { summary ->
                val entry = TangemPayCustomerSummaryStore.Entry(
                    summary = summary,
                    fetchedAt = System.nanoTime(),
                )
                customerSummaryStore.update { cache -> cache + (userWalletId to entry) }
            }
        }
    }

    override fun getFromCache(userWalletId: UserWalletId): CustomerSummary? {
        return customerSummaryStore.get().value[userWalletId]?.summary
    }

    private suspend fun request(userWalletId: UserWalletId): Either<VisaApiError, CustomerSummary> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCustomerSummary(authHeader = authHeader)
        }.flatMap { response ->
            val result = response.result ?: return@flatMap VisaApiError.UnknownWithoutCode.left()
            CustomerSummaryConverter.convert(result).right()
        }
    }
}