package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.utils.transformer.Transformer

internal class SetPossiblePendingTransactionTransformer(
    private val balanceState: BalanceState,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val possibleConfirmationState =
            prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState

        val balancesId = (cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data)?.getBalancesUniqueId() ?: 0

        return prevState.copy(
            confirmationState = possibleConfirmationState.copy(
                possiblePendingTransaction = PendingTransaction(
                    groupId = balanceState.groupId,
                    type = balanceState.type,
                    amount = balanceState.cryptoDecimal,
                    rawCurrencyId = balanceState.rawCurrencyId,
                    validator = balanceState.validator,
                    balancesId = balancesId,
                ),
            ),
        )
    }
}
