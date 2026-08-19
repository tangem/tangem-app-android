package com.tangem.features.jointaccount.main

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

/**
 * UI model for the joint-account "Invite members" / "Members" screen.
 *
 * A single model powers two modes: the creator's working invite view (with per-slot invite actions
 * and the archive menu) and the reference view opened from account settings (the same member list
 * without the invite/activate actions). The mode is decided by whoever builds this state, not by the
 * screen: hiding an action means simply not wiring it here.
 *
 * @property title           screen title ("Invite members" in the working view, "Members" in the reference view).
 * @property progress        collection progress, e.g. "1 of 5 joined, including you".
 * @property shareSafely     permanent "Share invites safely" info block; tapping opens the info sheet.
 * @property members         joined members followed by free slots, in display order.
 * @property otherMembersLabel section label shown above the free slots; `null` hides it (no free slots).
 * @property canArchive      whether the three-dot menu with the destructive "Archive account" action is shown.
 * @property activation      the "Activate account" call to action; `null` hides it (not the creator, the
 * composition is not full yet, or the reference view).
 * @property onArchiveClick  archive the account.
 * @property onCloseClick    close the screen and return to the wallet.
 */
@Immutable
data class JointAccountMembersUM(
    val title: TextReference,
    val progress: TextReference,
    val shareSafely: ShareSafelyUM,
    val members: ImmutableList<MemberUM>,
    val otherMembersLabel: TextReference?,
    val canArchive: Boolean,
    val activation: ActivationUM?,
    val onArchiveClick: () -> Unit,
    val onCloseClick: () -> Unit,
) {

    /**
     * The "Activate account" call to action pinned to the bottom of the screen — creator only, shown when
     * every slot is filled.
     *
     * @property onActivateClick open the activation confirmation sheet.
     * @property confirmation    the confirmation sheet state; `null` while the sheet is hidden.
     */
    @Immutable
    data class ActivationUM(
        val onActivateClick: () -> Unit,
        val confirmation: ConfirmationUM?,
    ) {

        /**
         * The "You're activating a shared account" confirmation sheet.
         *
         * @property onConfirmClick proceed with the activation (leads into the NFC signing session).
         * @property onCancelClick  close the sheet; dismissing it by swipe or the close icon acts the same.
         */
        @Immutable
        data class ConfirmationUM(
            val onConfirmClick: () -> Unit,
            val onCancelClick: () -> Unit,
        )
    }

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