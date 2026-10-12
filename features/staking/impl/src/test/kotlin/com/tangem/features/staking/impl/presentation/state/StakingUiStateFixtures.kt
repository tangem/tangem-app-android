package com.tangem.features.staking.impl.presentation.state

import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationButtonsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.presentation.state.previewdata.ConfirmationStatePreviewData
import com.tangem.features.staking.impl.presentation.state.stub.StakingClickIntentsStub
import io.mockk.mockk

/**
 * A **real** [StakingUiState] on the assent Confirmation step. Transformers call `copy()`, which MockK intercepts on
 * a mocked data class, so applying a transformer to a mocked state would read nothing.
 */
internal fun createAssentConfirmationUiState(
    actionType: StakingActionCommonType = StakingActionCommonType.Exit(partiallyUnstakeDisabled = false),
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
    amountState = mockk<AmountState.Data>(relaxed = true),
    validatorState = StakingStates.ValidatorState.Empty(),
    rewardsValidatorsState = StakingStates.RewardsValidatorsState.Empty(),
    confirmationState = ConfirmationStatePreviewData.assentStakingState,
    isBalanceHidden = false,
    bottomSheetConfig = null,
    actionType = actionType,
    buttonsState = NavigationButtonsState.Empty,
    balanceState = null,
    isColdWalletInteractionIconVisible = true,
    shouldShowHoldToConfirmButton = false,
)