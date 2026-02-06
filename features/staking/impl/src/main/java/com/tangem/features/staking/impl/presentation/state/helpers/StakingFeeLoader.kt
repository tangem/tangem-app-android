package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.transaction.error.GetFeeError

internal interface StakingFeeLoader {

    suspend fun getFee(
        onStakingFee: (Fee, Boolean) -> Unit,
        onStakingFeeError: (StakingError) -> Unit,
        onApprovalFee: (TransactionFee) -> Unit,
        onFeeError: (GetFeeError) -> Unit,
    )
}