package com.tangem.domain.pay.model

import com.tangem.domain.models.kyc.KycStatus
import java.util.Locale

data class CustomerSummary(
    val customerId: String,
    val customerStatus: CustomerInfo.State,
    val isHidden: Boolean,
    val kycStatus: KycStatus,
    val paymentAccount: PaymentAccount,
    val bankingAccount: BankingAccount,
) {

    data class PaymentAccount(
        val id: String?,
        val activation: Activation,
        val cards: Cards,
    )

    data class BankingAccount(
        val id: String?,
        val activation: Activation,
    )

    data class Activation(
        val status: Status,
        val step: Step?,
        val failureReason: FailureReason?,
    ) {

        enum class Status {
            NOT_STARTED,
            PROCESSING,
            FAILED,
            COMPLETED,
            DEACTIVATED,
            UNAVAILABLE,
            UNKNOWN,
            ;

            companion object {
                fun fromString(value: String): Status = when (value.uppercase(Locale.US)) {
                    "NOT_STARTED" -> NOT_STARTED
                    "PROCESSING" -> PROCESSING
                    "FAILED" -> FAILED
                    "COMPLETED" -> COMPLETED
                    "DEACTIVATED" -> DEACTIVATED
                    "UNAVAILABLE" -> UNAVAILABLE
                    else -> UNKNOWN
                }
            }
        }

        enum class Step {
            AWAITING_KYC,
            AWAITING_PLAN_SELECTION,
            AWAITING_DEPOSIT,
            ISSUING,
            UNKNOWN,
            ;

            companion object {
                fun fromString(value: String): Step = when (value.uppercase(Locale.US)) {
                    "AWAITING_KYC" -> AWAITING_KYC
                    "AWAITING_PLAN_SELECTION" -> AWAITING_PLAN_SELECTION
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

    data class Cards(
        val active: Int,
        val frozen: Int,
        val pending: Int,
    )
}