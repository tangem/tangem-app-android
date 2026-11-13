package com.tangem.features.earn.lending.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LendingAnalyticsEventTest {

    private val deposit = LendingAnalyticsTransaction(LendingAnalyticsTransaction.Type.Deposit, isWithUpgrade = true)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN lending event WHEN created THEN id and params match the spec`(model: EventModel) {
        // Assert
        assertThat(model.event.id).isEqualTo(model.expectedId)
        assertThat(model.event.params).isEqualTo(model.expectedParams)
    }

    @Test
    fun `GIVEN withdraw without upgrade WHEN submitted THEN type and upgrade map to spec values`() {
        // Arrange
        val withdraw = LendingAnalyticsTransaction(LendingAnalyticsTransaction.Type.Withdraw, isWithUpgrade = false)

        // Act
        val event = LendingAnalyticsEvent.TransactionSubmitted(withdraw)

        // Assert
        assertThat(event.params).isEqualTo(mapOf("Transaction Type" to "Withdraw", "With Upgrade" to "false"))
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
            name = "signed with amount bucket",
            event = LendingAnalyticsEvent.TransactionSigned(deposit, amountBucket = "250-1K"),
            expectedId = "[ETH Lending] Transaction Signed",
            expectedParams = mapOf(
                "Transaction Type" to "Deposit",
                "With Upgrade" to "true",
                "Amount Bucket" to "250-1K",
            ),
        ),
        EventModel(
            name = "signed without amount bucket",
            event = LendingAnalyticsEvent.TransactionSigned(deposit, amountBucket = null),
            expectedId = "[ETH Lending] Transaction Signed",
            expectedParams = mapOf("Transaction Type" to "Deposit", "With Upgrade" to "true"),
        ),
        EventModel(
            name = "submitted",
            event = LendingAnalyticsEvent.TransactionSubmitted(deposit),
            expectedId = "[ETH Lending] Transaction Submitted",
            expectedParams = mapOf("Transaction Type" to "Deposit", "With Upgrade" to "true"),
        ),
        EventModel(
            name = "failed",
            event = LendingAnalyticsEvent.TransactionFailed(deposit, errorCode = "NetworkError: 500"),
            expectedId = "[ETH Lending] Error - Transaction Failed",
            expectedParams = mapOf(
                "Transaction Type" to "Deposit",
                "With Upgrade" to "true",
                "Error Code" to "NetworkError: 500",
            ),
        ),
    )
}