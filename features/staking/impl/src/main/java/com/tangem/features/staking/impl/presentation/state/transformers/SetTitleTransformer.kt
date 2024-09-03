package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingStep
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.utils.getPendingActionTitle
import com.tangem.utils.transformer.Transformer

internal object SetTitleTransformer : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val actionType = prevState.actionType
        val currentStep = prevState.currentStep

        val title = when (currentStep) {
            StakingStep.Amount -> resourceReference(R.string.send_amount_label)
            StakingStep.Validators -> resourceReference(R.string.staking_validators)
            StakingStep.RewardsValidators -> resourceReference(R.string.common_claim_rewards)
            StakingStep.InitialInfo -> resourceReference(
                R.string.staking_title_stake,
                wrappedList(prevState.cryptoCurrencyName),
            )

            StakingStep.Confirmation -> {
                when (actionType) {
                    StakingActionCommonType.ENTER -> resourceReference(
                        R.string.staking_title_stake,
                        wrappedList(prevState.cryptoCurrencyName),
                    )
                    StakingActionCommonType.EXIT -> resourceReference(
                        R.string.staking_title_unstake,
                        wrappedList(prevState.cryptoCurrencyName),
                    )
                    else -> {
                        val confirmationState = prevState.confirmationState as? StakingStates.ConfirmationState.Data
                        val title = confirmationState?.pendingAction?.type?.getPendingActionTitle()

                        if (title == null || title == TextReference.EMPTY) {
                            resourceReference(
                                R.string.staking_title_stake,
                                wrappedList(prevState.cryptoCurrencyName),
                            )
                        } else {
                            title
                        }
                    }
                }
            }
        }

        val subtitle = when (currentStep) {
            StakingStep.Confirmation -> stringReference(prevState.walletName)
            else -> null
        }

        return prevState.copy(
            title = title,
            subtitle = subtitle,
        )
    }
}
