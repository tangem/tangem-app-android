package com.tangem.domain.onramp.model

import kotlinx.serialization.Serializable

@Serializable
data class OnrampPaymentMethod(
    val id: String,
    val name: String,
    val imageUrl: String,
    val type: PaymentMethodType,
)

enum class PaymentMethodType(val id: String?) {
    GOOGLE_PAY(id = "google-pay"),
    CARD(id = "card"),
    OTHER(id = null),
    ;

    fun getPriority(isGooglePayEnabled: Boolean): Int = if (isGooglePayEnabled) {
        when (this) {
            GOOGLE_PAY -> 0
            CARD -> 1
            OTHER -> 2
        }
    } else {
        when (this) {
            CARD -> 0
            GOOGLE_PAY -> 1
            OTHER -> 2
        }
    }

    companion object {
        fun getType(id: String): PaymentMethodType = when (id) {
            GOOGLE_PAY.id -> GOOGLE_PAY
            CARD.id -> CARD
            else -> OTHER
        }
    }
}