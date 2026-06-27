package com.tangem.features.send.feeselector.model

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.utils.FeeCalculationUtils
import com.tangem.features.send.commonFee
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeeSelectorAlertFactoryTest {

    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val factory = FeeSelectorAlertFactory(messageSender)

    @BeforeEach
    fun resetSender() {
        clearMocks(messageSender)
    }

    private fun ethFee(value: String): Fee =
        Fee.Common(Amount(currencySymbol = "ETH", value = BigDecimal(value), decimals = 18))

    private fun content(selected: FeeItem) = FeeSelectorUM.Content(
        isPrimaryButtonEnabled = true,
        fees = TransactionFee.Single(normal = commonFee()),
        feeItems = persistentListOf(selected),
        selectedFeeItem = selected,
        feeExtraInfo = mockk(),
        feeFiatRateUM = null,
        feeNonce = FeeNonce.None,
    )

    private fun choosable(normal: Fee, minimum: Fee, priority: Fee) =
        TransactionFee.Choosable(normal = normal, minimum = minimum, priority = priority)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetFeeUpdatedAlert {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN reloaded fee WHEN getFeeUpdatedAlert THEN resolves to warn proceed or nothing`(model: UpdatedModel) {
            // Arrange
            val proceed: () -> Unit = mockk(relaxed = true)

            // Act
            factory.getFeeUpdatedAlert(
                model.newFee,
                model.state,
                proceedAction = proceed,
                stopAction = mockk(relaxed = true),
            )

            // Assert
            verify(exactly = if (model.outcome == Outcome.DIALOG) 1 else 0) { messageSender.send(any()) }
            verify(exactly = if (model.outcome == Outcome.PROCEED) 1 else 0) { proceed() }
        }

        private fun provideTestModels() = listOf(
            // Market -> normal, higher -> warn
            UpdatedModel(
                content(FeeItem.Market(ethFee("1"))),
                choosable(ethFee("2"), ethFee("0"), ethFee("0")),
                Outcome.DIALOG
            ),
            // Market -> normal, not higher -> proceed
            UpdatedModel(
                content(FeeItem.Market(ethFee("2"))),
                choosable(ethFee("1"), ethFee("0"), ethFee("0")),
                Outcome.PROCEED
            ),
            // Slow -> minimum
            UpdatedModel(
                content(FeeItem.Slow(ethFee("1"))),
                choosable(ethFee("0"), ethFee("2"), ethFee("0")),
                Outcome.DIALOG
            ),
            // Fast -> priority
            UpdatedModel(
                content(FeeItem.Fast(ethFee("1"))),
                choosable(ethFee("0"), ethFee("0"), ethFee("2")),
                Outcome.DIALOG
            ),
            // Single -> normal
            UpdatedModel(
                content(FeeItem.Market(ethFee("1"))),
                TransactionFee.Single(ethFee("2")),
                Outcome.DIALOG
            ),
            // Suggested -> its own fee == old fee, never higher -> proceed
            UpdatedModel(
                content(FeeItem.Suggested(title = mockk(), fee = ethFee("5"))),
                choosable(ethFee("9"), ethFee("9"), ethFee("9")),
                Outcome.PROCEED,
            ),
            // Custom selected -> early return, nothing happens
            UpdatedModel(
                content(FeeItem.Custom(fee = ethFee("1"), customValues = persistentListOf())),
                choosable(ethFee("9"), ethFee("9"), ethFee("9")),
                Outcome.NOTHING,
            ),
            // non-content state -> early return, nothing happens
            UpdatedModel(
                FeeSelectorUM.Loading,
                TransactionFee.Single(ethFee("2")),
                Outcome.NOTHING
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CheckAndShowAlerts {

        @BeforeEach
        fun mockUtils() {
            mockkObject(FeeCalculationUtils)
        }

        @AfterEach
        fun unmockUtils() {
            unmockkObject(FeeCalculationUtils)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN fee validity WHEN checkAndShowAlerts THEN confirms only when no alert shown`(model: AlertsModel) {
            // Arrange
            every { FeeCalculationUtils.checkIfCustomFeeTooLow(any()) } returns model.tooLow
            every { FeeCalculationUtils.checkIfCustomFeeTooHigh(any()) } returns (model.tooHigh to "5")
            val onConfirm: () -> Unit = mockk(relaxed = true)

            // Act
            factory.checkAndShowAlerts(content(FeeItem.Market(ethFee("1"))), onConfirm)

            // Assert
            verify(exactly = model.expectedSends) { messageSender.send(any()) }
            verify(exactly = if (model.expectConfirm) 1 else 0) { onConfirm() }
        }

        private fun provideTestModels() = listOf(
            AlertsModel(tooLow = false, tooHigh = false, expectedSends = 0, expectConfirm = true),
            AlertsModel(tooLow = true, tooHigh = false, expectedSends = 1, expectConfirm = false),
            AlertsModel(tooLow = false, tooHigh = true, expectedSends = 1, expectConfirm = false),
        )
    }

    enum class Outcome { DIALOG, PROCEED, NOTHING }

    data class UpdatedModel(val state: FeeSelectorUM, val newFee: TransactionFee, val outcome: Outcome)
    data class AlertsModel(
        val tooLow: Boolean,
        val tooHigh: Boolean,
        val expectedSends: Int,
        val expectConfirm: Boolean,
    )
}