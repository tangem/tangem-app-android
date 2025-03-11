package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer
import java.math.RoundingMode

internal class AmountRoundToIntegerTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<StakingUiState> {
    override fun transform(prevState: StakingUiState): StakingUiState {
        val amountState = prevState.amountState as? AmountState.Data ?: return prevState
        val amountTextField = amountState.amountTextField
        if (amountTextField.value.isEmpty()) return prevState

        val cryptoAmount = amountState.amountTextField.cryptoAmount
        val fiatAmount = amountState.amountTextField.fiatAmount
        val fiatDecimals = fiatAmount.decimals

        val roundedDownCrypto = cryptoAmount.value?.setScale(0, RoundingMode.DOWN)
        val roundedDownFiat = roundedDownCrypto?.multiply(cryptoCurrencyStatus.value.fiatRate)

        val value = roundedDownCrypto?.parseBigDecimal(0).orEmpty()
        val fiatValue = roundedDownFiat?.parseBigDecimal(fiatDecimals, RoundingMode.HALF_UP).orEmpty()

        return prevState.copy(
            amountState = amountState.copy(
                amountTextField = amountState.amountTextField.copy(
                    cryptoAmount = cryptoAmount.copy(value = roundedDownCrypto),
                    fiatAmount = fiatAmount.copy(value = roundedDownFiat),
                    value = value,
                    fiatValue = fiatValue,
                    isWarning = false,
                ),
            ),
        )
    }
}