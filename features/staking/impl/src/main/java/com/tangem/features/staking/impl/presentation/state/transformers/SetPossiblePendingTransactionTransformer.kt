package com.tangem.features.staking.impl.presentation.state.transformers

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.transformer.Transformer

internal class SetPossiblePendingTransactionTransformer(
    private val yield: Yield,
    private val balanceState: BalanceState,
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
) : Transformer<StakingUiState> {

    override fun transform(prevState: StakingUiState): StakingUiState {
        val possibleConfirmationState =
            prevState.confirmationState as? StakingStates.ConfirmationState.Data ?: return prevState
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data

        val balancesId = yieldBalance?.getBalancesUniqueId() ?: 0
        val token = yield.getCurrentToken(cryptoCurrencyStatus.currency.id.rawCurrencyId)

        return prevState.copy(
            confirmationState = possibleConfirmationState.copy(
                possiblePendingTransaction = PendingTransaction(
                    groupId = balanceState.groupId,
                    token = token,
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