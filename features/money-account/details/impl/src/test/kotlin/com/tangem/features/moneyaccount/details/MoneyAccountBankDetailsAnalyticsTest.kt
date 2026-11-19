package com.tangem.features.moneyaccount.details

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.tangempay.model.BankCredentials
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.tangempay.MockBankCredentials
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class MoneyAccountBankDetailsAnalyticsTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk()
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()
    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val analytics = MoneyAccountBankDetailsAnalytics(
        analytics = analyticsEventHandler,
        moneyAccountFeatureToggles = moneyAccountFeatureToggles,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler, moneyAccountFeatureToggles)
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
        every { moneyAccountFeatureToggles.isMxnEnabled } returns true
    }

    @Test
    fun `GIVEN MXN toggle off WHEN details shown, copied and shared THEN events carry neither currency nor rail`() {
        // Arrange
        every { moneyAccountFeatureToggles.isMxnEnabled } returns false

        // Act
        analytics.onDetailsShown(ACH)
        analytics.onFieldCopied(field = RequisiteField.ROUTING_NUMBER, credentials = ACH)
        analytics.onShareClicked(ACH)

        // Assert
        assertThat(sentEvents.map { it.toSent() }).containsExactly(
            SentEvent(id = "[Cash Account] Account Details Screen Opened", params = mapOf("route_state" to "active")),
            SentEvent(id = "[Cash Account] Details Field Copied", params = mapOf("field" to "routing_number")),
            SentEvent(id = "[Cash Account] Details Shared", params = emptyMap()),
        ).inOrder()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DetailsShown {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN route on screen WHEN details shown THEN screen opened event carries its currency and rail`(
            model: RouteModel,
        ) {
            // Act
            analytics.onDetailsShown(model.credentials)

            // Assert
            assertThat(sentEvents.map { it.toSent() }).containsExactly(
                SentEvent(
                    id = "[Cash Account] Account Details Screen Opened",
                    params = mapOf(
                        "route_state" to "active",
                        "currency" to model.expectedCurrency,
                        "rail" to model.expectedRail,
                    ),
                ),
            )
        }

        private fun provideTestModels() = listOf(
            RouteModel(credentials = ACH, expectedCurrency = "usd", expectedRail = "ach_wire"),
            RouteModel(credentials = WIRE, expectedCurrency = "usd", expectedRail = "ach_wire"),
            RouteModel(credentials = SPEI, expectedCurrency = "mxn", expectedRail = "spei"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Shared {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN route on screen WHEN share clicked THEN details shared event carries its currency`(
            model: RouteModel,
        ) {
            // Act
            analytics.onShareClicked(model.credentials)

            // Assert
            assertThat(sentEvents.map { it.toSent() }).containsExactly(
                SentEvent(id = "[Cash Account] Details Shared", params = mapOf("currency" to model.expectedCurrency)),
            )
        }

        private fun provideTestModels() = listOf(
            RouteModel(credentials = ACH, expectedCurrency = "usd", expectedRail = "ach_wire"),
            RouteModel(credentials = SPEI, expectedCurrency = "mxn", expectedRail = "spei"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FieldCopied {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN requisite WHEN copied THEN field copied event carries the field name and the route currency`(
            model: FieldModel,
        ) {
            // Act
            analytics.onFieldCopied(field = model.field, credentials = model.credentials)

            // Assert
            assertThat(sentEvents.map { it.toSent() }).containsExactly(
                SentEvent(
                    id = "[Cash Account] Details Field Copied",
                    params = mapOf("field" to model.expectedField, "currency" to model.expectedCurrency),
                ),
            )
        }

        private fun provideTestModels() = listOf(
            FieldModel(RequisiteField.ACCOUNT_NUMBER, ACH, expectedField = "account_number", expectedCurrency = "usd"),
            FieldModel(RequisiteField.ROUTING_NUMBER, ACH, expectedField = "routing_number", expectedCurrency = "usd"),
            FieldModel(RequisiteField.CLABE, SPEI, expectedField = "clabe", expectedCurrency = "mxn"),
            FieldModel(RequisiteField.BENEFICIARY_NAME, WIRE, expectedField = "beneficiary", expectedCurrency = "usd"),
            FieldModel(
                field = RequisiteField.BENEFICIARY_ADDRESS,
                credentials = ACH,
                expectedField = "beneficiary_address",
                expectedCurrency = "usd",
            ),
            FieldModel(RequisiteField.BANK_NAME, ACH, expectedField = "bank_name", expectedCurrency = "usd"),
            FieldModel(RequisiteField.BANK_ADDRESS, ACH, expectedField = "bank_address", expectedCurrency = "usd"),
        )
    }

    private fun AnalyticsEvent.toSent() = SentEvent(id = id, params = params)

    data class RouteModel(val credentials: BankCredentials, val expectedCurrency: String, val expectedRail: String)

    data class FieldModel(
        val field: RequisiteField,
        val credentials: BankCredentials,
        val expectedField: String,
        val expectedCurrency: String,
    )

    data class SentEvent(val id: String, val params: Map<String, String>)

    private companion object {
        val ACH = MockBankCredentials.bankCredentials(rail = "ACH")
        val WIRE = MockBankCredentials.bankCredentials(rail = "WIRE")
        val SPEI = MockBankCredentials.speiBankCredentials()
    }
}