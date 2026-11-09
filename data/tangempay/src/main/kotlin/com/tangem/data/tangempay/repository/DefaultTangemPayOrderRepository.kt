package com.tangem.data.tangempay.repository

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.data.tangempay.util.OrderRequestConverter
import com.tangem.data.tangempay.util.TangemPayOrderConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import com.tangem.spend.datasource.tangempay.TangemPayOrderApi
import javax.inject.Inject

internal class DefaultTangemPayOrderRepository @Inject constructor(
    private val orderApi: TangemPayOrderApi,
    private val requestHelper: TangemPayRequestPerformer,
) : TangemPayOrderRepository {

    override suspend fun getOrder(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<TangemPayApiError, TangemPayOrder> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            orderApi.getOrder(authHeader = authHeader, orderId = orderId)
        }.mapLeft { error ->
            if (error is TangemPayApiError.NotFound) TangemPayApiError.OrderNotFound else error
        }.map { response ->
            TangemPayOrderConverter.convert(response.result)
        }
    }

    override suspend fun findOrders(
        userWalletId: UserWalletId,
        types: Set<TangemPayOrder.Type>,
        statuses: Set<TangemPayOrder.Status>,
    ): Either<TangemPayApiError, List<TangemPayOrder>> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            orderApi.findOrders(
                authHeader = authHeader,
                orderTypes = types.map(::toWireType),
                orderStatuses = statuses.mapNotNull(::toWireStatus),
            )
        }.map { response ->
            response.result.map(TangemPayOrderConverter::convert)
        }
    }

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: CustomerOrderRequest,
        idempotencyKey: String,
    ): Either<TangemPayApiError, TangemPayOrder> = either {
        val customerWalletAddress = catch({ requestHelper.getCustomerWalletAddress(userWalletId) }) { _ ->
            raise(TangemPayApiError.UnknownWithoutCode)
        }
        val body = OrderRequestConverter.convert(
            OrderRequestConverter.Input(customerWalletAddress = customerWalletAddress, request = request),
        ).bind()
        val response = requestHelper.performRequest(userWalletId) { authHeader ->
            orderApi.createOrder(authHeader = authHeader, idempotencyKey = idempotencyKey, body = body)
        }.bind()
        TangemPayOrderConverter.convert(response.result)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun toWireType(type: TangemPayOrder.Type): String = when (type) {
        TangemPayOrder.Type.CARD_ISSUE_VIRTUAL -> "CARD_ISSUE_VIRTUAL_RAIN"
        TangemPayOrder.Type.CARD_ISSUE_VIRTUAL_KYC -> "CARD_ISSUE_VIRTUAL_RAIN_KYC"
        TangemPayOrder.Type.CARD_ISSUE_VIRTUAL_KYC_V2 -> "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2"
        TangemPayOrder.Type.CARD_ISSUE_PLASTIC -> "CARD_ISSUE_PLASTIC_RAIN"
        TangemPayOrder.Type.CARD_ACTIVATION_PLASTIC -> "CARD_ACTIVATION_PLASTIC_RAIN"
        TangemPayOrder.Type.CARD_REISSUE_PLASTIC -> "CARD_REISSUE_PLASTIC_RAIN"
        TangemPayOrder.Type.CARD_REISSUE_VIRTUAL -> "CARD_REISSUE_VIRTUAL_RAIN"
        TangemPayOrder.Type.CARD_FREEZE -> "CARD_FREEZE_RAIN"
        TangemPayOrder.Type.CARD_UNFREEZE -> "CARD_UNFREEZE_RAIN"
        TangemPayOrder.Type.CARD_CLOSE -> "CARD_CLOSE_RAIN"
        TangemPayOrder.Type.ACCOUNT_ISSUE_VIRTUAL -> "ACCOUNT_ISSUE_VIRTUAL_RAIN"
        TangemPayOrder.Type.ACCOUNT_ISSUE_BANKING -> "ACCOUNT_ISSUE_BANKING_RAIN"
        TangemPayOrder.Type.WITHDRAWAL -> "WITHDRAWAL_RAIN"
        TangemPayOrder.Type.BANK_OFFRAMP -> "BANK_OFFRAMP_RAIN"
        TangemPayOrder.Type.SMART_CONTRACT_ISSUE -> "SMART_CONTRACT_ISSUE_RAIN"
        TangemPayOrder.Type.TARIFF_PLAN_TRANSITION -> "TARIFF_PLAN_TRANSITION"
        TangemPayOrder.Type.ONBOARDING -> "ONBOARDING_RAIN"
        TangemPayOrder.Type.UNDEFINED -> "UNDEFINED"
    }

    private fun toWireStatus(status: TangemPayOrder.Status): String? = when (status) {
        TangemPayOrder.Status.NEW -> "NEW"
        TangemPayOrder.Status.PROCESSING -> "PROCESSING"
        TangemPayOrder.Status.COMPLETED -> "COMPLETED"
        TangemPayOrder.Status.CANCELED -> "CANCELED"
        TangemPayOrder.Status.UNKNOWN -> null
    }
}