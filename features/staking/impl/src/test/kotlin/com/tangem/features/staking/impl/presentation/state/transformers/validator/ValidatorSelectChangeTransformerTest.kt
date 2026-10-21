package com.tangem.features.staking.impl.presentation.state.transformers.validator

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.features.staking.impl.presentation.state.target
import com.tangem.features.staking.impl.presentation.state.validatorData
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Test

internal class ValidatorSelectChangeTransformerTest {

    private val secondValidator = MockStaking.validator(
        address = MockStaking.SECOND_VALIDATOR_ADDRESS,
        name = "Validator 2",
    )
    private val first = target()
    private val second = target(secondValidator)

    private val twoPreferred = cosmosIntegration(listOf(MockStaking.validator(), secondValidator))
    private val onePreferred = cosmosIntegration(listOf(MockStaking.validator()))
    private val nonePreferred = cosmosIntegration(listOf(MockStaking.validator(preferred = false)))

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)

    @Test
    fun `GIVEN enter on amount step and selected target WHEN transform THEN selected chosen and all preferred available`() {
        // Act
        val actual = transform(twoPreferred, second, stakingUiState(StakingStep.Amount, enter))

        // Assert
        assertThat(actual.validatorState).isEqualTo(
            StakingStates.ValidatorState.Data(
                isPrimaryButtonEnabled = true,
                isClickable = true,
                isVisibleOnConfirmation = true,
                chosenTarget = second,
                activeTarget = null,
                availableTargets = listOf(first, second),
            ),
        )
    }

    @Test
    fun `GIVEN enter on initial info and selected target WHEN transform THEN selected target kept`() {
        // Act
        val actual = transform(twoPreferred, second, stakingUiState(StakingStep.InitialInfo, enter))

        // Assert
        assertThat(validator(actual).chosenTarget).isEqualTo(second)
    }

    @Test
    fun `GIVEN no selected target WHEN transform THEN first preferred chosen`() {
        // Act
        val actual = transform(twoPreferred, null, stakingUiState(StakingStep.Amount, enter))

        // Assert
        assertThat(validator(actual).chosenTarget).isEqualTo(first)
    }

    @Test
    fun `GIVEN no selected and no preferred WHEN transform THEN same state`() {
        // Arrange
        val input = stakingUiState(StakingStep.Amount, enter)

        // Act
        val actual = transform(nonePreferred, null, input)

        // Assert
        assertThat(actual).isSameInstanceAs(input)
    }

    @Test
    fun `GIVEN single preferred WHEN transform THEN not clickable`() {
        // Act
        val actual = transform(onePreferred, first, stakingUiState(StakingStep.Amount, enter))

        // Assert
        assertThat(validator(actual).isClickable).isFalse()
    }

    @Test
    fun `GIVEN restake from initial info WHEN transform THEN active excluded and next preferred chosen`() {
        // Act
        val actual = transform(
            twoPreferred,
            first,
            stakingUiState(StakingStep.InitialInfo, StakingActionCommonType.Pending.Restake),
        )

        // Assert
        val data = validator(actual)
        assertThat(data.activeTarget).isEqualTo(first)
        assertThat(data.availableTargets).containsExactly(second)
        assertThat(data.chosenTarget).isEqualTo(second)
        assertThat(data.isVisibleOnConfirmation).isTrue()
    }

    @Test
    fun `GIVEN pending stake from initial info WHEN transform THEN same restake selection`() {
        // Act
        val actual = transform(
            twoPreferred,
            first,
            stakingUiState(StakingStep.InitialInfo, StakingActionCommonType.Pending.Stake(shouldSkipEnterAmount = false)),
        )

        // Assert
        assertThat(validator(actual).activeTarget).isEqualTo(first)
        assertThat(validator(actual).chosenTarget).isEqualTo(second)
    }

    @Test
    fun `GIVEN restake on validator step WHEN transform THEN active kept from validator state`() {
        // Arrange
        val input = stakingUiState(
            currentStep = StakingStep.RestakeValidator,
            actionType = StakingActionCommonType.Pending.Restake,
            validatorState = validatorData(chosenTarget = second, activeTarget = first),
        )

        // Act
        val actual = transform(twoPreferred, second, input)

        // Assert
        val data = validator(actual)
        assertThat(data.activeTarget).isEqualTo(first)
        assertThat(data.chosenTarget).isEqualTo(second)
        assertThat(data.availableTargets).containsExactly(second)
    }

    @Test
    fun `GIVEN exit WHEN transform THEN not visible on confirmation`() {
        // Act
        val actual = transform(
            twoPreferred,
            first,
            stakingUiState(StakingStep.Confirmation, StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)),
        )

        // Assert
        assertThat(validator(actual).isVisibleOnConfirmation).isFalse()
    }

    @Test
    fun `GIVEN pending vote locked WHEN transform THEN visible on confirmation`() {
        // Arrange
        val input = stakingUiState(
            currentStep = StakingStep.Confirmation,
            actionType = StakingActionCommonType.Pending.Other,
            confirmationState = confirmationData(
                pendingAction = MockStaking.pendingAction(StakingActionType.VOTE_LOCKED),
            ),
        )

        // Act
        val actual = transform(twoPreferred, first, input)

        // Assert
        assertThat(validator(actual).isVisibleOnConfirmation).isTrue()
    }

    @Test
    fun `GIVEN pending rewards WHEN transform THEN not visible on confirmation`() {
        // Arrange
        val input = stakingUiState(
            currentStep = StakingStep.Confirmation,
            actionType = StakingActionCommonType.Pending.Rewards,
            confirmationState = confirmationData(
                pendingAction = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS),
            ),
        )

        // Act
        val actual = transform(twoPreferred, first, input)

        // Assert
        assertThat(validator(actual).isVisibleOnConfirmation).isFalse()
    }

    private fun cosmosIntegration(validators: List<Yield.Validator>) = stakeKitIntegration(
        id = StakingIntegrationID.StakeKit.Coin.Cosmos,
        yield = MockStaking.yield(id = MockStaking.COSMOS_ID, token = MockStaking.atomToken, validators = validators),
    )

    private fun transform(integration: StakingIntegration, selected: StakingTarget?, state: StakingUiState) =
        ValidatorSelectChangeTransformer(integration = integration, selectedTarget = selected).transform(state)

    private fun validator(state: StakingUiState) = state.validatorState as StakingStates.ValidatorState.Data
}