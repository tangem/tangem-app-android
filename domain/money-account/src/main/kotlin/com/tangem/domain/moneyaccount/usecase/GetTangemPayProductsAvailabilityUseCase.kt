package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.usecase.GetTangemPayProductsAvailabilityForCustomerUseCase
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetTangemPayProductsAvailabilityUseCase(
    private val getTangemPayProductsAvailabilityForCustomerUseCase: GetTangemPayProductsAvailabilityForCustomerUseCase,
    private val isMoneyAccountPathOccupiedUseCase: IsMoneyAccountPathOccupiedUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<TangemPayApiError, Set<TangemPayProduct>> {
        return getTangemPayProductsAvailabilityForCustomerUseCase(userWalletId).map { products ->
            if (TangemPayProduct.BANKING_ACCOUNT in products && isMoneyAccountPathOccupiedUseCase(userWalletId)) {
                products - TangemPayProduct.BANKING_ACCOUNT
            } else {
                products
            }
        }
    }
}