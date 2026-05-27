package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class AssetsDiscoveryProgressUM {

    data object Idle : AssetsDiscoveryProgressUM()

    data class InProgress(val progressPercent: Int) : AssetsDiscoveryProgressUM()

    data object Completed : AssetsDiscoveryProgressUM()
}