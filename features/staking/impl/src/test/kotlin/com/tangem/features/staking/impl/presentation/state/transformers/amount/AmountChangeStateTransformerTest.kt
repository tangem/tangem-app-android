package com.tangem.features.staking.impl.presentation.state.transformers.amount

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.balanceState
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale
import com.tangem.common.ui.R as CommonUiR

internal class AmountChangeStateTransformerTest {

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
    private val exit = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)
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
    fun `GIVEN enter within balance WHEN change THEN valid amount`() {
        // Act
        val actual = change(value = "6", actionType = enter)

        // Assert
        assertThat(actual.amountTextField.cryptoAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("6"))
        assertThat(actual.amountTextField.isError).isFalse()
        assertThat(actual.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN exit above staked balance WHEN change THEN insufficient balance error kept`() {
        // Act
        val actual = change(value = "6", actionType = exit)

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error)
            .isEqualTo(resourceReference(CommonUiR.string.common_insufficient_balance))
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN exit within staked balance WHEN change THEN no error`() {
        // Act
        val actual = change(value = "4", actionType = exit)

        // Assert
        assertThat(actual.amountTextField.isError).isFalse()
    }

    @Test
    fun `GIVEN tron enter fractional WHEN change THEN integer warning`() {
        // Act
        val actual = change(
            value = "1.5",
            actionType = enter,
            status = currencyStatus(coin(Blockchain.Tron)),
            integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Tron),
        )

        // Assert
        assertThat(actual.amountTextField.isWarning).isTrue()
        assertThat(actual.amountTextField.error)
            .isEqualTo(resourceReference(R.string.staking_amount_tron_integer_error, wrappedList("1")))
    }

    @Test
    fun `GIVEN below minimum transaction amount WHEN change THEN minimum error`() {
        // Act
        val actual = change(
            value = "0.5",
            actionType = enter,
            minimum = EnterAmountBoundary(amount = BigDecimal.ONE),
        )

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat((actual.amountTextField.error as TextReference.Res).id)
            .isEqualTo(CommonUiR.string.transfer_notification_invalid_minimum_transaction_amount_text)
    }

    @Test
    fun `GIVEN exit without selected balance WHEN change THEN amount parsed without staked maximum`() {
        // Act
        val actual = change(value = "1", actionType = exit, balance = null)

        // Assert
        assertThat(actual.amountTextField.cryptoAmount.value).isEquivalentAccordingToCompareTo(BigDecimal("1"))
    }

    @Test
    fun `GIVEN exit in fiat above staked fiat balance WHEN change THEN insufficient balance error`() {
        // Act
        val actual = change(value = "30", actionType = exit, isFiatValue = true)

        // Assert
        assertThat(actual.amountTextField.isError).isTrue()
        assertThat(actual.amountTextField.error)
            .isEqualTo(resourceReference(CommonUiR.string.common_insufficient_balance))
    }

    @Test
    fun `GIVEN empty input WHEN change THEN cleared and disabled`() {
        // Act
        val actual = change(value = "", actionType = enter)

        // Assert
        assertThat(actual.amountTextField.value).isEmpty()
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
        assertThat(actual.amountTextField.isError).isFalse()
    }

    private fun change(
        value: String,
        actionType: StakingActionCommonType,
        status: CryptoCurrencyStatus = currencyStatus(),
        integration: StakingIntegration = stakeKitIntegration(),
        minimum: EnterAmountBoundary? = null,
        balance: BalanceState? = balanceState(cryptoAmount = BigDecimal("5"), fiatAmount = BigDecimal("10")),
        isFiatValue: Boolean = false,
    ): AmountState.Data {
        val state = stakingUiState(
            actionType = actionType,
            amountState = amountData(BigDecimal.ZERO, value = "", cryptoDecimals = 6, isFiatValue = isFiatValue),
            balanceState = balance,
        )
        return AmountChangeStateTransformer(
            cryptoCurrencyStatus = status,
            minimumTransactionAmount = minimum,
            value = value,
            integration = integration,
        ).transform(state).amountState as AmountState.Data
    }
}