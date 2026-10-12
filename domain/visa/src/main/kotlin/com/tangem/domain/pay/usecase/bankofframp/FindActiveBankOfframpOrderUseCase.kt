package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.util.OrderResolver
import com.tangem.domain.visa.error.VisaApiError

/**
 * The wallet's live bank off-ramp order, or `null` when there is none.
 *
 * The backend refuses a second bank order while one is still being processed, so the entry to the
 * withdrawal hub is gated on this lookup rather than letting the customer fill the whole wizard in and
 * fail on Send — the same gate iOS applies.
 *
 * When several orders come back, the most recently updated one wins ([Order.updatedAt] is ISO-8601 UTC,
 * so a lexicographic compare is enough); an order the backend sent without a timestamp never outranks a
 * timestamped one.
 */
class FindActiveBankOfframpOrderUseCase(
    private val repository: CustomerOrderRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Order?> {
        return repository.findOrders(
            userWalletId = userWalletId,
            types = BANK_OFFRAMP_TYPES,
            statuses = OrderStatus.activeStatuses,
        ).map { orders -> OrderResolver.selectActiveByTypes(orders = orders, types = BANK_OFFRAMP_TYPES) }
    }

    private companion object {
        val BANK_OFFRAMP_TYPES = setOf(OrderType.BANK_OFFRAMP_RAIN)
    }
}