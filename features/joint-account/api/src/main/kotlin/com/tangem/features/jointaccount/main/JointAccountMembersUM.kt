package com.tangem.features.jointaccount.main

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

/**
 * UI model for the joint-account "Invite members" / "Members" screen.
 *
 * Which fields are populated depends entirely on the account's own data — its `Status` and whether this
 * device holds its invites — not on how the screen was opened. There is no "mode": hiding an action means
 * simply not wiring it here for the current account state.
 *
 * @property topBar          which affordance leaves the screen and what it shows — see [TopBarUM].
 * @property header          the in-content title/progress block; `null` once the account is active — its
 * title moves into [topBar] instead and there is nothing left to collect.
 * @property shareSafely     "Share invites safely" info block; tapping opens the info sheet. `null` once the
 * account is active, or when this device isn't the creator — only the creator shares invites.
 * @property joined          members who have already joined, in display order.
 * @property freeSlots       slots still waiting to be filled, in display order.
 * @property otherMembersLabel section label shown above [freeSlots]; `null` hides it (no free slots).
 * @property onActivateClick the "Activate account" call to action pinned to the bottom of the screen —
 * creator only, shown when every slot is filled; `null` hides it (not the creator, the composition is not
 * full yet, or the account is already active). The confirmation sheet it opens is hosted as its own child
 * slot, not part of this state — see `JointAccountMembersModel.activationConfirmationNavigation`.
 */
@Immutable
data class JointAccountMembersUM(
    val topBar: TopBarUM,
    val header: HeaderUM?,
    val shareSafely: ShareSafelyUM?,
    val joined: ImmutableList<MemberUM.Joined>,
    val freeSlots: ImmutableList<MemberUM.FreeSlot>,
    val otherMembersLabel: TextReference?,
    val onActivateClick: (() -> Unit)?,
) {

    /**
     * The top bar's affordance for leaving the screen. The two shapes are mutually exclusive by design —
     * archiving is never offered once the account is active, so that state simply carries no such field.
     */
    @Immutable
    sealed interface TopBarUM {

        /**
         * Top-right close "X" — dismisses the working (collecting/activating) view.
         *
         * @property canArchive     whether the three-dot menu with the destructive "Archive account" action
         * is shown.
         * @property onArchiveClick archive the account.
         * @property onCloseClick   leave the screen and return to the wallet.
         */
        data class Dismissible(
            val canArchive: Boolean,
            val onArchiveClick: () -> Unit,
            val onCloseClick: () -> Unit,
        ) : TopBarUM

        /**
         * Centered title with a top-left back arrow — the account is active, this is now a plain read-only
         * roster.
         *
         * @property title        screen title ("Members").
         * @property onBackClick  leave the screen and return to the wallet.
         */
        data class Titled(
            val title: TextReference,
            val onBackClick: () -> Unit,
        ) : TopBarUM
    }

    /**
     * The in-content title/progress block shown above the member list.
     *
     * @property title    screen title ("Invite members" while collecting).
     * @property progress collection progress, e.g. "1 of 5 joined, including you".
     */
    data class HeaderUM(
        val title: TextReference,
        val progress: TextReference,
    )

    /**
     * "Share invites safely" info block.
     *
     * @property title       block headline.
     * @property description supporting line explaining that anyone with the link can join and sign.
     * @property onClick     open the "Sharing safely" info sheet.
     */
    data class ShareSafelyUM(
        val title: TextReference,
        val description: TextReference,
        val onClick: () -> Unit,
    )

    /**
     * Avatar for a joined member: a colored circle with the member's initial.
     *
     * @property monogram single-letter initial rendered inside the circle.
     * @property color    circle color, reusing the account-icon color palette.
     */
    data class MemberAvatarUM(
        val monogram: String,
        val color: CryptoPortfolioIcon.Color,
    )

    @Immutable
    sealed interface MemberUM {

        val id: String

        /**
         * A member who has already joined.
         *
         * @property avatar      colored monogram avatar.
         * @property name        member's display name.
         * @property role        role label: "You • Creator", "You" or "Joined".
         * @property onInfoClick open the member card with the full owner address.
         */
        data class Joined(
            override val id: String,
            val avatar: MemberAvatarUM,
            val name: TextReference,
            val role: TextReference,
            val onInfoClick: () -> Unit,
        ) : MemberUM

        /**
         * A free slot waiting to be filled.
         *
         * @property title         slot title ("Member").
         * @property status        slot status ("Not invited").
         * @property canInvite     whether the "Invite" button is shown (creator only, working view only).
         * @property onInviteClick share this slot's invite link via the system share sheet.
         */
        data class FreeSlot(
            override val id: String,
            val title: TextReference,
            val status: TextReference,
            val canInvite: Boolean,
            val onInviteClick: () -> Unit,
        ) : MemberUM
    }
}