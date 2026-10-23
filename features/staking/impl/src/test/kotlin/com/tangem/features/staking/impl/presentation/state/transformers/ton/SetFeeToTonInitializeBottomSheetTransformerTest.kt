package com.tangem.features.staking.impl.presentation.state.transformers.ton

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.ethFee
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.bottomsheet.TonInitializeAccountBottomSheetConfig
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.Provider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetFeeToTonInitializeBottomSheetTransformerTest {

    private val fee = ethFee(BigDecimal("0.05"), "TON", 9)
    private val content = TonInitializeAccountBottomSheetConfig(
        title = TextReference.EMPTY,
        message = TextReference.EMPTY,
        footer = TextReference.EMPTY,
        onButtonClick = {},
        isButtonEnabled = false,
        isButtonLoading = true,
        feeState = FeeState.Loading,
        isColdWalletInteractionIconVisible = true,
    )
    private val sheet = TangemBottomSheetConfig(isShown = true, onDismissRequest = {}, content = content)

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN activation sheet WHEN fee loaded THEN sheet shows fee and enables button`(model: TonFeeModel) {
        // Arrange
        val state = stakingUiState(bottomSheetConfig = sheet)

        // Act
        val actual = transformer(model.feeStatus).transform(state)

        // Assert
        assertThat(actual).isEqualTo(
            state.copy(
                bottomSheetConfig = sheet.copy(
                    content = content.copy(
                        isButtonEnabled = true,
                        isButtonLoading = false,
                        feeState = FeeState.Content(
                            fee = fee,
                            rate = model.expectedRate,
                            isFeeConvertibleToFiat = model.expectedConvertible,
                            appCurrency = AppCurrency.Default,
                            isFeeApproximate = true,
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN no bottom sheet WHEN fee loaded THEN same state`() {
        // Arrange
        val state = stakingUiState()

        // Act
        val actual = transformer(feeStatus = null).transform(state)

        // Assert
        assertThat(actual).isSameInstanceAs(state)
    }

    private fun transformer(feeStatus: CryptoCurrencyStatus?) = SetFeeToTonInitializeBottomSheetTransformer(
        appCurrencyProvider = Provider { AppCurrency.Default },
        feeCryptoCurrencyStatus = feeStatus,
        fee = fee,
        isFeeApproximate = true,
    )

    private fun provideTestModels() = listOf(
        TonFeeModel(feeStatus = null, expectedRate = null, expectedConvertible = false),
        TonFeeModel(
            feeStatus = currencyStatus(coin(Blockchain.TON), fiatRate = BigDecimal("2")),
            expectedRate = BigDecimal("2"),
            expectedConvertible = true,
        ),
    )

    internal data class TonFeeModel(
        val feeStatus: CryptoCurrencyStatus?,
        val expectedRate: BigDecimal?,
        val expectedConvertible: Boolean,
    )
}