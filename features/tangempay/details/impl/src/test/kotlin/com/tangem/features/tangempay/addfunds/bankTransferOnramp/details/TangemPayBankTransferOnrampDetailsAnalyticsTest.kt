package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankTransferOnrampDetailsAnalyticsTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val analytics = TangemPayBankTransferOnrampDetailsAnalytics(analytics = analyticsEventHandler)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    @Test
    fun `GIVEN details screen WHEN shown THEN banking details showed event sent`() {
        // Act
        analytics.onDetailsShown()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(ofType<TangemPayAnalyticsEvents.VaBankingDetailsShowed>()) }
    }

    @Test
    fun `GIVEN details screen WHEN share clicked THEN share details event sent`() {
        // Act
        analytics.onShareClicked()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(ofType<TangemPayAnalyticsEvents.VaShareDetailsButtonClicked>())
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN requisite WHEN copied THEN copy field event sent with its name`(model: FieldModel) {
        // Act
        analytics.onFieldCopied(model.field)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(TangemPayAnalyticsEvents.VaCopyFieldClicked(model.fieldName))
        }
    }

    private fun provideTestModels() = listOf(
        FieldModel(field = RequisiteField.ACCOUNT_NUMBER, fieldName = "Account number"),
        FieldModel(field = RequisiteField.ROUTING_NUMBER, fieldName = "Routing number"),
        FieldModel(field = RequisiteField.BENEFICIARY_NAME, fieldName = "Beneficiary name"),
        FieldModel(field = RequisiteField.BENEFICIARY_ADDRESS, fieldName = "Beneficiary address"),
        FieldModel(field = RequisiteField.BANK_NAME, fieldName = "Bank name"),
        FieldModel(field = RequisiteField.BANK_ADDRESS, fieldName = "Bank address"),
    )

    data class FieldModel(val field: RequisiteField, val fieldName: String)
}