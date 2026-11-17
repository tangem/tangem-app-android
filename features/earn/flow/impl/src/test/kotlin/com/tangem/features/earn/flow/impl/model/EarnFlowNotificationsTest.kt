package com.tangem.features.earn.flow.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnFlowNotificationsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN fee and wallet balance WHEN amount THEN fee exceeds balance card follows them`(model: FeeModel) {
        // Arrange
        val state = state(
            fee = model.fee,
            feeStatus = model.feeStatus,
            isAmountSubtractAvailable = model.isAmountSubtractAvailable,
        )

        // Act
        val actual = EarnFlowNotifications.amount(state, isFromPosition = model.isFromPosition, onFeeReload = {})

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN withdrawal with fee above wallet balance WHEN confirm THEN fee exceeds balance card shown`() {
        // Arrange
        val state = state(fee = "0.0001", feeStatus = testStatus(amount = BigDecimal("0.00005")))

        // Act
        val actual = EarnFlowNotifications.confirm(state, isFromPosition = true, onFeeReload = {})

        // Assert
        assertThat(actual).containsExactly(feeExceedsBalance)
    }

    private fun provideTestModels() = listOf(
        FeeModel(
            isFromPosition = true,
            fee = "0.0001",
            feeStatus = testStatus(amount = BigDecimal("0.00005")),
            expected = listOf(feeExceedsBalance),
        ),
        FeeModel(
            isFromPosition = true,
            fee = "0.0001",
            feeStatus = testStatus(amount = BigDecimal("0.0001")),
            expected = emptyList(),
        ),
        FeeModel(
            isFromPosition = true,
            fee = "0.0001",
            feeStatus = null,
            expected = emptyList(),
        ),
        FeeModel(
            isFromPosition = false,
            fee = "0.0001",
            feeStatus = testStatus(amount = BigDecimal("0.00005")),
            expected = listOf(feeExceedsBalance),
        ),
        FeeModel(
            isFromPosition = false,
            fee = "0.0001",
            feeStatus = testStatus(amount = BigDecimal("0.0001")),
            isAmountSubtractAvailable = true,
            expected = listOf(feeExceedsBalance),
        ),
        FeeModel(
            isFromPosition = false,
            fee = "0.0001",
            feeStatus = testStatus(amount = BigDecimal("0.0001")),
            isAmountSubtractAvailable = false,
            expected = emptyList(),
        ),
        FeeModel(
            isFromPosition = false,
            fee = "0.0001",
            feeStatus = testStatus(amount = BigDecimal("0.0002")),
            isAmountSubtractAvailable = true,
            expected = emptyList(),
        ),
    )

    private fun state(fee: String, feeStatus: CryptoCurrencyStatus?, isAmountSubtractAvailable: Boolean = false) =
        EarnFlowState(
            step = EarnFlowStep.Amount,
            status = testStatus(),
            feeStatus = feeStatus,
            fee = testFeeContent(fee),
            isAmountSubtractAvailable = isAmountSubtractAvailable,
        )

    private val feeExceedsBalance = NotificationUM.Error.ExceedsBalance(
        networkIconId = testEthereum.networkIconResId,
        currencyName = testEthereum.name,
        feeName = testEthereum.name,
        feeSymbol = testEthereum.symbol,
        networkName = testEthereum.name,
    )

    data class FeeModel(
        val isFromPosition: Boolean,
        val fee: String,
        val feeStatus: CryptoCurrencyStatus?,
        val expected: List<NotificationUM>,
        val isAmountSubtractAvailable: Boolean = false,
    )
}