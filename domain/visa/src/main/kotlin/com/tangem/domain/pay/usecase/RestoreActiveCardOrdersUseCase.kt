package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayIssueCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch

/**
 * Restores in-flight card-issuance and card-activation orders on app launch / when returning to the wallet
 * screen.
 *
 * `findOrders` is the source of truth — a locally stored order id is only a hint that does not

 * survive a force close. This use case re-discovers the active orders and (re)starts polling so each is
 * driven to its terminal state. Only issue orders get their ids persisted, because only those render an
 * "issuing" placeholder card — an activation order is already reflected by the card's own state, and
 * persisting its id would add a phantom placeholder next to the real card.
 *
 * Non-fatal exceptions are logged and collapsed to [VisaApiError.Unspecified]; the caller treats the
 * result as fire-and-forget.
 *
 * @property customerOrderRepository source of truth for active orders.
 * @property issueCardRepository persists issue-order ids for placeholder rendering.
 * @property startTangemPayOrderPollingUseCase drives a restored order to its terminal state.
 */
class RestoreActiveCardOrdersUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val issueCardRepository: TangemPayIssueCardRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Unit> = either {
        val orders = catch(
            block = {
                customerOrderRepository.findOrders(
                    userWalletId = userWalletId,
                    types = OrderType.issueCardTypes + OrderType.CARD_ACTIVATION_PLASTIC_RAIN,
                    statuses = OrderStatus.activeStatuses,
                ).bind()
            },
            catch = { handleError(it) },
        ).filter { it.status.isActive }

        orders.forEach { order ->
            if (order.type.isIssuing) {
                issueCardRepository.storeIssueOrderId(userWalletId = userWalletId, orderId = order.id)
            }

            appCoroutineScope.launch {
                startTangemPayOrderPollingUseCase(
                    order = TangemPayOrderInfo.fromOrder(order),
                    userWalletId = userWalletId,
                    onOrderStateChange = { newOrder ->
                        if (newOrder.orderStatus.isTerminal && order.type.isIssuing) {
                            issueCardRepository.removeIssueOrderId(userWalletId, order.id)
                        }
                    },
                )
            }
        }
    }

    private fun Raise<VisaApiError>.handleError(throwable: Throwable): Nothing {
        TangemLogger.e("Error in RestoreActiveCardOrdersUseCase", throwable)
        raise(VisaApiError.Unspecified)
    }
}