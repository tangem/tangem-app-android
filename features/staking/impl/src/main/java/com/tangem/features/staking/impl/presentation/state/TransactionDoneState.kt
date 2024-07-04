package com.tangem.features.staking.impl.presentation.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class TransactionDoneState {

    data class Content(
        val timestamp: Long,
    ) : TransactionDoneState()

    data object Empty : TransactionDoneState()
}
