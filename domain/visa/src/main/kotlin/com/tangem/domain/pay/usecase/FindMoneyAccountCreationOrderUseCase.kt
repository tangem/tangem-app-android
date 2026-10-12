package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.visa.error.VisaApiError
import kotlinx.datetime.Instant

class FindMoneyAccountCreationOrderUseCase(
    private val findOrdersUseCase: FindOrdersUseCase,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Order?> {
        return findOrdersUseCase.invoke(
            userWalletId = userWalletId,
            types = setOf(OrderType.ACCOUNT_ISSUE_BANKING_RAIN),
            statuses = setOf(OrderStatus.NEW, OrderStatus.PROCESSING, OrderStatus.COMPLETED),
        ).map { orders ->
            orders.maxByOrNull { order ->
                order.updatedAt?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST
            }
        }
    }
}