package com.tangem.features.staking.impl.presentation.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

/**
 * Guards the freshness gate on the staking confirm button at one representative site.
 *
 * All six transformers that gate `isPrimaryButtonEnabled` read the same expression; this one is the only place
 * where it is the *whole* condition, so a failure here can only mean the gate itself is wrong. The others carry
 * extra conjuncts (an amount check, a notifications check) that would muddy the signal, and cost far more setup.
 */
internal class SetConfirmationStateResetAssentTransformerTest {

    @Test
    fun `GIVEN every source actual WHEN transform THEN the primary button is enabled`() {
        // Arrange
        val transformer = SetConfirmationStateResetAssentTransformer(
            cryptoCurrencyStatus = statusWith(CryptoCurrencyStatus.Sources()),
        )

        // Act
        val actual = transformer.transform(stakingUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.isPrimaryButtonEnabled).isTrue()
        assertThat(confirmation.innerState).isEqualTo(InnerConfirmationStakingState.ASSENT)
    }

    @Test
    fun `GIVEN a stale contribution WHEN transform THEN the primary button is disabled`() {
        // Arrange — the toggle-on shape: `Sources.stakingBalanceSource` is stamped ACTUAL and the real staleness
        // rides on the contribution, which is what `stakingSource` resolves
        val transformer = SetConfirmationStateResetAssentTransformer(
            cryptoCurrencyStatus = statusWith(
                sources = CryptoCurrencyStatus.Sources(stakingBalanceSource = StatusSource.ACTUAL),
                contributions = listOf(cachedStakingBalance()),
            ),
        )

        // Act
        val actual = transformer.transform(stakingUiState())

        // Assert — reading `Sources.stakingBalanceSource` directly would leave this enabled
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN a stale legacy staking balance WHEN transform THEN the primary button is disabled`() {
        // Arrange — the toggle-off shape, which has to keep behaving exactly as it always did
        val transformer = SetConfirmationStateResetAssentTransformer(
            cryptoCurrencyStatus = statusWith(
                sources = CryptoCurrencyStatus.Sources(),
                stakingBalance = cachedStakingBalance(),
            ),
        )

        // Act
        val actual = transformer.transform(stakingUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN a stale network WHEN transform THEN the primary button is disabled`() {
        // Arrange — the pre-migration behaviour has to survive the switch untouched
        val transformer = SetConfirmationStateResetAssentTransformer(
            cryptoCurrencyStatus = statusWith(
                CryptoCurrencyStatus.Sources(networkSource = StatusSource.ONLY_CACHE),
            ),
        )

        // Act
        val actual = transformer.transform(stakingUiState())

        // Assert
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN only the quote stale WHEN transform THEN the primary button stays enabled`() {
        // Arrange — quotes sit in CACHE routinely; a stale price must not block staking
        val transformer = SetConfirmationStateResetAssentTransformer(
            cryptoCurrencyStatus = statusWith(
                CryptoCurrencyStatus.Sources(quoteSource = StatusSource.ONLY_CACHE),
            ),
        )

        // Act
        val actual = transformer.transform(stakingUiState())

        // Assert — this is why the gate reads `balanceSource` and not `total`
        val confirmation = actual.confirmationState as StakingStates.ConfirmationState.Data
        assertThat(confirmation.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN the confirmation state is not Data WHEN transform THEN it is left alone`() {
        // Arrange
        val empty = StakingStates.ConfirmationState.Empty()
        val transformer = SetConfirmationStateResetAssentTransformer(
            cryptoCurrencyStatus = statusWith(
                CryptoCurrencyStatus.Sources(networkSource = StatusSource.ONLY_CACHE),
            ),
        )

        // Act
        val actual = transformer.transform(stakingUiState(confirmationState = empty))

        // Assert
        assertThat(actual.confirmationState).isSameInstanceAs(empty)
    }

    // region Fixtures
    /**
     * [CryptoCurrencyStatus.Value.stakingBalance] and [CryptoCurrencyStatus.Value.contributions] are stubbed
     * explicitly, not left to `relaxed`. A relaxed mock hands back a *child mock* for the nullable balance rather
     * than `null`, and then a relaxed `StatusSource` resolves to the enum's first constant — which is `CACHE`,
     * not `ACTUAL`. That silently pins every case to "stale" and would let the disabled-button assertions pass
     * for the wrong reason.
     */
    private fun statusWith(
        sources: CryptoCurrencyStatus.Sources,
        stakingBalance: StakingBalance? = null,
        contributions: List<BalanceContribution> = emptyList(),
    ): CryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = mockk(relaxed = true),
        value = mockk(relaxed = true) {
            every { this@mockk.sources } returns sources
            every { this@mockk.stakingBalance } returns stakingBalance
            every { this@mockk.contributions } returns contributions
        },
    )

    /** A **real** balance: `source` is read off it, and a mocked enum would resolve to the first constant. */
    private fun cachedStakingBalance() = StakingBalance.Data.P2PEthPool(
        stakingId = StakingID(integrationId = "integration", address = "0x1"),
        source = StatusSource.CACHE,
        accounts = emptyList(),
    )

    /**
     * Mirrors `StakingStateController.getInitialState()`. `prevState` has to be a **real** [StakingUiState]: the
     * transformer calls `copy()`, and MockK intercepts that member on a data class, so a mocked state would hand
     * back another mock and the assertion would read nothing.
     */
    private fun stakingUiState(
        confirmationState: StakingStates.ConfirmationState = ConfirmationStatePreviewData.assentStakingState,
    ) = StakingUiState(
        title = TextReference.EMPTY,
        subtitle = null,
        clickIntents = StakingClickIntentsStub,
        walletName = "",
        cryptoCurrencyName = "",
        cryptoCurrencySymbol = "",
        cryptoCurrencyBlockchainId = "",
        currentStep = StakingStep.Confirmation,
        initialInfoState = StakingStates.InitialInfoState.Empty(),
        amountState = AmountState.Empty,
        validatorState = StakingStates.ValidatorState.Empty(),
        rewardsValidatorsState = StakingStates.RewardsValidatorsState.Empty(),
        confirmationState = confirmationState,
        isBalanceHidden = false,
        bottomSheetConfig = null,
        actionType = StakingActionCommonType.Enter(skipEnterAmount = false),
        buttonsState = NavigationButtonsState.Empty,
        balanceState = null,
        isColdWalletInteractionIconVisible = true,
        shouldShowHoldToConfirmButton = false,
    )
    // endregion
}