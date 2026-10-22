package com.tangem.features.staking.impl.presentation.state.transformers.ton

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.bottomsheet.TonInitializeAccountBottomSheetConfig
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.feeContent
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class CompleteInitializeBottomSheetTransformerTest {

    private val transformer = CompleteInitializeBottomSheetTransformer(
        cryptoCurrencyStatus = currencyStatus(coin(Blockchain.TON), amount = BigDecimal("10")),
        minimumTransactionAmount = null,
    )
    private lateinit var defaultLocale: Locale

    @BeforeEach
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN activation fee WHEN complete THEN amount reduced by fee times one point one`() {
        // Act
        val actual = observedReduce(transformer.transform(givenState()))

        // Assert
        assertThat(actual.confirmationReduceBy).isEqualTo(BigDecimal("0.055000000"))
        assertThat(actual.amountReduceBy).isEqualTo(BigDecimal("0.055000000"))
        assertThat(actual.cryptoAmount).isEquivalentAccordingToCompareTo(BigDecimal("0.945"))
        assertThat(actual.isButtonLoading).isTrue()
        assertThat(actual.isButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN fee with many decimals WHEN complete THEN result scaled to fee decimals`() {
        // Act
        val actual = observedReduce(
            transformer.transform(givenState(fee = feeContent(ethFee(BigDecimal("0.123456789"), "TON", 9)))),
        )

        // Assert
        assertThat(actual.confirmationReduceBy).isEqualTo(BigDecimal("0.135802468"))
    }

    @Test
    fun `GIVEN no sheet WHEN complete THEN same state`() {
        // Arrange
        val input = givenState(sheet = false)

        // Act & Assert
        assertThat(transformer.transform(input)).isSameInstanceAs(input)
    }

    @Test
    fun `GIVEN loading fee WHEN complete THEN same state`() {
        // Arrange
        val input = givenState(fee = FeeState.Loading)

        // Act & Assert
        assertThat(transformer.transform(input)).isSameInstanceAs(input)
    }

    @Test
    fun `GIVEN fee without value WHEN complete THEN same state`() {
        // Arrange
        val input = givenState(fee = feeContent(ethFee(value = null, symbol = "TON")))

        // Act & Assert
        assertThat(transformer.transform(input)).isSameInstanceAs(input)
    }

    @Test
    fun `GIVEN empty confirmation WHEN complete THEN same state`() {
        // Arrange
        val input = givenState(confirmation = StakingStates.ConfirmationState.Empty())

        // Act & Assert
        assertThat(transformer.transform(input)).isSameInstanceAs(input)
    }

    private fun givenState(
        fee: FeeState = feeContent(ethFee(BigDecimal("0.05"), "TON", 9)),
        amount: BigDecimal = BigDecimal.ONE,
        sheet: Boolean = true,
        confirmation: StakingStates.ConfirmationState = confirmationData(),
    ) = stakingUiState(
        amountState = amountData(amount, cryptoDecimals = 9),
        confirmationState = confirmation,
        bottomSheetConfig = if (sheet) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = TonInitializeAccountBottomSheetConfig(
                    title = TextReference.EMPTY,
                    message = TextReference.EMPTY,
                    footer = TextReference.EMPTY,
                    onButtonClick = {},
                    isButtonEnabled = true,
                    isButtonLoading = false,
                    feeState = fee,
                    isColdWalletInteractionIconVisible = true,
                ),
            )
        } else {
            null
        },
    )

    private fun observedReduce(result: StakingUiState): ObservedReduce {
        val amount = result.amountState as AmountState.Data
        val sheet = requireNotNull(result.bottomSheetConfig).content as TonInitializeAccountBottomSheetConfig
        return ObservedReduce(
            confirmationReduceBy = (result.confirmationState as StakingStates.ConfirmationState.Data).reduceAmountBy,
            amountReduceBy = amount.reduceAmountBy,
            cryptoAmount = amount.amountTextField.cryptoAmount.value,
            isButtonLoading = sheet.isButtonLoading,
            isButtonEnabled = sheet.isButtonEnabled,
        )
    }

    private data class ObservedReduce(
        val confirmationReduceBy: BigDecimal?,
        val amountReduceBy: BigDecimal,
        val cryptoAmount: BigDecimal?,
        val isButtonLoading: Boolean,
        val isButtonEnabled: Boolean,
    )
}