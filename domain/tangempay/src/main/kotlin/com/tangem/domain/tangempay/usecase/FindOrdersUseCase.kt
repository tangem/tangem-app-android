package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.Order
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class FindOrdersUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        types: Set<OrderType> = emptySet(),
        statuses: Set<OrderStatus> = emptySet(),
    ): Either<TangemPayApiError, List<Order>> {
        return customerOrderRepository.findOrders(
            userWalletId = userWalletId,
            types = types,
            statuses = statuses,
        )
    }
}