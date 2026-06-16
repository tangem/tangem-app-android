package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError

/**

 * same customer.
 *
 * Wraps `findOrders` (the source of truth) and filters to the active set (NEW / PROCESSING).
 * The caller decides how to dispatch each order to the appropriate flow.
 */
class RestoreActiveOrdersUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, List<Order>> {
        return customerOrderRepository.findOrders(
            userWalletId = userWalletId,
            statuses = ACTIVE_STATUSES,
        )
    }

    private companion object {
        val ACTIVE_STATUSES: Set<OrderStatus> = setOf(OrderStatus.NEW, OrderStatus.PROCESSING)
    }
}