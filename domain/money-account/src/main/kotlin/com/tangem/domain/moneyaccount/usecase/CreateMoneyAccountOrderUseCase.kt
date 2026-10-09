package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.usecase.FindMoneyAccountCreationOrderUseCase
import com.tangem.domain.visa.error.VisaApiError

class CreateMoneyAccountOrderUseCase(
    private val orderRepository: MoneyAccountOrderRepository,
    private val trackOrder: TrackMoneyAccountOrderUseCase,
    private val findMoneyAccountCreationOrderUseCase: FindMoneyAccountCreationOrderUseCase,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        depositAddress: String,
        signature: String,
    ): Either<VisaApiError, Unit> {
        val activeOrder = findMoneyAccountCreationOrderUseCase.invoke(userWalletId).getOrNull()
        return if (activeOrder != null) {
            startOrderTracking(userWalletId, activeOrder)
            Unit.right()
        } else {
            orderRepository.createOrder(
                userWalletId = userWalletId,
                depositAddress = depositAddress,
                signature = signature,
            )
                .handleActiveOrderConflict(userWalletId)
                .map { order -> startOrderTracking(userWalletId, order) }
        }
    }

    private suspend fun Either<VisaApiError, Order>.handleActiveOrderConflict(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, Order> = fold(
        ifLeft = { error ->
            if (error == VisaApiError.AccountIssueActiveOrderExists) {
                findMoneyAccountCreationOrderUseCase.invoke(userWalletId)
                    .flatMap { order -> order?.right() ?: VisaApiError.OrderNotFound.left() }
            } else {
                error.left()
            }
        },
        ifRight = { order -> order.right() },
    )

    private fun startOrderTracking(userWalletId: UserWalletId, order: Order) {
        trackOrder(userWalletId = userWalletId, order = TangemPayOrderInfo.fromOrder(order))
    }
}