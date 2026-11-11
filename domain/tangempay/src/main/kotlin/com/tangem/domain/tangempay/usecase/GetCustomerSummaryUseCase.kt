package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CachePolicy
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.repository.CustomerSummaryRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetCustomerSummaryUseCase(
    private val customerSummaryRepository: CustomerSummaryRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cachePolicy: CachePolicy = CachePolicy.CACHE_FIRST,
    ): Either<TangemPayApiError, CustomerSummary?> {
        return when (cachePolicy) {
            CachePolicy.ONLY_CACHE -> customerSummaryRepository.getFromCache(userWalletId).right()
            CachePolicy.CACHE_FIRST -> {
                customerSummaryRepository.getFromCache(userWalletId)?.right()
                    ?: customerSummaryRepository.fetch(userWalletId)
            }
            CachePolicy.REFRESH -> customerSummaryRepository.fetch(userWalletId)
        }
    }
}