package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class TokenSyncProgressUM {

    data object Idle : TokenSyncProgressUM()

    data class InProgress(val progressPercent: Int) : TokenSyncProgressUM()

    data object Completed : TokenSyncProgressUM()
}