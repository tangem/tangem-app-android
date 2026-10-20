package com.tangem.features.staking.impl.fixtures

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakingBalanceData
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class StakingFixturesSmokeTest {

    @Test
    fun `GIVEN tron coin WHEN coin THEN network raw id is tron network id`() {
        // Act
        val rawId = coin(Blockchain.Tron).network.rawId

        // Assert
        assertThat(rawId).isEqualTo(Blockchain.Tron.toNetworkId())
        assertThat(BlockchainUtils.isTron(rawId)).isTrue()
    }

    @Test
    fun `GIVEN staking balance WHEN currencyStatus THEN contribution readable as stakingBalanceData`() {
        // Act
        val value = currencyStatus(stakingBalance = MockStaking.stakeKitBalance()).value

        // Assert
        assertThat(value).isInstanceOf(CryptoCurrencyStatus.Loaded::class.java)
        assertThat(value.stakingBalanceData).isEqualTo(MockStaking.stakeKitBalance())
    }

    @Test
    fun `GIVEN null amount WHEN currencyStatus THEN NoAmount value`() {
        // Act
        val value = currencyStatus(amount = null).value

        // Assert
        assertThat(value).isInstanceOf(CryptoCurrencyStatus.NoAmount::class.java)
        assertThat(value.amount).isNull()
    }

    @Test
    fun `GIVEN null fiat rate WHEN currencyStatus THEN NoQuote value`() {
        // Act
        val value = currencyStatus(fiatRate = null).value

        // Assert
        assertThat(value).isInstanceOf(CryptoCurrencyStatus.NoQuote::class.java)
        assertThat(value.fiatRate).isNull()
        assertThat(value.amount).isEqualTo(BigDecimal("10"))
    }

    @Test
    fun `GIVEN defaults WHEN amountData THEN crypto amount is one`() {
        // Act
        val data = amountData()

        // Assert
        assertThat(data.amountTextField.cryptoAmount.value).isEqualTo(BigDecimal.ONE)
        assertThat(data.amountTextField.value).isEqualTo("1")
    }

    @Test
    fun `GIVEN defaults WHEN stakingUiState THEN initial info step with enter action`() {
        // Act
        val state = stakingUiState()

        // Assert
        assertThat(state.currentStep).isEqualTo(StakingStep.InitialInfo)
        assertThat(state.actionType).isEqualTo(StakingActionCommonType.Enter(shouldSkipEnterAmount = false))
    }
}