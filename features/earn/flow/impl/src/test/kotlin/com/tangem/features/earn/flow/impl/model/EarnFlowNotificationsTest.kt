package com.tangem.features.earn.flow.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.extensions.networkIconResId
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.yield.MockYieldSupply
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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

    @ParameterizedTest
    @MethodSource("provideSmartGasModels")
    fun `GIVEN Smart Gas fee WHEN withdrawal amount THEN fee token balance decides the card`(model: SmartGasModel) {
        // Arrange
        val state = EarnFlowState(
            step = EarnFlowStep.Amount,
            status = testStatus(),
            feeStatus = testStatus(amount = BigDecimal.ZERO),
            fee = testFeeContent(
                value = "1.5",
                extended = smartGasFee,
                feeCryptoCurrencyStatus = MockYieldSupply.loadedStatus(currency = feeToken, amount = model.tokenBalance),
            ),
        )

        // Act
        val actual = EarnFlowNotifications.amount(state, isFromPosition = true, onFeeReload = {})

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideSmartGasModels() = listOf(
        SmartGasModel(tokenBalance = BigDecimal("2"), expected = emptyList()),
        SmartGasModel(tokenBalance = BigDecimal("1"), expected = listOf(tokenFeeExceedsBalance)),
    )

    private val feeToken = MockYieldSupply.token(symbol = "USDC")

    private val smartGasFee = TransactionFeeExtended(
        transactionFee = TransactionFee.Single(testFee("1.5")),
        feeTokenId = feeToken.id,
        gaslessProtocolData = GaslessProtocolData.Evm,
    )

    private val tokenFeeExceedsBalance = NotificationUM.Error.ExceedsBalance(
        networkIconId = feeToken.networkIconResId,
        currencyName = feeToken.name,
        feeName = feeToken.name,
        feeSymbol = feeToken.symbol,
        networkName = feeToken.name,
    )

    data class SmartGasModel(val tokenBalance: BigDecimal, val expected: List<NotificationUM>)

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