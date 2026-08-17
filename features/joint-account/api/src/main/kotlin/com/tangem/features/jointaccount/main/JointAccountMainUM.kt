package com.tangem.features.jointaccount.main

import androidx.compose.runtime.Immutable
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
     * Not yet activated — waiting for members to join. Renders as a normal account row with a
     * member-collection subtitle (e.g. "1 of 5 members") and a zero balance; no token list. The
     * creator additionally sees an "Invite members to join" banner.
     *
     * @property title         account name shown as the row title.
     * @property subtitle      member-collection progress, e.g. "1 of 5 members".
     * @property icon          account icon (creator-chosen crypto-portfolio icon + color).
     * @property balance       total fiat balance (zero before activation).
     * @property priceChange   price-change indicator under the balance; `null` hides it.
     * @property canInvite     `true` only for the creator — renders the invite banner.
     * @property onClick       tap on the row → members screen.
     * @property onInviteClick tap on the invite banner → invite flow.
     */
    data class WaitingMembers(
        val title: TextReference,
        val subtitle: TextReference,
        val icon: AccountIconUM,
        val balance: TextReference,
        val priceChange: TangemPriceChange.State?,
        val canInvite: Boolean,
        val onClick: () -> Unit,
        val onInviteClick: () -> Unit,
    ) : JointAccountMainUM

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