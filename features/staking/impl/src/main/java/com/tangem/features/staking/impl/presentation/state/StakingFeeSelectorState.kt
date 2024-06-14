package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.blockchain.common.transaction.TransactionFee

@Immutable
internal sealed class StakingFeeSelectorState {

    data class Content(
        val fees: TransactionFee,
    ) : StakingFeeSelectorState()

    data object Loading : StakingFeeSelectorState()

    data object Error : StakingFeeSelectorState()
}
