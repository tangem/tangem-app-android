package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Wallet screen top bar config
 *
 * @property endActions list of top bar end action buttons (e.g. QR scan, More)
 */
@Immutable
internal data class WalletTopBarConfig(
    val endActions: ImmutableList<TangemTopBarActionUM> = persistentListOf(),
)