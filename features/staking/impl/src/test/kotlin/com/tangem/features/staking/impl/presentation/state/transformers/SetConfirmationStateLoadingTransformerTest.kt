package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.common.RewardSchedule
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.fixtures.coin
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.feeContent
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.features.staking.impl.presentation.state.utils.getRewardScheduleText
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetConfirmationStateLoadingTransformerTest {

    private val solana = coin(Blockchain.Solana)
    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)

    @Test
    fun `GIVEN enter with fiat amount WHEN transform THEN loading fee disabled and summary footer`() {
        // Act
        val actual = confirmation(transform(state(enter, fiatAmount = BigDecimal("20"))))

        // Assert
        val expectedFooter = resourceReference(
            R.string.staking_summary_description_text,
            wrappedList(
                BigDecimal("20").format { fiat(AppCurrency.Default.code, AppCurrency.Default.symbol) },
                requireNotNull(getRewardScheduleText(RewardSchedule.DAY, solana.network.rawId, decapitalize = true)),
            ),
        )
        assertThat(actual.feeState).isEqualTo(FeeState.Loading)
        assertThat(actual.isPrimaryButtonEnabled).isFalse()
        assertThat(actual.footerText).isEqualTo(expectedFooter)
    }

    @Test
    fun `GIVEN exit WHEN transform THEN empty footer`() {
        // Act
        val actual = confirmation(
            transform(state(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false), BigDecimal("20"))),
        )

        // Assert
        assertThat(actual.footerText).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN no fiat amount WHEN transform THEN empty footer`() {
        // Act
        val actual = confirmation(transform(state(enter, fiatAmount = null)))

        // Assert
        assertThat(actual.footerText).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN no amount data WHEN transform THEN loading fee and empty footer`() {
        // Arrange
        val input = stakingUiState(
            actionType = enter,
            amountState = AmountState.Empty,
            confirmationState = confirmationData(feeState = feeContent()),
        )

        // Act
        val actual = confirmation(transform(input))

        // Assert
        assertThat(actual.feeState).isEqualTo(FeeState.Loading)
        assertThat(actual.footerText).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN unknown reward schedule WHEN transform THEN empty footer`() {
        // Arrange
        val integration = stakeKitIntegration(
            yield = MockStaking.yield(
                metadata = MockStaking.metadata(rewardSchedule = Yield.Metadata.RewardSchedule.UNKNOWN),
            ),
        )

        // Act
        val actual = confirmation(transform(state(enter, BigDecimal("20")), integration))

        // Assert
        assertThat(actual.footerText).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN empty confirmation WHEN transform THEN confirmation unchanged`() {
        // Arrange
        val input = stakingUiState(amountState = amountData(fiatAmount = BigDecimal("20")))

        // Act
        val actual = transform(input)

        // Assert
        assertThat(actual.confirmationState).isEqualTo(StakingStates.ConfirmationState.Empty())
    }

    private fun state(actionType: StakingActionCommonType, fiatAmount: BigDecimal?) = stakingUiState(
        actionType = actionType,
        amountState = amountData(fiatAmount = fiatAmount),
        confirmationState = confirmationData(feeState = feeContent()),
    )

    private fun transform(state: StakingUiState, integration: StakingIntegration = stakeKitIntegration()) =
        SetConfirmationStateLoadingTransformer(
            integration = integration,
            appCurrency = AppCurrency.Default,
            cryptoCurrency = solana,
        ).transform(state)

    private fun confirmation(state: StakingUiState) = state.confirmationState as StakingStates.ConfirmationState.Data
}