package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.transaction.error.SendTransactionError

internal interface StakingTransactionSender {

    suspend fun send(callbacks: Callbacks)

    class Callbacks(
        val onConstructSuccess: (List<StakingTransaction>) -> Unit,
        val onConstructError: (StakingError) -> Unit,
        val onSendSuccess: (String) -> Unit,
        val onSendError: (SendTransactionError) -> Unit,
        val onFeeIncreased: (Fee, Boolean) -> Unit,
        val onTransactionExpired: () -> Unit,
    )
}