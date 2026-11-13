package com.tangem.features.earn.lending.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class LendingAnalyticsEvent(
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(category = "ETH Lending", event = event, params = params) {

    data class TransactionSigned(
        private val transaction: LendingAnalyticsTransaction,
        private val amountBucket: String?,
    ) : LendingAnalyticsEvent(
        event = "Transaction Signed",
        params = buildMap {
            putAll(transaction.toParams())
            amountBucket?.let { put(AMOUNT_BUCKET, it) }
        },
    )

    data class TransactionSubmitted(
        private val transaction: LendingAnalyticsTransaction,
    ) : LendingAnalyticsEvent(event = "Transaction Submitted", params = transaction.toParams())

    data class TransactionFailed(
        private val transaction: LendingAnalyticsTransaction,
        private val errorCode: String,
    ) : LendingAnalyticsEvent(
        event = "Error - Transaction Failed",
        params = transaction.toParams() + (AnalyticsParam.ERROR_CODE to errorCode),
    )

    private companion object {
        const val TRANSACTION_TYPE = "Transaction Type"
        const val WITH_UPGRADE = "With Upgrade"
        const val AMOUNT_BUCKET = "Amount Bucket"

        fun LendingAnalyticsTransaction.toParams(): Map<String, String> = mapOf(
            TRANSACTION_TYPE to type.value,
            WITH_UPGRADE to isWithUpgrade.toString(),
        )
    }
}

internal data class LendingAnalyticsTransaction(
    val type: Type,
    val isWithUpgrade: Boolean,
) {

    enum class Type(val value: String) {
        Deposit("Deposit"),
        Withdraw("Withdraw"),
    }
}