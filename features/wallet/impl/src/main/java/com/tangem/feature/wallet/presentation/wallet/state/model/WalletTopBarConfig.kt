package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Wallet screen top bar config
 *
 * @property endActions list of top bar end action buttons (e.g. QR scan, More)
 * @property onAllServicesClick opens All Services from the start button; `null` shows the Tangem logo instead
 */
@Immutable
internal data class WalletTopBarConfig(
    val endActions: ImmutableList<TangemTopBarActionUM> = persistentListOf(),
    val onAllServicesClick: (() -> Unit)? = null,
) {

    companion object {
        const val ALL_SERVICES_SHARED_TRANSITION_KEY = "wallet_top_bar_all_services"
    }
}