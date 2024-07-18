package com.tangem.feature.walletsettings.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList

@Immutable
internal data class WalletSettingsUM(
    val popBack: () -> Unit,
    val items: PersistentList<WalletSettingsItemUM>,
)