package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.IssueAdditionalCardError
import com.tangem.domain.pay.model.IssueAdditionalCardResult
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.model.deeplinkOffer
import com.tangem.domain.pay.model.virtualOffer
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayIssueCardRepository
import com.tangem.domain.pay.util.OrderResolver
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Orchestrates the issue-additional-card flow. The use case is idempotent and resume-safe:
 *
 * 1. Eligibility — fetches the customer's offers and confirms a [Offer.Type.CARD_ISSUE_VIRTUAL_RAIN]
 *    offer is available; otherwise returns [IssueAdditionalCardError.Other] with [VisaApiError.Unspecified].
 *    With a [deeplinkValue][invoke] the campaign offer ([Offer.Type.CARD_ISSUE_VIRTUAL_RAIN_DEEPLINK]) is
 *    required instead; its absence is [IssueAdditionalCardError.NoCampaignOffer].
 * 2. Resume — looks up active orders of the offer's [Offer.Data.orderType] and, if one is in flight,
 *    returns it as [IssueAdditionalCardResult.Resumed] instead of creating a duplicate (find-before-create).
 *    On the campaign path any active virtual issue order counts, whatever its type.
 * 3. Create — otherwise issues `POST /v1/order` with the offer's specification name and a fresh
 *    idempotency key; backend failures propagate as [Either.Left].
 *
 * Non-fatal exceptions from either repository are logged and collapsed to [VisaApiError.Unspecified]
 * so the caller always receives a typed [Either].
 *
 * @property customerOffersRepository source of the customer's currently available offers.
 * @property customerOrderRepository  used to look up active orders and create new ones.
 */
class IssueAdditionalCardUseCase(
    private val customerOffersRepository: CustomerOffersRepository,
    private val customerOrderRepository: CustomerOrderRepository,
    private val issueCardRepository: TangemPayIssueCardRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        deeplinkValue: String? = null,
    ): Either<IssueAdditionalCardError, IssueAdditionalCardResult> = either {
        val offer = resolveOffer(userWalletId = userWalletId, deeplinkValue = deeplinkValue)

        val activeOrders = catch(
            block = {
                customerOrderRepository
                    .findOrders(
                        userWalletId = userWalletId,
                        types = setOf(offer.data.orderType) + OrderType.issueCardTypes,
                        statuses = OrderStatus.activeStatuses,
                    )
                    .mapLeft(VisaApiError::toIssueError)
                    .bind()
            },
            catch = { handleError(it) },
        )

        val existing = if (deeplinkValue == null) {
            OrderResolver.selectActive(orders = activeOrders, type = offer.data.orderType)
        } else {
            OrderResolver.selectActiveByTypes(orders = activeOrders, types = OrderType.virtualIssueCardTypes)
        }
        val result = if (existing != null) {
            IssueAdditionalCardResult.Resumed(existing)
        } else {
            val created = customerOrderRepository.createOrder(
                userWalletId = userWalletId,
                type = offer.data.orderType,
                specificationName = offer.data.specificationName
                    ?: raise(IssueAdditionalCardError.Other(VisaApiError.Unspecified)),
                idempotencyKey = UUID.randomUUID().toString(),
                deeplinkValue = deeplinkValue,
            ).mapLeft(VisaApiError::toIssueError).bind()
            IssueAdditionalCardResult.Created(created)
        }

        trackOrder(userWalletId = userWalletId, order = result.order)
        result
    }

    private suspend fun Raise<IssueAdditionalCardError>.resolveOffer(
        userWalletId: UserWalletId,
        deeplinkValue: String?,
    ): Offer {
        val offers = catch(
            block = {
                customerOffersRepository
                    .getOffers(userWalletId = userWalletId, deeplinkValue = deeplinkValue)
                    .mapLeft(VisaApiError::toIssueError)
                    .bind()
                    .orderable
            },
            catch = { handleError(it) },
        )
        return if (deeplinkValue == null) {
            offers.virtualOffer() ?: raise(IssueAdditionalCardError.Other(VisaApiError.Unspecified))
        } else {
            offers.deeplinkOffer() ?: raise(IssueAdditionalCardError.NoCampaignOffer)
        }
    }

    private suspend fun trackOrder(userWalletId: UserWalletId, order: Order) {
        issueCardRepository.storeIssueOrderId(userWalletId = userWalletId, orderId = order.id)

        appCoroutineScope.launch {
            startTangemPayOrderPollingUseCase(
                order = TangemPayOrderInfo.fromOrder(order),
                userWalletId = userWalletId,
                onOrderStateChange = { newOrder ->
                    if (newOrder.orderStatus.isTerminal) {
                        issueCardRepository.removeIssueOrderId(userWalletId, order.id)
                    }
                },
            )
        }
    }

    private fun Raise<IssueAdditionalCardError>.handleError(throwable: Throwable): Nothing {
        TangemLogger.e("Error in IssueAdditionalCardUseCase", throwable)
        raise(IssueAdditionalCardError.Other(VisaApiError.Unspecified))
    }
}

private fun VisaApiError.toIssueError(): IssueAdditionalCardError = when (this) {
    VisaApiError.CardIssueOfferNotAvailable -> IssueAdditionalCardError.OfferNotAvailable
    else -> IssueAdditionalCardError.Other(this)
}