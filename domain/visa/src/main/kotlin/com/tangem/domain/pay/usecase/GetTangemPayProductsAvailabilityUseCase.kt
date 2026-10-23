package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetTangemPayProductsAvailabilityUseCase(
    private val checkTangemPayEligibilityForCustomerUseCase: CheckTangemPayEligibilityForCustomerUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Set<TangemPayProduct>> {
        val results = coroutineScope {
            TangemPayProduct.entries
                .map { product ->
                    async {
                        product to checkTangemPayEligibilityForCustomerUseCase(
                            userWalletId = userWalletId,
                            product = product,
                        )
                    }
                }
                .awaitAll()
        }
        return either {
            results
                .filter { (_, eligibility) -> eligibility.bind().isAvailable }
                .mapTo(mutableSetOf()) { (product, _) -> product }
        }
    }
}