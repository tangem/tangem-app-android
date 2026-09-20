package com.tangem.features.jointaccount.main

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference

/**
 * UI model for the joint-account row shown in the wallet account list.
 *
 * Full UI parity with a crypto-account row (icon, title, subtitle, fiat balance and price change),
 * but built on the new design system (DS3: `TangemRow`, `colors3` / `typography3`). Joint-specific
 * differences are limited to the subtitle text and the extra content below the row (invite banner /
 * blocked warning).
 */
@Immutable
sealed interface JointAccountMainUM {

    /** Nothing to render (e.g. joint accounts unavailable for this wallet). */
    data object Empty : JointAccountMainUM

    /** First-time fetch: shimmer placeholder row. */
    data object Loading : JointAccountMainUM

    /**
     * Not yet activated — waiting for members to join or for the creator to activate. Renders as a normal
     * account row with a progress subtitle (e.g. "1 of 5 members", "Waiting for activation") and a zero
     * balance; no token list. The creator additionally sees the next-step [banner].
     *
     * @property title       account name shown as the row title.
     * @property subtitle    member-collection progress, e.g. "1 of 5 members".
     * @property icon        account icon (creator-chosen crypto-portfolio icon + color).
     * @property balance     total fiat balance (zero before activation).
     * @property priceChange price-change indicator under the balance; `null` hides it.
     * @property banner      the creator's next step under the row — invite more members while slots are free,
     * activate once the account is confirmed ready; `null` for a member and while there is nothing to do.
     * @property onClick     tap on the row → members screen.
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

    /** The call-to-action strip under a [WaitingMembers] row */
    data class BannerUM(
        val text: TextReference,
        val icon: ImageVector,
        val onClick: () -> Unit,
    )

    /**
     * Blocked — the account stays visible with its balance, but operations are unavailable and a
     * permanent warning is shown.
     *
     * @property title       account name.
     * @property subtitle    token and member counts.
     * @property icon        account icon.
     * @property balance     total fiat balance of the account.
     * @property priceChange price-change indicator under the balance; `null` hides it.
     * @property warning     permanent warning message rendered below the row.
     * @property onClick     tap on the row.
     */
    data class Blocked(
        val title: TextReference,
        val subtitle: TextReference,
        val icon: AccountIconUM,
        val balance: TextReference,
        val priceChange: TangemPriceChange.State?,
        val warning: TextReference,
        val onClick: () -> Unit,
    ) : JointAccountMainUM

    /** Transient backend error (unavailable / not synced): disabled, non-interactive row. */
    data object TemporaryUnavailable : JointAccountMainUM
}