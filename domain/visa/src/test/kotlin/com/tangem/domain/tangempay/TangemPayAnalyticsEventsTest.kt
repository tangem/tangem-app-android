package com.tangem.domain.tangempay

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayAnalyticsEventsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN visa event WHEN created THEN id and params match the spec`(model: EventModel) {
        // Act
        val event = model.event

        // Assert
        assertThat(event.id).isEqualTo(model.expectedId)
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    internal data class EventModel(
        val event: TangemPayAnalyticsEvents,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    )

    private fun provideTestModels() = listOf(
        EventModel(
            event = TangemPayAnalyticsEvents.TermsAndLimitsClicked(product = TangemPayAnalyticsEvents.Product.CARD),
            expectedId = "[Visa Screen] Button - Terms And Limits",
            expectedParams = mapOf("product" to "card"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.TermsAndLimitsClicked(product = TangemPayAnalyticsEvents.Product.ACCOUNT),
            expectedId = "[Visa Screen] Button - Terms And Limits",
            expectedParams = mapOf("product" to "account"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.TransactionInListClicked(
                product = TangemPayAnalyticsEvents.Product.CARD,
                type = "spend",
                status = "declined",
            ),
            expectedId = "[Visa Screen] Clicked On Transaction In List",
            expectedParams = mapOf("product" to "card", "type" to "spend", "status" to "declined"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.TransactionInListClicked(
                product = TangemPayAnalyticsEvents.Product.ACCOUNT,
                type = "deposit",
                status = "completed",
            ),
            expectedId = "[Visa Screen] Clicked On Transaction In List",
            expectedParams = mapOf("product" to "account", "type" to "deposit", "status" to "completed"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.CurrentPlanClicked(),
            expectedId = "[Visa Tiers] Current Plan Clicked",
            expectedParams = mapOf("source" to "payment_account"),
        ),
        EventModel(
            event = TangemPayAnalyticsEvents.Tiers.ChangePlanClicked(),
            expectedId = "[Visa Tiers] Change Plan Clicked",
            expectedParams = mapOf("source" to "payment_account"),
        ),
    )
}