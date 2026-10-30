package com.tangem.features.moneyaccount.details

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
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
internal class MoneyAccountBankDetailsAnalyticsTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val analytics = MoneyAccountBankDetailsAnalytics(analytics = analyticsEventHandler)

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    @Test
    fun `GIVEN details screen WHEN shown THEN deposit details screen opened event sent`() {
        // Act
        analytics.onDetailsShown()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(ofType<MoneyAccountAnalyticsEvents.DepositDetailsScreenOpened>())
        }
    }

    @Test
    fun `GIVEN details screen WHEN share clicked THEN details shared event sent`() {
        // Act
        analytics.onShareClicked()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(ofType<MoneyAccountAnalyticsEvents.DetailsShared>()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN requisite WHEN copied THEN field copied event sent with the Amplitude field name`(model: FieldModel) {
        // Act
        analytics.onFieldCopied(model.field)

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(MoneyAccountAnalyticsEvents.DetailsFieldCopied(fieldName = model.fieldName))
        }
    }

    private fun provideTestModels() = listOf(
        FieldModel(field = RequisiteField.ACCOUNT_NUMBER, fieldName = "account_number"),
        FieldModel(field = RequisiteField.ROUTING_NUMBER, fieldName = "routing_number"),
        FieldModel(field = RequisiteField.BENEFICIARY_NAME, fieldName = "beneficiary"),
        FieldModel(field = RequisiteField.BENEFICIARY_ADDRESS, fieldName = "beneficiary"),
        FieldModel(field = RequisiteField.BANK_NAME, fieldName = "bank"),
        FieldModel(field = RequisiteField.BANK_ADDRESS, fieldName = "bank"),
    )

    data class FieldModel(val field: RequisiteField, val fieldName: String)
}