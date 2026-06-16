package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.repository.CustomerOffersRepository
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.util.OrderResolver
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.logging.TangemLogger
import java.util.UUID

/**
 * Orchestrates the issue-additional-card flow. The use case is idempotent and resume-safe:
 *
 * 1. Eligibility — fetches the customer's offers and confirms a [Offer.Type.CARD_ISSUE_VIRTUAL_RAIN]
 *    offer is available; otherwise returns [VisaApiError.Unspecified].
 * 2. Resume — looks up active orders of the offer's [Offer.Data.orderType] and, if one is in flight,
 *    returns it instead of creating a duplicate (find-before-create).
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
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<VisaApiError, Result> = either {
        val offer = catch(
            block = {
                customerOffersRepository.getOffers(userWalletId)
                    .bind()
                    .firstOrNull { it.type == Offer.Type.CARD_ISSUE_VIRTUAL_RAIN }
            },
            catch = { handleError(it) },
        ) ?: raise(VisaApiError.Unspecified)

        val activeOrders = catch(
            block = {
                customerOrderRepository
                    .findOrders(userWalletId = userWalletId, types = setOf(offer.data.orderType))
                    .bind()
            },
            catch = { handleError(it) },
        )

        val existing = OrderResolver.selectActive(orders = activeOrders, type = offer.data.orderType)
        val order = existing ?: customerOrderRepository.createOrder(
            userWalletId = userWalletId,
            type = offer.data.orderType,
            specificationName = offer.data.specificationName,
            idempotencyKey = UUID.randomUUID().toString(),
        ).bind()

        Result(order = order, offer = offer)
    }

    private fun Raise<VisaApiError>.handleError(throwable: Throwable): Nothing {
        TangemLogger.e("Error in IssueAdditionalCardUseCase", throwable)
        raise(VisaApiError.Unspecified)
    }

    /**
     * Outcome of a successful run.
     *

     * @property offer the offer that authorised issuance, carried back so the caller can show pricing
     *   without an extra round trip.
     */
    data class Result(val order: Order, val offer: Offer)
}