package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.ConflictResolution
import com.tangem.domain.pay.model.OrderConflictRules
import com.tangem.domain.pay.model.OrderIntent
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError

/**
 * Evaluates whether a user-driven [OrderIntent] is allowed given the currently active orders.
 *
 * Re-fetches active orders before evaluating so the decision uses up-to-date server state, not a
 * stale UI cache. UI is expected to call this immediately before triggering the action.
 */
class CheckOrderConflictUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        intent: OrderIntent,
    ): Either<VisaApiError, ConflictResolution> {
        return customerOrderRepository
            .findOrders(userWalletId = userWalletId, statuses = ACTIVE_STATUSES)
            .map { orders -> OrderConflictRules.resolve(intent = intent, activeOrders = orders) }
    }

    private companion object {
        val ACTIVE_STATUSES: Set<OrderStatus> = setOf(OrderStatus.NEW, OrderStatus.PROCESSING)
    }
}