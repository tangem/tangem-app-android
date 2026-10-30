package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.balanceState
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

internal class AmountMaxValueStateTransformerTest {

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
    private val exit = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)
    private val staked = balanceState(cryptoAmount = BigDecimal("5"), fiatAmount = BigDecimal("10"))
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
    fun `GIVEN exit WHEN max THEN staked balance used`() {
        // Act
        val actual = max(param = exit, stateAction = exit)

        // Assert
        assertThat(actual.amountTextField.cryptoAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("5"))
        assertThat(actual.amountTextField.fiatAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("10"))
        assertThat(actual.amountTextField.isValuePasted).isTrue()
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN enter WHEN max THEN wallet balance used`() {
        // Act
        val actual = max(param = enter, stateAction = enter)

        // Assert
        assertThat(actual.amountTextField.cryptoAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("10"))
        assertThat(actual.amountTextField.fiatAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("20"))
    }

    @Test
    fun `GIVEN exit without balance state WHEN max THEN amount unchanged`() {
        // Act
        val actual = max(param = exit, stateAction = exit, balanceState = null, amount = BigDecimal("2"))

        // Assert
        assertThat(actual.amountTextField.cryptoAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("2"))
        assertThat(actual.amountTextField.isValuePasted).isFalse()
    }

    @Test
    fun `GIVEN max below minimum transaction amount WHEN max THEN minimum error and disabled`() {
        // Act
        val actual = max(param = enter, stateAction = enter, minimum = EnterAmountBoundary(amount = BigDecimal("50")))

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN exit param but enter state WHEN max THEN staked balance checked against enter requirement`() {
        // Arrange
        val integration = stakeKitIntegration(
            yield = MockStaking.yield(
                enter = MockStaking.enterArgs(minimum = BigDecimal("6")),
                exit = MockStaking.enterArgs(minimum = BigDecimal.ONE),
            ),
        )

        // Act
        val actual = max(param = exit, stateAction = enter, integration = integration)

        // Assert
        assertThat(actual.amountTextField.cryptoAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("5"))
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error).isEqualTo(
            resourceReference(
                R.string.staking_amount_requirement_error,
                wrappedList(BigDecimal("6").format { crypto(currencyStatus().currency) }),
            ),
        )
    }

    @Suppress("LongParameterList")
    private fun max(
        param: StakingActionCommonType,
        stateAction: StakingActionCommonType,
        balanceState: BalanceState? = staked,
        amount: BigDecimal = BigDecimal.ONE,
        minimum: EnterAmountBoundary? = null,
        integration: StakingIntegration = stakeKitIntegration(),
    ): AmountState.Data {
        val state = stakingUiState(
            actionType = stateAction,
            amountState = amountData(amount, cryptoDecimals = 6),
            balanceState = balanceState,
        )
        return AmountMaxValueStateTransformer(
            cryptoCurrencyStatus = currencyStatus(),
            minimumTransactionAmount = minimum,
            actionType = param,
            integration = integration,
        ).transform(state).amountState as AmountState.Data
    }
}