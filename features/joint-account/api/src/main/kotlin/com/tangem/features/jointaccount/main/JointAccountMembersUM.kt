package com.tangem.features.jointaccount.main

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
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
 * @property header          the in-content title; `null` once the account is active — its title moves into
 * [topBar] instead.
 * @property summaryCard     the account's name, icon, joining progress and approval rule; `null` until the
 * account's own record (name and icon) arrives.
 * @property joined          members who have already joined, in display order.
 * @property freeSlots       slots still waiting to be filled, in display order.
 * @property membersLabel    section label shown above [joined]; `null` hides it (nobody joined).
 * @property waitingLabel    section label shown above [freeSlots]; `null` hides it (no free slots).
 * @property footer        the "Verify account" footer pinned to the bottom of the screen; `null` once the
 * account is active — see [FooterUM].
 */
@Immutable
data class JointAccountMembersUM(
    val topBar: TopBarUM,
    val header: HeaderUM?,
    val summaryCard: SummaryCardUM?,
    val joined: ImmutableList<MemberUM.Joined>,
    val freeSlots: ImmutableList<MemberUM.FreeSlot>,
    val membersLabel: TextReference?,
    val waitingLabel: TextReference?,
    val footer: FooterUM?,
) {

    /** The top bar's affordance for leaving the screen. */
    @Immutable
    sealed interface TopBarUM {

        /**
         * Top-right close "X" — dismisses the working (collecting/activating) view.
         *
         * @property onCloseClick leave the screen and return to the wallet.
         */
        data class Dismissible(
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
     * The in-content title shown above the member list.
     *
     * @property title screen title ("Invite members" while collecting).
     */
    data class HeaderUM(
        val title: TextReference,
    )

    /**
     * The account summary card above the member list.
     *
     * @property icon                 account icon, the same as on the wallet screen.
     * @property name                 account name.
     * @property progress             joining progress, e.g. "1 of 5 members joined".
     * @property approvals            approval rule, e.g. "Any 2 of 5".
     * @property onApprovalsInfoClick explain how the approval rule works.
     */
    data class SummaryCardUM(
        val icon: AccountIconUM.CryptoPortfolio,
        val name: TextReference,
        val progress: TextReference,
        val approvals: TextReference,
        val onApprovalsInfoClick: () -> Unit,
    )

    /**
     * The footer pinned to the bottom of the screen.
     *
     * @property hint   how many members still need to join, shown above [button]; `null` hides it.
     * @property button the "Verify account" button.
     */
    data class FooterUM(
        val hint: TextReference?,
        val button: VerifyButtonUM,
    )

    /**
     * "Verify account" button.
     *
     * @property isEnabled     the button can be pressed — only the creator, once every slot is filled.
     * @property isLoading     the activation is being signed and sent.
     * @property walletIconRes wallet-interaction icon at the end of the button — a card for a cold wallet, which
     * signs with a tap; `null` for a hot wallet.
     * @property onClick       sign and activate the account.
     */
    data class VerifyButtonUM(
        val isEnabled: Boolean,
        val isLoading: Boolean,
        val walletIconRes: Int?,
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
         * @property isYou       this wallet is the member — a "You" chip follows the name.

         * @property status      membership status ("Joined").
         * @property onInfoClick open the member card with the full owner address.
         */
        data class Joined(
            override val id: String,
            val avatar: MemberAvatarUM,
            val name: TextReference,
            val isYou: Boolean,
            val isCreator: Boolean,
            val status: TextReference,
            val onInfoClick: () -> Unit,
        ) : MemberUM

        /**
         * A free slot waiting to be filled.
         *
         * @property title  slot title ("Member 2").
         * @property status slot status ("Not invited").
         * @property action the button the slot offers; `null` shows none (not the creator, or nothing to share).
         */
        data class FreeSlot(
            override val id: String,
            val title: TextReference,
            val status: TextReference,
            val action: SlotActionUM?,
        ) : MemberUM
    }

    /** The button of a free slot. */
    @Immutable
    sealed interface SlotActionUM {

        val onClick: () -> Unit

        /** "Invite" — share this slot's invite link via the system share sheet. */
        data class Invite(override val onClick: () -> Unit) : SlotActionUM

        /** "Resend" — share this slot's invite link again. */
        data class Resend(override val onClick: () -> Unit) : SlotActionUM
    }
}