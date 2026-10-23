package com.tangem.domain.pay.model

import java.util.Locale

data class CustomerSummary(
    val customerId: String,
    val customerStatus: CustomerStatus,
    val isHidden: Boolean,
    val kycStatus: KycStatus,
    val paymentAccount: PaymentAccount,
    val moneyAccount: MoneyAccount,
) {

    enum class CustomerStatus {
        NEW,
        ACTIVE,
        BLOCKED,
        FORMER,
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String): CustomerStatus = when (value.uppercase(Locale.US)) {
                "NEW" -> NEW
                "ACTIVE" -> ACTIVE
                "BLOCKED" -> BLOCKED
                "FORMER" -> FORMER
                else -> UNKNOWN
            }
        }
    }

    enum class KycStatus {
        REQUIRED,
        IN_PROGRESS,
        APPROVED,
        DECLINED,
        UNKNOWN,
        ;

        companion object {
            fun fromString(value: String): KycStatus = when (value.uppercase(Locale.US)) {
                "REQUIRED" -> REQUIRED
                "IN_PROGRESS" -> IN_PROGRESS
                "APPROVED" -> APPROVED
                "DECLINED" -> DECLINED
                else -> UNKNOWN
            }
        }
    }

    data class PaymentAccount(
        val id: String?,
        val activation: Activation,
        val cards: Cards,
    ) {

        data class Activation(
            val status: Status,
            val step: Step?,
            val failureReason: FailureReason?,
        ) {

            enum class Status {
                NOT_STARTED,
                UNAVAILABLE,
                PROCESSING,
                FAILED,
                COMPLETED,
                UNKNOWN,
                ;

                companion object {
                    fun fromString(value: String): Status = when (value.uppercase(Locale.US)) {
                        "NOT_STARTED" -> NOT_STARTED
                        "UNAVAILABLE" -> UNAVAILABLE
                        "PROCESSING" -> PROCESSING
                        "FAILED" -> FAILED
                        "COMPLETED" -> COMPLETED
                        else -> UNKNOWN
                    }
                }
            }

            enum class Step {
                AWAITING_KYC,
                AWAITING_DEPOSIT,
                ISSUING,
                UNKNOWN,
                ;

                companion object {
                    fun fromString(value: String): Step = when (value.uppercase(Locale.US)) {
                        "AWAITING_KYC" -> AWAITING_KYC
                        "AWAITING_DEPOSIT" -> AWAITING_DEPOSIT
                        "ISSUING" -> ISSUING
                        else -> UNKNOWN
                    }
                }
            }

            enum class FailureReason {
                RAIN_REJECTED,
                DEPOSIT_TIMEOUT,
                TECHNICAL,
                UNKNOWN,
                ;

                companion object {
                    fun fromString(value: String): FailureReason = when (value.uppercase(Locale.US)) {
                        "RAIN_REJECTED" -> RAIN_REJECTED
                        "DEPOSIT_TIMEOUT" -> DEPOSIT_TIMEOUT
                        "TECHNICAL" -> TECHNICAL
                        else -> UNKNOWN
                    }
                }
            }
        }
    }

    data class MoneyAccount(
        val id: String?,
        val activation: Activation,
    ) {

        data class Activation(
            val status: Status,
            val step: Step?,
            val failureReason: FailureReason?,
        ) {

            enum class Status {
                NOT_STARTED,
                UNAVAILABLE,
                PROCESSING,
                FAILED,
                COMPLETED,
                DEACTIVATED,
                UNKNOWN,
                ;

                companion object {
                    fun fromString(value: String): Status = when (value.uppercase(Locale.US)) {
                        "NOT_STARTED" -> NOT_STARTED
                        "UNAVAILABLE" -> UNAVAILABLE
                        "PROCESSING" -> PROCESSING
                        "FAILED" -> FAILED
                        "COMPLETED" -> COMPLETED
                        "DEACTIVATED" -> DEACTIVATED
                        else -> UNKNOWN
                    }
                }
            }

            enum class Step {
                AWAITING_KYC,
                ISSUING,
                UNKNOWN,
                ;

                companion object {
                    fun fromString(value: String): Step = when (value.uppercase(Locale.US)) {
                        "AWAITING_KYC" -> AWAITING_KYC
                        "ISSUING" -> ISSUING
                        else -> UNKNOWN
                    }
                }
            }

            enum class FailureReason {
                RAIN_REJECTED,
                TECHNICAL,
                UNKNOWN,
                ;

                companion object {
                    fun fromString(value: String): FailureReason = when (value.uppercase(Locale.US)) {
                        "RAIN_REJECTED" -> RAIN_REJECTED
                        "TECHNICAL" -> TECHNICAL
                        else -> UNKNOWN
                    }
                }
            }
        }
    }

    data class Cards(
        val active: Int,
        val frozen: Int,
        val pending: Int,
    )
}

val CustomerSummary.KycStatus.inProgress: Boolean
    get() = when (this) {
        CustomerSummary.KycStatus.IN_PROGRESS,
        CustomerSummary.KycStatus.UNKNOWN,
        -> true
        CustomerSummary.KycStatus.REQUIRED,
        CustomerSummary.KycStatus.DECLINED,
        CustomerSummary.KycStatus.APPROVED,
        -> false
    }