package com.tangem.feature.tester.presentation.excludedblockchains.state

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.PersistentList

internal data class ExcludedBlockchainsScreenUM(
    val popBack: () -> Unit,
    val search: SearchBarUM,
    val blockchains: PersistentList<BlockchainUM>,
    val showRecoverWarning: Boolean,
    val appVersion: String,
    val onRecoverClick: () -> Unit,
    val onRestartClick: () -> Unit,
)