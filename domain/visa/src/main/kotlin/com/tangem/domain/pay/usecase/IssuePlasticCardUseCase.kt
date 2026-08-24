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
import com.tangem.domain.pay.model.plasticOffer
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.util.OrderResolver
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch

class IssuePlasticCardUseCase(
    private val customerOffersRepository: CustomerOffersRepository,
    private val customerOrderRepository: CustomerOrderRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        plasticCardOrder: PlasticCardOrder,
        idempotencyKey: String,
    ): Either<VisaApiError, Order> = either {
        val offer = catch(
            block = { customerOffersRepository.getOffers(userWalletId).bind().plasticOffer() },
            catch = { handleError(it) },
        ) ?: raise(VisaApiError.CardIssueOfferNotAvailable)

        val activeOrders = catch(
            block = {
                customerOrderRepository
                    .findOrders(
                        userWalletId = userWalletId,
                        types = setOf(OrderType.CARD_ISSUE_PLASTIC_RAIN),
                        statuses = OrderStatus.activeStatuses,
                    )
                    .bind()
            },
            catch = { handleError(it) },
        )

        if (OrderResolver.selectActive(orders = activeOrders, type = OrderType.CARD_ISSUE_PLASTIC_RAIN) != null) {
            raise(VisaApiError.CardIssueActiveOrderExists)
        }

        val order = catch(
            block = {
                customerOrderRepository
                    .createPlasticIssueOrder(
                        userWalletId = userWalletId,
                        specificationName = offer.data.specificationName,
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
        TangemLogger.e("Error in IssuePlasticCardUseCase", throwable)
        raise(VisaApiError.Unspecified)
    }
}