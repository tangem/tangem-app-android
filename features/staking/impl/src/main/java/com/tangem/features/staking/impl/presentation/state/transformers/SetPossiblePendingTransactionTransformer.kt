package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.transformer.Transformer

internal class SetPossiblePendingTransactionTransformer(
    private val balanceState: BalanceState,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val possibleConfirmationState =
            prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState

        return prevState.copy(
            confirmationState = possibleConfirmationState.copy(
                possiblePendingTransaction = PendingTransaction(
                    groupId = balanceState.groupId,
                    type = balanceState.type,
                    amount = balanceState.cryptoDecimal,
                    rawCurrencyId = balanceState.rawCurrencyId,
                    validator = balanceState.validator,
                )
            ),
        )
    }
}
