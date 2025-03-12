package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.InnerConfirmationStakingState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.TransactionDoneState
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SetConfirmationStateCompletedTransformer(
    private val txUrl: String,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        return prevState.copy(
            confirmationState = prevState.confirmationState.copyWrapped(),
        )
    }

    private fun StakingStates.ConfirmationState.copyWrapped(): StakingStates.ConfirmationState {
        return if (this is StakingStates.ConfirmationState.Data) {
            copy(
                isPrimaryButtonEnabled = with(cryptoCurrencyStatus.value) {
                    sources.yieldBalanceSource.isActual() && sources.networkSource.isActual()
                },
                innerState = InnerConfirmationStakingState.COMPLETED,
                footerText = TextReference.EMPTY,
                notifications = persistentListOf(),
                transactionDoneState = TransactionDoneState.Content(
                    timestamp = System.currentTimeMillis(),
                    txUrl = txUrl,
                ),
            )
        } else {
            this
        }
    }
}