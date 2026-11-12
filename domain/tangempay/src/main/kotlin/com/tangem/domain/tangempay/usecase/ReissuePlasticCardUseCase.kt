package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.Order
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.model.PlasticCardOrder
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.repository.TangemPayReissueCardRepository
import com.tangem.domain.tangempay.util.OrderResolver
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch

class ReissuePlasticCardUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val reissueCardRepository: TangemPayReissueCardRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        sourceProductInstanceId: String,
        sourceCardId: String,
        plasticCardOrder: PlasticCardOrder,
        idempotencyKey: String,
    ): Either<TangemPayApiError, Order> = either {
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

        val conflictingOrder = OrderResolver.selectActiveBySource(
            orders = activeOrders,
            type = OrderType.CARD_REISSUE_PLASTIC_RAIN,
            sourceProductInstanceId = sourceProductInstanceId,
        )
        if (conflictingOrder != null) raise(TangemPayApiError.CardReissuePlasticActiveOrderExists)

        val order = catch(
            block = {
                customerOrderRepository
                    .createOrder(
                        userWalletId = userWalletId,
                        request = CustomerOrderRequest.PlasticCardReissue(
                            sourceProductInstanceId = sourceProductInstanceId,
                            order = plasticCardOrder,
                        ),
                        idempotencyKey = idempotencyKey,
                    )
                    .bind()
            },
            catch = { handleError(it) },
        )

        reissueCardRepository.storeReissueOrderId(sourceCardId, order.id)

        appCoroutineScope.launch {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(order),
                userWalletId = userWalletId,
                onOrderStateChange = { newOrder ->
                    if (newOrder.orderStatus.isTerminal) {
                        reissueCardRepository.removeReissueOrderId(sourceCardId)
                    }
                },
            )
        }

        order
    }

    private fun Raise<TangemPayApiError>.handleError(throwable: Throwable): Nothing {
        TangemLogger.e("Error in ReissuePlasticCardUseCase", throwable)
        raise(TangemPayApiError.Unspecified)
    }
}