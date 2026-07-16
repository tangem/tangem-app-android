package com.tangem.domain.pay.model

/**
 * Domain model for a TangemPay order returned by `GET /v1/order` (findOrders) or `GET /v1/order/{id}`.
 *
 * Each order carries enough context to be matched to the originating card / product instance
 * for card-scoped flows.
 *
 * @property id backend order identifier.
 * @property type order type; unknown values resolve to [OrderType.UNKNOWN].
 * @property status current order status.
 * @property step optional per-status step indicator (KYC / Rain / Issue / Fee / Activation / …).
 * @property stepChangeCode optional code accompanying step transitions.
 * @property productInstanceId set for card-scoped orders.
 * @property paymentAccountId set for card-scoped and payment-account-level orders.
 * @property cardId set for card-scoped orders that are filtered by card.
 * @property withdrawTxHash present for completed [OrderType.WITHDRAW] orders.
 * @property updatedAt ISO-8601 timestamp used to pick the most recent matching order.
 */
data class Order(
    val id: String,
    val customerId: String?,
    val type: OrderType,
    val status: OrderStatus,
    val step: String?,
    val stepChangeCode: Int?,
    val productInstanceId: String?,
    val paymentAccountId: String?,
    val cardId: String?,
    val withdrawTxHash: String?,
    val createdAt: String?,
    val updatedAt: String?,
) {
    /** True when the order belongs to a specific card/product instance (vs payment-account-level). */
    val isCardScoped: Boolean get() = productInstanceId != null

    /** True when the order is still in flight. */
    val isActive: Boolean get() = status.isActive
}