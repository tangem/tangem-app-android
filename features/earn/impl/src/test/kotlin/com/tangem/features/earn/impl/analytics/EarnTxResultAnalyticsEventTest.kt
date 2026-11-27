package com.tangem.features.earn.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnTxResultAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN result event WHEN built THEN id and params match the spec`(model: EventModel) {
        // Act
        val event = model.event

        // Assert
        assertThat(event.id).isEqualTo(model.expectedId)
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    private fun provideTestModels() = listOf(
        EventModel(
            name = "confirmed deposit",
            event = EarnTxResultAnalyticsEvent.TransactionConfirmed(
                category = "ETH Lending",
                params = mapOf("Transaction Type" to "Deposit", "With Upgrade" to "false", "Amount Bucket" to "250-1K"),
            ),
            expectedId = "[ETH Lending] Transaction Confirmed",
            expectedParams = mapOf(
                "Transaction Type" to "Deposit",
                "With Upgrade" to "false",
                "Amount Bucket" to "250-1K",
            ),
        ),
        EventModel(
            name = "failed withdraw",
            event = EarnTxResultAnalyticsEvent.TransactionFailed(
                category = "ETH Lending",
                params = mapOf("Transaction Type" to "Withdraw", "With Upgrade" to "true"),
            ),
            expectedId = "[ETH Lending] Error - Transaction Failed",
            expectedParams = mapOf(
                "Transaction Type" to "Withdraw",
                "With Upgrade" to "true",
                "Error Code" to "Reverted",
            ),
        ),
        EventModel(
            name = "failed without params",
            event = EarnTxResultAnalyticsEvent.TransactionFailed(category = "ETH Staking", params = emptyMap()),
            expectedId = "[ETH Staking] Error - Transaction Failed",
            expectedParams = mapOf("Error Code" to "Reverted"),
        ),
    )

    internal data class EventModel(
        val name: String,
        val event: AnalyticsEvent,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    ) {
        override fun toString(): String = name
    }
}