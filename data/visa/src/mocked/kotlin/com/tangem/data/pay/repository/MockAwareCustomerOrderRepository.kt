package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerOrderRequest
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.config.TangemPay
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MockAwareCustomerOrderRepository @Inject constructor(
    private val real: DefaultCustomerOrderRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : CustomerOrderRepository {

    /**

     * The mocked `customer/me` is served by a remote stub that cannot flip the card to `ACTIVE`, so these
     * orders never complete — restarting the app clears them.
     */
    private val mockActivationOrders = ConcurrentHashMap<String, Order>()

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override suspend fun getOrderData(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, OrderData> =
        real.getOrderData(userWalletId, orderId)

    override suspend fun findOrders(
        userWalletId: UserWalletId,
        types: Set<OrderType>,
        statuses: Set<OrderStatus>,
    ): Either<VisaApiError, List<Order>> {
        if (!isMockMode) return real.findOrders(userWalletId, types, statuses)

        val mocked = mockActivationOrders.values.filter { it.type in types && it.status in statuses }
        return real.findOrders(userWalletId, types, statuses)
            .fold(ifLeft = { mocked.right() }, ifRight = { (it + mocked).right() })
    }

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: CustomerOrderRequest,
        idempotencyKey: String,
    ): Either<VisaApiError, Order> {
        if (!isMockMode || request !is CustomerOrderRequest.CardActivation) {
            return real.createOrder(userWalletId = userWalletId, request = request, idempotencyKey = idempotencyKey)
        }
        val order = request.order
        if (order.lastFourDigits != MOCK_VALID_LAST_DIGITS) return VisaApiError.CardActivationInvalidCardData.left()

        val created = Order(
            id = "$MOCK_ORDER_ID_PREFIX${order.productInstanceId}",
            customerId = null,
            type = OrderType.CARD_ACTIVATION_PLASTIC_RAIN,
            status = OrderStatus.PROCESSING,
            step = OrderStep.UNKNOWN,
            stepChangeCode = null,
            productInstanceId = order.productInstanceId,
            paymentAccountId = null,
            cardId = null,
            toTariffPlanId = null,
            withdrawTxHash = null,
            createdAt = null,
            updatedAt = null,
        )
        mockActivationOrders[order.productInstanceId] = created
        return created.right()
    }

    override suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<VisaApiError, Unit> =
        real.cancelOrder(userWalletId, orderId)

    private companion object {
        const val MOCK_VALID_LAST_DIGITS = "8252"
        const val MOCK_ORDER_ID_PREFIX = "mock-card-activation-order-"
    }
}