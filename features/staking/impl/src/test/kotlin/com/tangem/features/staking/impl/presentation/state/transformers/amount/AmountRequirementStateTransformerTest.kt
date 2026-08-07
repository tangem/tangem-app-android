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

    private fun solanaCryptoStatus(): CryptoCurrencyStatus = mockk(relaxed = true) {
        every { currency.network.rawId } returns "solana"
    }

    private fun solanaExitIntegration(exitMin: BigDecimal?, enterMin: BigDecimal? = null): StakingIntegration =
        mockk {
            every { exitMinimumAmount } returns exitMin
            every { enterMinimumAmount } returns enterMin
            every { exitArgs } returns null
        }

    private fun solanaTransformer(
        staked: BigDecimal,
        exitMin: BigDecimal?,
        enterMin: BigDecimal? = null,
    ) = AmountRequirementStateTransformer(
        cryptoCurrencyStatus = solanaCryptoStatus(),
        maxAmount = EnterAmountBoundary(amount = staked, fiatAmount = null, fiatRate = null),
        integration = solanaExitIntegration(exitMin = exitMin, enterMin = enterMin),
        actionType = StakingActionCommonType.Exit(partiallyUnstakeDisabled = false),
    )

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

    @Test
    fun `WHEN Solana full unstake THEN no error`() {
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        val result = transformer.transform(amountState(BigDecimal("5"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isFalse()
        assertThat(result.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `WHEN Solana full unstake of small stake below minimum THEN no error`() {
        val small = BigDecimal("0.098090754")
        val transformer = solanaTransformer(staked = small, exitMin = BigDecimal("1"))

        val result = transformer.transform(amountState(small)) as AmountState.Data

        assertThat(result.amountTextField.isError).isFalse()
        assertThat(result.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `WHEN Solana partial unstake below minimum THEN unstake min error and button disabled`() {
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        val result = transformer.transform(amountState(BigDecimal("0.5"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat(result.isPrimaryButtonEnabled).isFalse()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_unstake_amount_requirement_error)
    }

    @Test
    fun `WHEN Solana partial unstake leaving remainder below minimum THEN low staked balance error and button disabled`() {
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        val result = transformer.transform(amountState(BigDecimal("4.5"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat(result.isPrimaryButtonEnabled).isFalse()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_notification_low_staked_balance_text)
    }

    @Test
    fun `WHEN Solana partial unstake violating both minimums THEN unstake min error takes priority`() {
        val transformer = solanaTransformer(staked = BigDecimal("1.5"), exitMin = BigDecimal("1"))

        val result = transformer.transform(amountState(BigDecimal("0.7"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_unstake_amount_requirement_error)
    }

    @Test
    fun `WHEN Solana partial unstake with both parts above minimum THEN no error`() {
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        val result = transformer.transform(amountState(BigDecimal("1.5"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isFalse()
        assertThat(result.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `WHEN Solana amount exactly at minimum THEN no error`() {
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        val result = transformer.transform(amountState(BigDecimal("1"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isFalse()
        assertThat(result.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `WHEN Solana remainder exactly at minimum THEN no error`() {
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = BigDecimal("1"))

        val result = transformer.transform(amountState(BigDecimal("4"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isFalse()
    }

    @Test
    fun `WHEN Solana exit minimum is zero THEN falls back to enter minimum`() {
        val transformer = solanaTransformer(
            staked = BigDecimal("5"),
            exitMin = BigDecimal.ZERO,
            enterMin = BigDecimal("1"),
        )

        val result = transformer.transform(amountState(BigDecimal("0.5"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isTrue()
        assertThat((result.amountTextField.error as TextReference.Res).id)
            .isEqualTo(R.string.staking_unstake_amount_requirement_error)
    }

    @Test
    fun `WHEN Solana both minimums null THEN partial unstake allowed`() {
        val transformer = solanaTransformer(staked = BigDecimal("5"), exitMin = null, enterMin = null)

        val result = transformer.transform(amountState(BigDecimal("0.5"))) as AmountState.Data

        assertThat(result.amountTextField.isError).isFalse()
        assertThat(result.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `WHEN currency is not Solana THEN Solana rule does not apply`() {
        val transformer = AmountRequirementStateTransformer(
            cryptoCurrencyStatus = cryptoCurrencyStatus, // relaxed mock: network.rawId is not "solana"
            maxAmount = EnterAmountBoundary(amount = BigDecimal("5"), fiatAmount = null, fiatRate = null),
            integration = exitIntegrationWith(minimum = null, maximum = null),
            actionType = StakingActionCommonType.Exit(partiallyUnstakeDisabled = false),
        )

        val result = transformer.transform(amountState(BigDecimal("0.5"))) as AmountState.Data

        // Non-Solana: falls through to legacy exitArgs path (minimum null → no error), NOT the Solana remainder rule.
        assertThat(result.amountTextField.isError).isFalse()
    }
}