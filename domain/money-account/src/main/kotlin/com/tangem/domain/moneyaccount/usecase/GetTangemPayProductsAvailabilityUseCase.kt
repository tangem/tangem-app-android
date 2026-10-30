package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetTangemPayProductsAvailabilityForCustomerUseCase
import com.tangem.domain.visa.error.VisaApiError

class GetTangemPayProductsAvailabilityUseCase(
    private val getTangemPayProductsAvailabilityForCustomerUseCase: GetTangemPayProductsAvailabilityForCustomerUseCase,
    private val isMoneyAccountPathOccupiedUseCase: IsMoneyAccountPathOccupiedUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Set<TangemPayProduct>> {
        return getTangemPayProductsAvailabilityForCustomerUseCase(userWalletId).map { products ->
            if (TangemPayProduct.BANKING_ACCOUNT in products && isMoneyAccountPathOccupiedUseCase(userWalletId)) {
                products - TangemPayProduct.BANKING_ACCOUNT
            } else {
                products
            }
        }
    }
}