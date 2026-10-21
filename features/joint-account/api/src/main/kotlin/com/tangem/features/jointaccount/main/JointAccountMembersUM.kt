package com.tangem.features.jointaccount.main

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

/**
 * UI model for the joint-account "Invite members" / "Confirm the account" / "Members" screen.
 *
 * Which fields are populated depends entirely on the account's own data — its status, whether this device holds
 * its invites and whether this wallet has confirmed the account — not on how the screen was opened.
 *
 * @property header      `null` once the account is active — its title moves into [topBar].
 * @property summaryCard `null` until the account's own record (name and icon) arrives.
 * @property footer      `null` once the account is active.
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

    @Immutable
    sealed interface TopBarUM {

        /** Top-right close "X" of the collecting / confirming view. */
        data class Dismissible(
            val onCloseClick: () -> Unit,
        ) : TopBarUM

        /** Centered title with a back arrow — the read-only roster of an active account. */
        data class Titled(
            val title: TextReference,
            val onBackClick: () -> Unit,
        ) : TopBarUM
    }

    /** The in-content title above the member list; only the confirming view has a [subtitle]. */
    data class HeaderUM(
        val title: TextReference,
        val subtitle: TextReference? = null,
    )

    /** The account summary card: [membersCount] is "5 members", [approvals] is "Any 2 of 5". */
    data class SummaryCardUM(
        val icon: AccountIconUM.CryptoPortfolio,
        val name: TextReference,
        val membersCount: TextReference,
        val approvals: TextReference,
        val onApprovalsInfoClick: () -> Unit,
    )

    /** [button] is `null` once this wallet has confirmed the account — only the [hint] remains. */
    data class FooterUM(
        val hint: TextReference?,
        val button: ConfirmButtonUM?,
    )

    /** "Confirm shared address" button. */
    data class ConfirmButtonUM(
        val isEnabled: Boolean,
        val onClick: () -> Unit,
    )

    /** A colored circle with the member's initial, reusing the account-icon palette. */
    data class MemberAvatarUM(
        val monogram: String,
        val color: CryptoPortfolioIcon.Color,
    )

    @Immutable
    sealed interface MemberUM {

        val id: String

        /** A member who has already joined; [onInfoClick] opens the member card with the full owner address. */
        data class Joined(
            override val id: String,
            val avatar: MemberAvatarUM,
            val name: TextReference,
            val isYou: Boolean,
            val isCreator: Boolean,
            val status: MemberStatusUM,
            val onInfoClick: () -> Unit,
        ) : MemberUM

        /** A free slot waiting to be filled; [action] is `null` when this device cannot invite. */
        data class FreeSlot(
            override val id: String,
            val title: TextReference,
            val status: SlotStatusUM,
            val action: SlotActionUM?,
        ) : MemberUM
    }

    /** The status badge of a joined member: "Joined" while collecting, the confirmation state while confirming. */
    enum class MemberStatusUM(val text: TextReference, val badgeStatus: TangemBadge.Status) {
        Joined(
            text = TextReference.Res(R.string.joint_account_members_role_joined),
            badgeStatus = TangemBadge.Status.Info,
        ),
        NotConfirmed(
            text = TextReference.Res(R.string.joint_account_members_status_not_confirmed),
            badgeStatus = TangemBadge.Status.Neutral,
        ),
        Confirmed(
            text = TextReference.Res(R.string.joint_account_members_status_confirmed),
            badgeStatus = TangemBadge.Status.Info,
        ),
    }

    @Immutable
    sealed interface SlotStatusUM {

        data object NotInvited : SlotStatusUM

        /** [label] is "Invited", or the invite's deadline when the backend reports one ("Until tomorrow, 13:15"). */
        data class Invited(val label: TextReference) : SlotStatusUM

        data object Expired : SlotStatusUM
    }

    @Immutable
    sealed interface SlotActionUM {

        data class Invite(val onClick: () -> Unit) : SlotActionUM

        data class ReInvite(val onClick: () -> Unit) : SlotActionUM

        /** A new invite is being issued for this slot. */
        data object Loading : SlotActionUM
    }
}