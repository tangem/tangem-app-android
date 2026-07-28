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
 * Restores in-flight card-issuance orders on app launch / when returning to the wallet screen.
 *
 * `findOrders` is the source of truth — a locally stored order id is only a hint that does not

 * survive a force close. This use case re-discovers the active issue orders, persists their ids so
 * the payment-account state renders an "issuing" placeholder card, and (re)starts polling so the
 * placeholder is driven to its terminal state.
 *
 * Non-fatal exceptions are logged and collapsed to [VisaApiError.Unspecified]; the caller treats the
 * result as fire-and-forget.
 *
 * @property customerOrderRepository source of truth for active orders.
 * @property issueCardRepository persists issue-order ids for placeholder rendering.
 * @property startTangemPayOrderPollingUseCase drives a restored order to its terminal state.
 */
class RestoreActiveIssueOrdersUseCase(
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
                    types = OrderType.issueCardTypes,
                    statuses = OrderStatus.activeStatuses,
                ).bind()
            },
            catch = { handleError(it) },
        ).filter { it.status.isActive }

        orders.forEach { order ->
            issueCardRepository.storeIssueOrderId(userWalletId = userWalletId, orderId = order.id)

            appCoroutineScope.launch {
                startTangemPayOrderPollingUseCase(
                    order = TangemPayOrderInfo(orderId = order.id, orderStatus = order.status),
                    userWalletId = userWalletId,
                    onTerminalReached = { issueCardRepository.removeIssueOrderId(userWalletId, order.id) },
                )
            }
        }
    }

    private fun Raise<VisaApiError>.handleError(throwable: Throwable): Nothing {
        TangemLogger.e("Error in RestoreActiveIssueOrdersUseCase", throwable)
        raise(VisaApiError.Unspecified)
    }
}