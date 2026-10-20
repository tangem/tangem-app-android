package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.currencyStatus
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.balanceState
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetAmountDataTransformerTest {

    private val clickIntents: StakingClickIntents = mockk(relaxed = true)
    private val wallet = MockUserWalletFactory.create()
    private val status = currencyStatus(amount = BigDecimal("10"), fiatRate = BigDecimal("2"))
    private val currency = status.currency

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)

    @Test
    fun `GIVEN exit WHEN transform THEN staked amount title and staked balance as maximum`() {
        // Act
        val actual = data(
            transform(
                actionType = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false),
                balanceState = balanceState(cryptoAmount = BigDecimal("5"), fiatAmount = BigDecimal("10")),
            ),
        )

        // Assert
        assertThat(actual.accountTitleUM)
            .isEqualTo(AccountTitleUM.Text(resourceReference(R.string.staking_staked_amount)))
        assertThat(actual.availableBalanceCrypto)
            .isEqualTo(stringReference(BigDecimal("5").format { crypto(currency) }))
    }

    @Test
    fun `GIVEN enter without accounts mode WHEN transform THEN wallet name title and wallet balance`() {
        // Act
        val actual = data(transform(actionType = enter))

        // Assert
        assertThat(actual.accountTitleUM).isEqualTo(AccountTitleUM.Text(stringReference(wallet.name)))
        assertThat(actual.availableBalanceCrypto)
            .isEqualTo(stringReference(BigDecimal("10").format { crypto(currency) }))
    }

    @Test
    fun `GIVEN pending action WHEN transform THEN amount title and balance state maximum`() {
        // Act
        val actual = data(
            transform(
                actionType = StakingActionCommonType.Pending.Rewards,
                balanceState = balanceState(cryptoAmount = BigDecimal.ONE),
            ),
        )

        // Assert
        assertThat(actual.accountTitleUM).isEqualTo(AccountTitleUM.Text(resourceReference(R.string.common_amount)))
        assertThat(actual.availableBalanceCrypto).isEqualTo(stringReference(BigDecimal.ONE.format { crypto(currency) }))
    }

    @Test
    fun `GIVEN pending without balance state WHEN transform THEN null maximum formatted`() {
        // Act
        val actual = data(transform(actionType = StakingActionCommonType.Pending.Other, balanceState = null))

        // Assert
        assertThat(actual.availableBalanceCrypto)
            .isEqualTo(stringReference((null as BigDecimal?).format { crypto(currency) }))
    }

    @Test
    fun `GIVEN hidden balance WHEN transform THEN masked crypto and empty fiat`() {
        // Act
        val actual = data(transform(actionType = enter, isBalanceHidden = true))

        // Assert
        assertThat(actual.availableBalanceCrypto).isEqualTo(stringReference(StringsSigns.THREE_STARS))
        assertThat(actual.availableBalanceFiat).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN loading status WHEN transform THEN empty amount state`() {
        // Act
        val actual = transform(
            actionType = enter,
            status = CryptoCurrencyStatus(coin(Blockchain.Solana), CryptoCurrencyStatus.Loading),
        )

        // Assert
        assertThat(actual.amountState).isEqualTo(AmountState.Empty)
    }

    private fun transform(
        actionType: StakingActionCommonType,
        balanceState: BalanceState? = null,
        isBalanceHidden: Boolean = false,
        status: CryptoCurrencyStatus = this.status,
    ): StakingUiState = SetAmountDataTransformer(
        clickIntents = clickIntents,
        cryptoCurrencyStatusProvider = Provider { status },
        userWalletProvider = Provider { wallet },
        appCurrencyProvider = Provider { AppCurrency.Default },
        isBalanceHidden = isBalanceHidden,
        isAccountsModeEnabled = false,
        account = null,
    ).transform(stakingUiState(actionType = actionType, balanceState = balanceState))

    private fun data(state: StakingUiState) = state.amountState as AmountState.Data
}