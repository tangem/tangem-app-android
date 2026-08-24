package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.util.OrderResolver
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch

class ReissuePlasticCardUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        sourceProductInstanceId: String,
        plasticCardOrder: PlasticCardOrder,
        idempotencyKey: String,
    ): Either<VisaApiError, Order> = either {
        val activeOrders = catch(
            block = {
                customerOrderRepository
                    .findOrders(
                        userWalletId = userWalletId,
                        types = setOf(OrderType.CARD_REISSUE_PLASTIC_RAIN),
                        statuses = OrderStatus.activeStatuses,
                    )
                    .bind()
            },
            catch = { handleError(it) },
        )

        val conflictingOrder = OrderResolver.selectActive(
            orders = activeOrders,
            type = OrderType.CARD_REISSUE_PLASTIC_RAIN,
            productInstanceId = sourceProductInstanceId,
        )
        if (conflictingOrder != null) raise(VisaApiError.CardReissuePlasticActiveOrderExists)

        val order = catch(
            block = {
                customerOrderRepository
                    .createPlasticReissueOrder(
                        userWalletId = userWalletId,
                        sourceProductInstanceId = sourceProductInstanceId,
                        order = plasticCardOrder,
                        idempotencyKey = idempotencyKey,
                    )
                    .bind()
            },
            catch = { handleError(it) },
        )

        appCoroutineScope.launch {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(order),
                userWalletId = userWalletId,
            )
        }

        order
    }

    private fun Raise<VisaApiError>.handleError(throwable: Throwable): Nothing {
        TangemLogger.e("Error in ReissuePlasticCardUseCase", throwable)
        raise(VisaApiError.Unspecified)
    }
}