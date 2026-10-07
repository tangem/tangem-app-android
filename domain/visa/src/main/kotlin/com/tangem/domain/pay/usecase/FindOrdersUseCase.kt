package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError

class FindOrdersUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        types: Set<OrderType> = emptySet(),
        statuses: Set<OrderStatus> = emptySet(),
    ): Either<VisaApiError, List<Order>> {
        return customerOrderRepository.findOrders(
            userWalletId = userWalletId,
            types = types,
            statuses = statuses,
        )
    }
}