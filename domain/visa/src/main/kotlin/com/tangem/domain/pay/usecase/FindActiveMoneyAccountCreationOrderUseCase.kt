package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.util.OrderResolver
import com.tangem.domain.visa.error.VisaApiError

class FindActiveMoneyAccountCreationOrderUseCase(
    private val findActiveOrdersUseCase: FindActiveOrdersUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Order?> {
        val types = setOf(OrderType.ACCOUNT_ISSUE_BANKING_RAIN)
        return findActiveOrdersUseCase.invoke(
            userWalletId = userWalletId,
            types = types,
        ).map { orders ->
            OrderResolver.selectActiveByTypes(orders = orders, types = types)
        }
    }
}