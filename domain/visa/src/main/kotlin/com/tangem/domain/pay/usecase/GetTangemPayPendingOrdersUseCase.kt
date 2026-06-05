package com.tangem.domain.pay.usecase

import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayPendingOrdersRepository
import kotlinx.coroutines.flow.Flow

class GetTangemPayPendingOrdersUseCase(
    private val pendingOrdersRepository: TangemPayPendingOrdersRepository,
) {
    operator fun invoke(): Flow<List<TangemPayPendingOrder>> = pendingOrdersRepository.getAllFlow()
}