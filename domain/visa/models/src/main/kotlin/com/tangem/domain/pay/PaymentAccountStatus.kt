package com.tangem.domain.pay

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
sealed class PaymentAccountStatus {

    abstract val source: StatusSource

    @Serializable
    data object Loading : PaymentAccountStatus() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    @Serializable
    data object NotCreated : PaymentAccountStatus() {
        override val source: StatusSource = StatusSource.ACTUAL
    }

    @Serializable
    data class UnderReview(
        override val source: StatusSource,
        val kycStatus: KycStatus,
    ) : PaymentAccountStatus()

    @Serializable
    data class IssuingCard(override val source: StatusSource) : PaymentAccountStatus()

    @Serializable
    data class Locked(override val source: StatusSource) : PaymentAccountStatus()

    @Serializable
    data class Loaded(
        override val source: StatusSource,
        val cardId: String,
        val lastFourDigits: String,
        val balance: SerializedBigDecimal,
        val currencyCode: String,
        val depositAddress: String?,
        val isPinSet: Boolean,
    ) : PaymentAccountStatus()

    @Serializable
    sealed class Error : PaymentAccountStatus() {
        @Serializable
        data object ExposedDevice : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        @Serializable
        data class Unavailable(override val source: StatusSource) : Error()

        @Serializable
        data object NotSynced : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }

        @Serializable
        data object CardIssueFailed : Error() {
            override val source: StatusSource = StatusSource.ACTUAL
        }
    }
}