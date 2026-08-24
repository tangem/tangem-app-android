package com.tangem.domain.pay.model

import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

/**
 * Customer offer returned by `GET /v1/customer/offers`.
 *
 * Used to gate the issue-additional-card flow: the offer fee drives the popup amount, and the
 * presence of the offer enables the "+" action.
 */
data class Offer(
    val type: Type,
    val fee: Fee,
    val data: Data,
    val mainImageUrl: String? = null,
) {

    val isPlastic: Boolean get() = type == Type.CARD_ISSUE_PLASTIC_RAIN

    data class Data(
        val specificationName: String,
        val orderType: OrderType,
        val deliveryEta: DeliveryEta? = null,
    )

    data class DeliveryEta(val minBusinessDays: Int?, val maxBusinessDays: Int)

    /** Offer type — unknown wire values resolve to [UNKNOWN]. */
    enum class Type(val wireValue: String) {
        CARD_ISSUE_VIRTUAL_RAIN("CARD_ISSUE_VIRTUAL_RAIN"),
        CARD_ISSUE_PLASTIC_RAIN("CARD_ISSUE_PLASTIC_RAIN"),
        UNKNOWN(""),
        ;

        companion object {
            fun fromString(value: String?): Type {
                if (value.isNullOrBlank()) return UNKNOWN
                val normalized = value.uppercase(Locale.US)
                return entries.firstOrNull { it.wireValue == normalized } ?: UNKNOWN
            }
        }
    }

    data class Fee(
        val amount: BigDecimal,
        val currency: Currency,
    )
}

fun List<Offer>.plasticOffer(): Offer? = firstOrNull(Offer::isPlastic)