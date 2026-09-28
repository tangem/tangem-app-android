package com.tangem.feature.tester.presentation.walletids.state

import kotlinx.collections.immutable.ImmutableList

internal data class WalletIdsScreenUM(
    val wallets: ImmutableList<WalletIdsUM>,
    val onBackClick: () -> Unit,
)

/**
 * @property hash the id Amplitude Experiment targets the wallet by
 */
internal data class WalletIdsUM(
    val name: String,
    val id: String,
    val hash: String,
    val onCopyIdClick: () -> Unit,
    val onCopyHashClick: () -> Unit,
)