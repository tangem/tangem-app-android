package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.moneyaccount.MoneyAccountFeatureToggles
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetTangemPayProductsAvailabilityForCountryUseCase(
    private val checkTangemPayEligibilityUseCase: CheckTangemPayEligibilityUseCase,
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
) {
    suspend operator fun invoke(countryCode: String, regionCode: String?): Either<VisaApiError, Set<TangemPayProduct>> {
        val results = coroutineScope {
            supportedProducts()
                .map { product ->
                    async {
                        product to checkTangemPayEligibilityUseCase(
                            product = product,
                            countryCode = countryCode,
                            regionCode = regionCode,
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

    private fun supportedProducts(): List<TangemPayProduct> {
        return if (moneyAccountFeatureToggles.isMoneyAccountsEnabled) {
            TangemPayProduct.entries
        } else {
            TangemPayProduct.entries - TangemPayProduct.BANKING_ACCOUNT
        }
    }
}