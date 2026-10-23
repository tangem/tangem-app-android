package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.model.StakingClickIntents
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.TransactionDoneState
import com.tangem.features.staking.impl.presentation.state.amountData
import com.tangem.features.staking.impl.presentation.state.confirmationData
import com.tangem.features.staking.impl.presentation.state.initialInfoData
import com.tangem.features.staking.impl.presentation.state.stakingUiState
import com.tangem.features.staking.impl.presentation.state.yieldBalanceData
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SetButtonsStateTransformerTest {

    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val clickIntents: StakingClickIntents = mockk(relaxed = true)
    private val transformer = SetButtonsStateTransformer(urlOpener)

    private val enter = StakingActionCommonType.Enter(shouldSkipEnterAmount = false)
    private val exit = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)
    private val cardanoId = Blockchain.Cardano.toNetworkId()

    @BeforeEach
    fun resetMocks() {
        clearMocks(urlOpener, clickIntents)
    }

    @Nested
    inner class Visibility {

        @Test
        fun `GIVEN rewards validators step WHEN transform THEN buttons empty`() {
            // Act
            val actual = transformer.transform(stakingUiState(currentStep = StakingStep.RewardsValidators))

            // Assert
            assertThat(actual.buttonsState).isEqualTo(NavigationButtonsState.Empty)
        }

        @Test
        fun `GIVEN empty tx url WHEN transform THEN no extra buttons`() {
            // Arrange
            val confirmation = confirmationData(transactionDoneState = TransactionDoneState.Content(0L, ""))

            // Act
            val actual = buttons(state(StakingStep.Confirmation, confirmationState = confirmation))

            // Assert
            assertThat(actual.extraButtons).isNull()
            assertThat(actual.txUrl).isEmpty()
        }

        @Test
        fun `GIVEN tx url WHEN transform THEN explore and share buttons`() {
            // Arrange
            val confirmation = confirmationData(transactionDoneState = TransactionDoneState.Content(0L, "https://tx"))

            // Act
            val actual = buttons(state(StakingStep.Success, confirmationState = confirmation))
            val (explore, share) = requireNotNull(actual.extraButtons)
            explore.onClick()
            share.onClick()

            // Assert
            assertThat(explore.textReference).isEqualTo(resourceReference(R.string.common_explore))
            assertThat(explore.iconRes).isEqualTo(R.drawable.ic_web_24)
            assertThat(explore.isSecondary).isTrue()
            assertThat(share.textReference).isEqualTo(resourceReference(R.string.common_share))
            assertThat(share.iconRes).isEqualTo(R.drawable.ic_share_24)
            assertThat(share.isSecondary).isTrue()
            assertThat(actual.txUrl).isEqualTo("https://tx")
            verify(exactly = 1) {
                clickIntents.onExploreClick()
                clickIntents.onShareClick()
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PrimaryText {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN state WHEN transform THEN primary text`(model: TextModel) {
            // Act
            val actual = primary(model.state)

            // Assert
            assertThat(actual.textReference).isEqualTo(model.expected)
        }

        // Pins current behaviour: a pending confirmation without an action renders an empty primary text.
        @Test
        fun `GIVEN pending confirmation without action WHEN transform THEN empty primary text`() {
            // Arrange
            val state = state(
                step = StakingStep.Confirmation,
                actionType = StakingActionCommonType.Pending.Other,
                confirmationState = confirmationData(pendingAction = null),
            )

            // Act
            val actual = primary(state)

            // Assert
            assertThat(actual.textReference).isEqualTo(TextReference.EMPTY)
        }

        @Suppress("LongMethod")
        private fun provideTestModels() = listOf(
            TextModel(
                name = "initial info with stake",
                state = state(
                    StakingStep.InitialInfo,
                    initialInfoState = initialInfoData(yieldBalance = yieldBalanceData()),
                ),
                expected = resourceReference(R.string.staking_stake_more),
            ),
            TextModel(
                name = "initial info without stake",
                state = state(StakingStep.InitialInfo, initialInfoState = initialInfoData()),
                expected = resourceReference(R.string.common_stake),
            ),
            TextModel(
                name = "initial info empty",
                state = state(StakingStep.InitialInfo),
                expected = resourceReference(R.string.common_stake),
            ),
            TextModel("success", state(StakingStep.Success), resourceReference(R.string.common_close)),
            TextModel("validators", state(StakingStep.Validators), resourceReference(R.string.common_continue)),
            TextModel("amount", state(StakingStep.Amount), resourceReference(R.string.common_next)),
            TextModel("restake", state(StakingStep.RestakeValidator), resourceReference(R.string.common_next)),
            TextModel(
                name = "confirmation empty",
                state = state(StakingStep.Confirmation),
                expected = resourceReference(R.string.common_close),
            ),
            TextModel(
                name = "confirmation enter",
                state = state(StakingStep.Confirmation, confirmationState = confirmationData()),
                expected = resourceReference(R.string.common_stake),
            ),
            TextModel(
                name = "confirmation exit",
                state = state(StakingStep.Confirmation, exit, confirmationState = confirmationData()),
                expected = resourceReference(R.string.common_unstake),
            ),
            TextModel(
                name = "confirmation claim rewards",
                state = state(
                    step = StakingStep.Confirmation,
                    actionType = StakingActionCommonType.Pending.Rewards,
                    confirmationState = confirmationData(
                        pendingAction = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS),
                    ),
                ),
                expected = resourceReference(R.string.common_claim_rewards),
            ),
            TextModel("approval required", approvalRequiredState(), resourceReference(R.string.give_permission_title)),
            TextModel(
                name = "approval needed but allowance covers amount",
                state = approvalRequiredState(allowance = BigDecimal("10")),
                expected = resourceReference(R.string.common_stake),
            ),
            TextModel(
                name = "approval needed but amount not entered",
                state = approvalRequiredState().copy(amountState = AmountState.Empty),
                expected = resourceReference(R.string.common_stake),
            ),
            TextModel(
                name = "approval needed but exit",
                state = approvalRequiredState().copy(actionType = exit),
                expected = resourceReference(R.string.common_unstake),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Flags {

        @Test
        fun `GIVEN cold wallet icon flag WHEN transform THEN tangem icon`() {
            // Act
            val actual = primary(state(StakingStep.Amount).copy(isColdWalletInteractionIconVisible = true))

            // Assert
            assertThat(actual.iconRes).isEqualTo(R.drawable.ic_tangem_24)
        }

        @Test
        fun `GIVEN no cold icon flag WHEN transform THEN no icon res`() {
            // Act
            val actual = primary(state(StakingStep.Amount).copy(isColdWalletInteractionIconVisible = false))

            // Assert
            assertThat(actual.iconRes).isNull()
        }

        @Test
        fun `GIVEN hot wallet confirmation WHEN transform THEN hold to confirm without icon`() {
            // Act
            val actual = primary(hot(state(StakingStep.Confirmation, confirmationState = confirmationData())))

            // Assert
            assertThat(actual.isHoldToConfirm).isTrue()
            assertThat(actual.isIconVisible).isFalse()
        }

        @Test
        fun `GIVEN hot wallet completed WHEN transform THEN no hold to confirm`() {
            // Arrange
            val confirmation = confirmationData(innerState = InnerConfirmationStakingState.COMPLETED)

            // Act
            val actual = primary(hot(state(StakingStep.Confirmation, confirmationState = confirmation)))

            // Assert
            assertThat(actual.isHoldToConfirm).isFalse()
            assertThat(actual.isIconVisible).isFalse()
        }

        @Test
        fun `GIVEN hot wallet approval required WHEN transform THEN no hold to confirm and icon visible`() {
            // Act
            val actual = primary(hot(approvalRequiredState()))

            // Assert
            assertThat(actual.isHoldToConfirm).isFalse()
            assertThat(actual.isIconVisible).isTrue()
        }

        @Test
        fun `GIVEN hot wallet confirmation step without confirmation data WHEN transform THEN hold to confirm`() {
            // Act
            val actual = primary(hot(state(StakingStep.Confirmation)))

            // Assert
            assertThat(actual.isHoldToConfirm).isTrue()
        }

        @Test
        fun `GIVEN cold confirmation WHEN transform THEN icon visible`() {
            // Act
            val actual = primary(state(StakingStep.Confirmation, confirmationState = confirmationData()))

            // Assert
            assertThat(actual.isIconVisible).isTrue()
            assertThat(actual.isHoldToConfirm).isFalse()
        }

        @Test
        fun `GIVEN not confirmation step WHEN transform THEN icon hidden`() {
            // Act
            val actual = primary(state(StakingStep.Amount))

            // Assert
            assertThat(actual.isIconVisible).isFalse()
        }

        @Test
        fun `GIVEN in progress WHEN transform THEN progress shown`() {
            // Arrange
            val confirmation = confirmationData(innerState = InnerConfirmationStakingState.IN_PROGRESS)

            // Act
            val actual = primary(state(StakingStep.Confirmation, confirmationState = confirmation))

            // Assert
            assertThat(actual.shouldShowProgress).isTrue()
        }

        @Test
        fun `GIVEN validation in progress WHEN transform THEN progress disabled no icon no hold`() {
            // Arrange
            val confirmation = confirmationData(isValidationInProgress = true, isPrimaryButtonEnabled = true)

            // Act
            val actual = primary(hot(state(StakingStep.Confirmation, confirmationState = confirmation)))

            // Assert
            assertThat(actual.shouldShowProgress).isTrue()
            assertThat(actual.isEnabled).isFalse()
            assertThat(actual.isIconVisible).isFalse()
            assertThat(actual.isHoldToConfirm).isFalse()
        }

        @Test
        fun `GIVEN validation flag outside confirmation WHEN transform THEN ignored`() {
            // Arrange
            val state = state(
                step = StakingStep.Amount,
                amountState = amountData(isPrimaryButtonEnabled = true),
                confirmationState = confirmationData(isValidationInProgress = true),
            )

            // Act
            val actual = primary(state)

            // Assert
            assertThat(actual.shouldShowProgress).isFalse()
            assertThat(actual.isEnabled).isTrue()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN step enabled flags WHEN transform THEN enabled follows step state`(model: EnabledModel) {
            // Act
            val actual = primary(model.state)

            // Assert
            assertThat(actual.isEnabled).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            EnabledModel(
                name = "initial info disabled",
                state = state(
                    StakingStep.InitialInfo,
                    initialInfoState = initialInfoData(isPrimaryButtonEnabled = false),
                ),
                expected = false,
            ),
            EnabledModel(
                name = "amount disabled",
                state = state(StakingStep.Amount, amountState = amountData(isPrimaryButtonEnabled = false)),
                expected = false,
            ),
            EnabledModel(
                name = "confirmation enabled",
                state = state(
                    StakingStep.Confirmation,
                    confirmationState = confirmationData(isPrimaryButtonEnabled = true),
                ),
                expected = true,
            ),
            EnabledModel(
                name = "confirmation disabled",
                state = state(
                    StakingStep.Confirmation,
                    confirmationState = confirmationData(isPrimaryButtonEnabled = false),
                ),
                expected = false,
            ),
            EnabledModel("validators", state(StakingStep.Validators), expected = true),
            EnabledModel("restake validator", state(StakingStep.RestakeValidator), expected = true),
            EnabledModel("success", state(StakingStep.Success), expected = true),
        )
    }

    @Nested
    inner class Dimmed {

        @Test
        fun `GIVEN cardano with stake on initial info WHEN transform THEN dimmed`() {
            // Act
            val actual = primary(cardanoWithStake())

            // Assert
            assertThat(actual.isDimmed).isTrue()
        }

        @Test
        fun `GIVEN cardano without stake WHEN transform THEN not dimmed`() {
            // Arrange
            val state = state(StakingStep.InitialInfo, initialInfoState = initialInfoData())
                .copy(cryptoCurrencyBlockchainId = cardanoId)

            // Act
            val actual = primary(state)

            // Assert
            assertThat(actual.isDimmed).isFalse()
        }

        // Pins current behaviour: a Cardano initial screen without loaded data counts as already staked.
        @Test
        fun `GIVEN cardano with empty initial info WHEN transform THEN dimmed`() {
            // Act
            val actual = primary(state(StakingStep.InitialInfo).copy(cryptoCurrencyBlockchainId = cardanoId))

            // Assert
            assertThat(actual.isDimmed).isTrue()
        }

        @Test
        fun `GIVEN cardano on amount step WHEN transform THEN not dimmed`() {
            // Act
            val actual = primary(cardanoWithStake().copy(currentStep = StakingStep.Amount))

            // Assert
            assertThat(actual.isDimmed).isFalse()
        }

        @Test
        fun `GIVEN stake and all targets full WHEN transform THEN dimmed`() {
            // Arrange
            val state = state(
                StakingStep.InitialInfo,
                initialInfoState = initialInfoData(yieldBalance = yieldBalanceData(), areAllTargetsFull = true),
            )

            // Act
            val actual = primary(state)

            // Assert
            assertThat(actual.isDimmed).isTrue()
        }

        @Test
        fun `GIVEN no stake and all targets full WHEN transform THEN not dimmed`() {
            // Arrange
            val state = state(StakingStep.InitialInfo, initialInfoState = initialInfoData(areAllTargetsFull = true))

            // Act
            val actual = primary(state)

            // Assert
            assertThat(actual.isDimmed).isFalse()
        }
    }

    @Nested
    inner class PrimaryClick {

        @Test
        fun `GIVEN dimmed button WHEN primary clicked THEN primary click alert shown`() {
            // Act
            primary(cardanoWithStake()).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.showPrimaryClickAlert() }
            verify(exactly = 0) { clickIntents.onNextClick(any()) }
        }

        @Test
        fun `GIVEN initial info WHEN primary clicked THEN next click`() {
            // Act
            primary(state(StakingStep.InitialInfo)).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.onNextClick(null) }
        }

        @Test
        fun `GIVEN validators WHEN primary clicked THEN next click`() {
            // Act
            primary(state(StakingStep.Validators)).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.onNextClick(null) }
        }

        @Test
        fun `GIVEN restake validator WHEN primary clicked THEN next click`() {
            // Act
            primary(state(StakingStep.RestakeValidator)).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.onNextClick(null) }
        }

        @Test
        fun `GIVEN success WHEN primary clicked THEN next click`() {
            // Act
            primary(state(StakingStep.Success)).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.onNextClick(null) }
        }

        @Test
        fun `GIVEN amount WHEN primary clicked THEN amount enter click`() {
            // Act
            primary(state(StakingStep.Amount)).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.onAmountEnterClick() }
        }

        @Test
        fun `GIVEN completed confirmation WHEN primary clicked THEN next click`() {
            // Arrange
            val confirmation = confirmationData(innerState = InnerConfirmationStakingState.COMPLETED)

            // Act
            primary(state(StakingStep.Confirmation, confirmationState = confirmation)).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.onNextClick(null) }
        }

        @Test
        fun `GIVEN approval required WHEN primary clicked THEN approval bottom sheet shown`() {
            // Act
            primary(approvalRequiredState()).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.showApprovalBottomSheet() }
        }

        @Test
        fun `GIVEN assent confirmation WHEN primary clicked THEN action click`() {
            // Act
            primary(state(StakingStep.Confirmation, confirmationState = confirmationData())).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.onActionClick() }
        }

        @Test
        fun `GIVEN empty confirmation WHEN primary clicked THEN back click`() {
            // Act
            primary(state(StakingStep.Confirmation)).onClick()

            // Assert
            verify(exactly = 1) { clickIntents.onBackClick() }
        }
    }

    private fun state(
        step: StakingStep,
        actionType: StakingActionCommonType = enter,
        initialInfoState: StakingStates.InitialInfoState = StakingStates.InitialInfoState.Empty(),
        amountState: AmountState = amountData(),
        confirmationState: StakingStates.ConfirmationState = StakingStates.ConfirmationState.Empty(),
    ) = stakingUiState(
        currentStep = step,
        actionType = actionType,
        clickIntents = clickIntents,
        initialInfoState = initialInfoState,
        amountState = amountState,
        confirmationState = confirmationState,
    )

    private fun approvalRequiredState(allowance: BigDecimal = BigDecimal("5")) = state(
        step = StakingStep.Confirmation,
        amountState = amountData(BigDecimal("10")),
        confirmationState = confirmationData(isApprovalNeeded = true, allowance = allowance),
    )

    private fun cardanoWithStake() = state(
        StakingStep.InitialInfo,
        initialInfoState = initialInfoData(yieldBalance = yieldBalanceData()),
    ).copy(cryptoCurrencyBlockchainId = cardanoId)

    private fun hot(state: StakingUiState) = state.copy(shouldShowHoldToConfirmButton = true)

    private fun buttons(state: StakingUiState) =
        transformer.transform(state).buttonsState as NavigationButtonsState.Data

    private fun primary(state: StakingUiState): NavigationButton = requireNotNull(buttons(state).primaryButton)

    internal data class TextModel(val name: String, val state: StakingUiState, val expected: TextReference) {
        override fun toString() = name
    }

    internal data class EnabledModel(val name: String, val state: StakingUiState, val expected: Boolean) {
        override fun toString() = name
    }
}