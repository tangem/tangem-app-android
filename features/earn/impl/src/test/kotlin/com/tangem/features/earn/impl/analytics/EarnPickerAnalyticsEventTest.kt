package com.tangem.features.earn.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.impl.getfunds.EarnGetTokenAction
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnRowKind
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnPickerAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN picker event WHEN built THEN id and params match the spec`(model: EventModel) {
        // Act
        val event = model.event

        // Assert
        assertThat(event.id).isEqualTo(model.expectedId)
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    private fun provideTestModels() = listOf(
        EventModel(
            event = EarnPickerAnalyticsEvent.ScreenOpened(
                source = EarnEntrySource.TokenPage,
                token = "ETH",
                product = "Lending",
                currentApy = "5.50",
                products = 2,
            ),
            expectedId = "[Earn] Yield Picker Screen Opened",
            expectedParams = mapOf(
                "Source" to "Token Page",
                "Token" to "ETH",
                "Product" to "Lending",
                "Current APY" to "5.50",
                "Products" to "2",
            ),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.ScreenOpened(
                source = EarnEntrySource.TokenPage,
                token = "ETH",
                product = "Lending",
                currentApy = null,
                products = 2,
            ),
            expectedId = "[Earn] Yield Picker Screen Opened",
            expectedParams = mapOf(
                "Source" to "Token Page",
                "Token" to "ETH",
                "Product" to "Lending",
                "Products" to "2",
            ),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.ProductChip(product = "Vault", position = 2, currentApy = "7.10"),
            expectedId = "[Earn] Button - Product Chip",
            expectedParams = mapOf("Product" to "Vault", "Position" to "2", "Current APY" to "7.10"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.ProductChip(product = "Vault", position = 2, currentApy = null),
            expectedId = "[Earn] Button - Product Chip",
            expectedParams = mapOf("Product" to "Vault", "Position" to "2"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.Tab(product = "Lending", tab = EarnPickerTab.Benefits),
            expectedId = "[Earn] Button - Yield Picker Tab",
            expectedParams = mapOf("Product" to "Lending", "Tab" to "Benefits"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.CalculatorHorizon(product = "Lending", horizon = EarnHorizon.Year3),
            expectedId = "[Earn] Button - Calculator Horizon",
            expectedParams = mapOf("Product" to "Lending", "Horizon" to "3Y"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.CalculatorInput(
                product = "Lending",
                input = EarnCalculatorComponent.Input.MonthlyTopUp,
            ),
            expectedId = "[Earn] Button - Calculator Input",
            expectedParams = mapOf("Product" to "Lending", "Input Type" to "Monthly Top-Up"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.DetailRow(product = "Lending", row = EarnRowKind.PerformanceFee),
            expectedId = "[Earn] Button - Detail Row",
            expectedParams = mapOf("Product" to "Lending", "Row" to "Performance Fee"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.DetailRow(product = "Lending", row = null),
            expectedId = "[Earn] Button - Detail Row",
            expectedParams = mapOf("Product" to "Lending"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.StartEarning(product = "Lending", currentApy = "5.50"),
            expectedId = "[Earn] Button - Start Earning",
            expectedParams = mapOf("Product" to "Lending", "Current APY" to "5.50"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.StartEarning(product = "Lending", currentApy = null),
            expectedId = "[Earn] Button - Start Earning",
            expectedParams = mapOf("Product" to "Lending"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.NoFundsNotice(token = "ETH", product = "Vault"),
            expectedId = "[Earn] Notice - No Funds",
            expectedParams = mapOf("Token" to "ETH", "Product" to "Vault"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.AddFunds(token = "ETH", source = EarnAddFundsSource.YieldPicker),
            expectedId = "[Earn] Button - Add Funds",
            expectedParams = mapOf("Token" to "ETH", "Source" to "Yield Picker"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.AddFunds(token = "ETH", source = EarnAddFundsSource.InfoScreen),
            expectedId = "[Earn] Button - Add Funds",
            expectedParams = mapOf("Token" to "ETH", "Source" to "Info Screen"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.GetAssetSheetOpened(token = "ETH", source = EarnAddFundsSource.YieldPicker),
            expectedId = "[Earn] Get Asset Sheet Opened",
            expectedParams = mapOf("Token" to "ETH", "Source" to "Yield Picker"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.GetAssetMethod(token = "ETH", method = EarnGetTokenAction.Deposit),
            expectedId = "[Earn] Button - Get Asset Method",
            expectedParams = mapOf("Token" to "ETH", "Method" to "Deposit"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.GetAssetMethod(token = "ETH", method = EarnGetTokenAction.Swap),
            expectedId = "[Earn] Button - Get Asset Method",
            expectedParams = mapOf("Token" to "ETH", "Method" to "Swap"),
        ),
        EventModel(
            event = EarnPickerAnalyticsEvent.GetAssetMethod(token = "ETH", method = EarnGetTokenAction.Buy),
            expectedId = "[Earn] Button - Get Asset Method",
            expectedParams = mapOf("Token" to "ETH", "Method" to "Buy"),
        ),
    )

    internal data class EventModel(
        val event: AnalyticsEvent,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    )
}