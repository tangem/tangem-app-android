package com.tangem.features.earn.lending.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.earn.lending.impl.session.LendingOperation

internal sealed class LendingAnalyticsEvent(
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(category = "ETH Lending", event = event, params = params) {

    data class TransactionSigned(
        private val operation: LendingOperation,
        private val isWithUpgrade: Boolean,
        private val amountBucket: String?,
    ) : LendingAnalyticsEvent(
        event = "Transaction Signed",
        params = buildMap {
            putAll(transactionParams(operation, isWithUpgrade))
            amountBucket?.let { put(AMOUNT_BUCKET, it) }
        },
    )

    data class TransactionSubmitted(
        private val operation: LendingOperation,
        private val isWithUpgrade: Boolean,
    ) : LendingAnalyticsEvent(
        event = "Transaction Submitted",
        params = transactionParams(operation, isWithUpgrade),
    )

    data class TransactionFailed(
        private val operation: LendingOperation,
        private val isWithUpgrade: Boolean,
        private val errorCode: String,
    ) : LendingAnalyticsEvent(
        event = "Error - Transaction Failed",
        params = transactionParams(operation, isWithUpgrade) + (AnalyticsParam.ERROR_CODE to errorCode),
    )

    private companion object {
        const val TRANSACTION_TYPE = "Transaction Type"
        const val WITH_UPGRADE = "With Upgrade"
        const val AMOUNT_BUCKET = "Amount Bucket"

        fun transactionParams(operation: LendingOperation, isWithUpgrade: Boolean): Map<String, String> = mapOf(
            TRANSACTION_TYPE to when (operation) {
                LendingOperation.Deposit -> "Deposit"
                LendingOperation.Withdraw -> "Withdraw"
            },
            WITH_UPGRADE to isWithUpgrade.toString(),
        )
    }
}