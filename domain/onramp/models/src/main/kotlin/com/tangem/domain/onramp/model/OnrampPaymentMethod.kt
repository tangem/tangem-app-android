package com.tangem.domain.onramp.model

import kotlinx.serialization.Serializable

@Serializable
data class OnrampPaymentMethod(
    val id: String,
    val name: String,
    val imageUrl: String,
    val type: PaymentMethodType,
    val imageUrlLight: String? = null,
    val imageUrlDark: String? = null,
) {

    val hasThemedImages: Boolean
        get() = imageUrlLight != null || imageUrlDark != null

    /** Port of iOS imageURL(isDark:): prefer the theme-matching url, then the other themed url, then legacy [imageUrl]. */
    fun imageUrl(isDark: Boolean): String = (if (isDark) imageUrlDark else imageUrlLight)
        ?: (if (isDark) imageUrlLight else imageUrlDark)
        ?: imageUrl
}

enum class PaymentMethodType(val id: String?) {
    GOOGLE_PAY(id = "google-pay"),
    CARD(id = "card"),
    REVOLUT_PAY(id = "invoice-revolut-pay"),
    SEPA(id = "sepa"),
    MOONPAY_BALANCE(id = "moonpay-balance"),
    OTHER(id = null),
    ;

    /**
     * Get priority regardless of real speed. By business logic.
     */
    @Suppress("MagicNumber")
    fun getPriorityForMethod(isGooglePayEnabled: Boolean): Int = if (isGooglePayEnabled) {
        when (this) {
            GOOGLE_PAY -> 0
            CARD -> 1
            REVOLUT_PAY -> 2
            SEPA -> 3
            MOONPAY_BALANCE, OTHER -> 4
        }
    } else {
        when (this) {
            CARD -> 2
            REVOLUT_PAY -> 1
            SEPA -> 2
            OTHER -> 3
            MOONPAY_BALANCE, GOOGLE_PAY -> 4
        }
    }

    @Suppress("MagicNumber")
    fun getPriorityBySpeed(isGooglePayEnabled: Boolean): Int = if (isGooglePayEnabled) {
        when (this) {
            GOOGLE_PAY -> 0
            REVOLUT_PAY -> 1
            CARD -> 2
            SEPA -> 3
            MOONPAY_BALANCE, OTHER -> 4
        }
    } else {
        when (this) {
            REVOLUT_PAY -> 0
            CARD -> 1
            SEPA -> 2
            MOONPAY_BALANCE, OTHER -> 3
            GOOGLE_PAY -> 4
        }
    }

    /**
     * BE AWARE. HARDCODED. Returns the speed of transaction for payment method type.
     */
    fun getProcessingSpeed(): PaymentSpeed = when (this) {
        GOOGLE_PAY,
        REVOLUT_PAY,
        MOONPAY_BALANCE,
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
            MOONPAY_BALANCE.id -> MOONPAY_BALANCE
            else -> OTHER
        }
    }
}