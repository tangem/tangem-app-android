package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.core.ui.R as CoreR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetTitleTransformerTest {

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
    private val stakeTitle = resourceReference(R.string.staking_title_stake, wrappedList("Solana"))

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN step and action WHEN transform THEN title and subtitle`(model: TitleModel) {
        // Arrange
        val state = stakingUiState(
            currentStep = model.step,
            actionType = model.actionType,
            cryptoCurrencyName = "Solana",
            walletName = "Wallet",
            confirmationState = model.confirmationState,
        )

        // Act
        val actual = SetTitleTransformer.transform(state)

        // Assert
        assertThat(actual.title).isEqualTo(model.expectedTitle)
        assertThat(actual.subtitle).isEqualTo(model.expectedSubtitle)
    }

    private fun provideTestModels() = listOf(
        TitleModel(StakingStep.Amount, enter, resourceReference(R.string.send_amount_label)),
        TitleModel(
            StakingStep.RestakeValidator,
            StakingActionCommonType.Pending.Restake,
            resourceReference(R.string.staking_validators),
        ),
        TitleModel(StakingStep.Validators, enter, resourceReference(R.string.staking_validators)),
        TitleModel(
            StakingStep.RewardsValidators,
            StakingActionCommonType.Pending.Rewards,
            resourceReference(R.string.common_claim_rewards),
        ),
        TitleModel(StakingStep.InitialInfo, enter, stakeTitle),
        TitleModel(StakingStep.Success, enter, TextReference.EMPTY),
        TitleModel(StakingStep.Confirmation, enter, stakeTitle, expectedSubtitle = stringReference("Wallet")),
        TitleModel(
            StakingStep.Confirmation,
            StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false),
            resourceReference(R.string.staking_title_unstake, wrappedList("Solana")),
        ),
        TitleModel(
            step = StakingStep.Confirmation,
            actionType = StakingActionCommonType.Pending.Rewards,
            expectedTitle = resourceReference(CoreR.string.common_claim_rewards),
            confirmationState = confirmationData(
                pendingAction = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS),
            ),
        ),
        TitleModel(
            step = StakingStep.Confirmation,
            actionType = StakingActionCommonType.Pending.Other,
            expectedTitle = stakeTitle,
            confirmationState = confirmationData(pendingAction = MockStaking.pendingAction(StakingActionType.UNKNOWN)),
        ),
        TitleModel(StakingStep.Confirmation, StakingActionCommonType.Pending.Other, stakeTitle),
        TitleModel(
            step = StakingStep.Confirmation,
            actionType = enter,
            expectedTitle = stakeTitle,
            expectedSubtitle = stringReference("Wallet"),
            confirmationState = confirmationData(
                pendingAction = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS),
            ),
        ),
    )

    internal data class TitleModel(
        val step: StakingStep,
        val actionType: StakingActionCommonType,
        val expectedTitle: TextReference,
        val expectedSubtitle: TextReference? = null,
        val confirmationState: StakingStates.ConfirmationState = StakingStates.ConfirmationState.Empty(),
    )
}