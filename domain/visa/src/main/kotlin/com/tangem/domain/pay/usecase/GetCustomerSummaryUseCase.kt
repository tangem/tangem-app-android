package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.repository.CustomerSummaryRepository
import com.tangem.domain.visa.error.VisaApiError

class GetCustomerSummaryUseCase(
    private val customerSummaryRepository: CustomerSummaryRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cachePolicy: CachePolicy = CachePolicy.CACHE_FIRST,
    ): Either<VisaApiError, CustomerSummary?> {
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