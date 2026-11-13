package com.tangem.features.earn.lending.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.features.earn.lending.impl.session.LendingOperation
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LendingAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN lending event WHEN created THEN id and params match the spec`(model: EventModel) {
        // Assert
        assertThat(model.event.id).isEqualTo(model.expectedId)
        assertThat(model.event.params).isEqualTo(model.expectedParams)
    }

    data class EventModel(
        val name: String,
        val event: AnalyticsEvent,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    ) {
        override fun toString(): String = name
    }

    private fun provideTestModels() = listOf(
        EventModel(
            name = "signed deposit with upgrade and amount bucket",
            event = LendingAnalyticsEvent.TransactionSigned(
                operation = LendingOperation.Deposit,
                isWithUpgrade = true,
                amountBucket = "250-1K",
            ),
            expectedId = "[ETH Lending] Transaction Signed",
            expectedParams = mapOf(
                "Transaction Type" to "Deposit",
                "With Upgrade" to "true",
                "Amount Bucket" to "250-1K",
            ),
        ),
        EventModel(
            name = "signed withdraw without upgrade and amount bucket",
            event = LendingAnalyticsEvent.TransactionSigned(
                operation = LendingOperation.Withdraw,
                isWithUpgrade = false,
                amountBucket = null,
            ),
            expectedId = "[ETH Lending] Transaction Signed",
            expectedParams = mapOf("Transaction Type" to "Withdraw", "With Upgrade" to "false"),
        ),
        EventModel(
            name = "submitted deposit with upgrade",
            event = LendingAnalyticsEvent.TransactionSubmitted(LendingOperation.Deposit, isWithUpgrade = true),
            expectedId = "[ETH Lending] Transaction Submitted",
            expectedParams = mapOf("Transaction Type" to "Deposit", "With Upgrade" to "true"),
        ),
        EventModel(
            name = "submitted withdraw without upgrade",
            event = LendingAnalyticsEvent.TransactionSubmitted(LendingOperation.Withdraw, isWithUpgrade = false),
            expectedId = "[ETH Lending] Transaction Submitted",
            expectedParams = mapOf("Transaction Type" to "Withdraw", "With Upgrade" to "false"),
        ),
        EventModel(
            name = "failed",
            event = LendingAnalyticsEvent.TransactionFailed(
                operation = LendingOperation.Deposit,
                isWithUpgrade = true,
                errorCode = "NetworkError: 500",
            ),
            expectedId = "[ETH Lending] Error - Transaction Failed",
            expectedParams = mapOf(
                "Transaction Type" to "Deposit",
                "With Upgrade" to "true",
                "Error Code" to "NetworkError: 500",
            ),
        ),
    )
}