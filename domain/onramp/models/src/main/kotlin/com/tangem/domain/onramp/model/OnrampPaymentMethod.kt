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
    REVOLUT_PAY(id = "invoice-revolut-pay"),
    SEPA(id = "sepa"),
    OTHER(id = null),
    ;

    @Suppress("MagicNumber")
    fun getPriority(isGooglePayEnabled: Boolean): Int = if (isGooglePayEnabled) {
        when (this) {
            GOOGLE_PAY -> 0
            CARD -> 1
            SEPA -> 2
            REVOLUT_PAY -> 3
            OTHER -> 4
        }
    } else {
        when (this) {
            CARD -> 0
            GOOGLE_PAY -> 1
            SEPA -> 2
            REVOLUT_PAY -> 3
            OTHER -> 4
        }
    }

    /**
     * BE AWARE. HARDCODED. Returns the speed of transaction for payment method type.
     */
    fun getProcessingSpeed(): PaymentSpeed = when (this) {
        REVOLUT_PAY,
        GOOGLE_PAY,
        -> PaymentSpeed.Instant
        CARD -> PaymentSpeed.FewMin
        SEPA -> PaymentSpeed.FewDays
        OTHER -> PaymentSpeed.PlentyDays
    }

    /**
     * @param speed - the lower the value, the faster the speed.
     */
    @Suppress("MagicNumber")
    enum class PaymentSpeed(val speed: Int) {
        Instant(0), FewMin(1), FewDays(2), PlentyDays(3), Unknown(4)
    }

    fun isInstant(): Boolean = getProcessingSpeed() == PaymentSpeed.Instant

    companion object {

        fun getType(id: String): PaymentMethodType = when (id) {
            GOOGLE_PAY.id -> GOOGLE_PAY
            CARD.id -> CARD
            REVOLUT_PAY.id -> REVOLUT_PAY
            SEPA.id -> SEPA
            else -> OTHER
        }
    }
}