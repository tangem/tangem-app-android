package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.transformer.Transformer

internal object SetTitleTransformer : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val actionType = prevState.actionType
        val currentStep = prevState.currentStep

        val title = when {
            currentStep == StakingStep.Amount -> {
                resourceReference(R.string.send_amount_label)
            }

            currentStep == StakingStep.Validators -> {
                resourceReference(R.string.staking_validators)
            }

            actionType == StakingActionCommonType.EXIT && currentStep != StakingStep.InitialInfo -> {
                resourceReference(
                    R.string.staking_title_unstake,
                    wrappedList(prevState.cryptoCurrencyName),
                )
            }
            else -> {
                resourceReference(
                    R.string.staking_title_stake,
                    wrappedList(prevState.cryptoCurrencyName),
                )
            }
        }

        return prevState.copy(title = title)
    }
}
