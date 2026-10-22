package com.tangem.features.jointaccount.main

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference

/** The joint-account row of the wallet account list */
@Immutable
sealed interface JointAccountMainUM {

    data object Empty : JointAccountMainUM

    data object Loading : JointAccountMainUM

    /**
     * Not activated yet: a normal row with a progress subtitle and a zero balance, no token list.
     *
     * @property banner the next step under the row; `null` while there is nothing for this device to do
     */
    data class WaitingMembers(
        val title: TextReference,
        val subtitle: TextReference,
        val icon: AccountIconUM,
        val balance: TextReference,
        val priceChange: TangemPriceChange.State?,
        val banner: BannerUM?,
        val onClick: () -> Unit,
    ) : JointAccountMainUM

    data class BannerUM(
        val text: TextReference,
        val icon: ImageVector,
        val onClick: () -> Unit,
    )

    /** The composition does not verify: a dimmed, non-expandable row whose tap shows an alert */
    data class Blocked(
        val title: TextReference,
        val subtitle: TextReference,
        val icon: AccountIconUM,
        val balance: TextReference,
        val priceChange: TangemPriceChange.State?,
        val onClick: () -> Unit,
    ) : JointAccountMainUM

    /** Transient backend error: a disabled, non-interactive row */
    data object TemporaryUnavailable : JointAccountMainUM
}