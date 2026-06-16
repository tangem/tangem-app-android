package com.tangem.domain.pay.model

import java.math.BigDecimal
import java.util.Currency

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
) {

    data class Data(val specificationName: String, val orderType: OrderType)

    /** Offer type — unknown wire values resolve to [UNKNOWN]. */
    enum class Type(val wireValue: String) {
        CARD_ISSUE_VIRTUAL_RAIN("CARD_ISSUE_VIRTUAL_RAIN"),
        UNKNOWN(""),
        ;

        companion object {
            fun fromString(value: String?): Type {
                if (value.isNullOrBlank()) return UNKNOWN
                return entries.firstOrNull { it.wireValue == value || it.name == value } ?: UNKNOWN
            }
        }
    }

    data class Fee(
        val amount: BigDecimal,
        val currency: Currency,
    )
}