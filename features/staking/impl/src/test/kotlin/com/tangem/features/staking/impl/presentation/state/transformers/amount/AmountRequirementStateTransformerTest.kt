package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.common.StakingActionArgs
import com.tangem.domain.staking.model.common.StakingAmountRequirement
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.tokens.model.Amount
import com.tangem.features.staking.impl.R
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class AmountRequirementStateTransformerTest {

    private val cryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true)

    private fun amountState(enteredCrypto: BigDecimal): AmountState.Data = AmountState.Data(
        isPrimaryButtonEnabled = true,
        accountTitleUM = mockk(relaxed = true),
        availableBalanceCrypto = mockk(relaxed = true),
        availableBalanceFiat = mockk(relaxed = true),
        tokenName = mockk(relaxed = true),
        tokenIconState = mockk(relaxed = true),
        amountTextField = AmountFieldModel(
            value = enteredCrypto.toPlainString(),
            onValueChange = {},
            keyboardOptions = mockk(relaxed = true),
            keyboardActions = mockk(relaxed = true),
            cryptoAmount = Amount(currencySymbol = "ETH", value = enteredCrypto, decimals = 18),
            fiatAmount = Amount(currencySymbol = "USD", value = BigDecimal.ZERO, decimals = 2),
            isFiatValue = false,
            fiatValue = "0",
            isFiatUnavailable = false,
            isValuePasted = false,
            onValuePastedTriggerDismiss = {},
            isError = false,
            isWarning = false,
            error = stringReference(""),
        ),
        appCurrency = mockk(relaxed = true),
    )

    private fun enterIntegrationWith(minimum: BigDecimal?, maximum: BigDecimal?): StakingIntegration = mockk {
        every { enterArgs } returns StakingActionArgs(
            amountRequirement = StakingAmountRequirement(
                isRequired = true,
                minimum = minimum,
                maximum = maximum,
            ),
            isPartialAmountDisabled = false,
        )
    }

    private fun exitIntegrationWith(minimum: BigDecimal?, maximum: BigDecimal?): StakingIntegration = mockk {
        every { exitArgs } returns StakingActionArgs(
            amountRequirement = StakingAmountRequirement(
                isRequired = true,
                minimum = minimum,
                maximum = maximum,
            ),
            isPartialAmountDisabled = false,
        )
    }

    @Test
    fun `WHEN amount exceeds positive maximum THEN max amount error string is used`() {
        val transformer = AmountRequirementStateTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxAmount = EnterAmountBoundary(amount = BigDecimal("100"), fiatAmount = null, fiatRate = null),
            integration = enterIntegrationWith(minimum = BigDecimal("0.01"), maximum = BigDecimal("0.15")),
            actionType = StakingActionCommonType.Enter(skipEnterAmount = false),
        )

        val result = transformer.transform(amountState(BigDecimal("0.2"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_max_amount_requirement_error)
    }

    @Test
    fun `WHEN amount below minimum THEN min amount error string is used`() {
        val transformer = AmountRequirementStateTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxAmount = EnterAmountBoundary(amount = BigDecimal("100"), fiatAmount = null, fiatRate = null),
            integration = enterIntegrationWith(minimum = BigDecimal("0.1"), maximum = null),
            actionType = StakingActionCommonType.Enter(skipEnterAmount = false),
        )

        val result = transformer.transform(amountState(BigDecimal("0.05"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_amount_requirement_error)
    }

    @Test
    fun `WHEN Exit action and amount below exit minimum THEN unstake min error string is used`() {
        val transformer = AmountRequirementStateTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxAmount = EnterAmountBoundary(amount = BigDecimal("100"), fiatAmount = null, fiatRate = null),
            integration = exitIntegrationWith(minimum = BigDecimal("0.1"), maximum = null),
            actionType = StakingActionCommonType.Exit(partiallyUnstakeDisabled = false),
        )

        val result = transformer.transform(amountState(BigDecimal("0.05"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_unstake_amount_requirement_error)
    }

    @Test
    fun `WHEN Enter action and maximum is null and amount exceeds balance cap THEN max error string is used`() {
        val transformer = AmountRequirementStateTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxAmount = EnterAmountBoundary(amount = BigDecimal("0.5"), fiatAmount = null, fiatRate = null),
            integration = enterIntegrationWith(minimum = BigDecimal("0.01"), maximum = null),
            actionType = StakingActionCommonType.Enter(skipEnterAmount = false),
        )

        val result = transformer.transform(amountState(BigDecimal("0.6"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_max_amount_requirement_error)
    }

    @Test
    fun `WHEN Exit action and amount exceeds staked balance THEN max amount error string is used`() {
        val transformer = AmountRequirementStateTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxAmount = EnterAmountBoundary(amount = BigDecimal("0.5"), fiatAmount = null, fiatRate = null),
            integration = exitIntegrationWith(minimum = BigDecimal("0.01"), maximum = null),
            actionType = StakingActionCommonType.Exit(partiallyUnstakeDisabled = false),
        )

        val result = transformer.transform(amountState(BigDecimal("0.6"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_max_amount_requirement_error)
    }
}