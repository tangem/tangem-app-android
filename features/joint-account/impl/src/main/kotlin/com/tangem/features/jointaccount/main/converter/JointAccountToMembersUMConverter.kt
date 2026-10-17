package com.tangem.features.jointaccount.main.converter

import com.tangem.common.ui.account.CryptoPortfolioIconConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Maps a [JointAccount] (plus the invites this device holds) to [JointAccountMembersUM].
 *
 * Every visual difference on the screen — title, top bar, the activation CTA — is driven by two things only:
 * [JointAccount.status] and whether this wallet is the account's creator (see [MembersScreenPhase]). There is
 * no separate "mode": the wallet row, account settings and the deep link all push the same route, and whichever
 * phase the account's own data resolves to is what renders, regardless of entry point.
 *
 * The wallet recognises itself among the members by [Input.ownAddress], the owner address derived from the keys
 * on the device: the backend lists every member the same way for everyone and marks nobody as "you".
 */
internal class JointAccountToMembersUMConverter(
    private val callbacks: Callbacks,
) : Converter<JointAccountToMembersUMConverter.Input, JointAccountMembersUM> {

    /**
     * @property invites       the invites this device holds; only the creating device ever receives them
     * @property sharedInvites the [invites] already handed to the share sheet — their slots read "Invited"
     * @property ownAddress    this wallet's owner address in the account, null while its key is not derived yet
     * @property walletIconRes wallet-interaction icon of the activate button, null for a hot wallet
     * @property isActivating  the activation is being signed and sent
     * @property accountName   the account's name from its own record, null until that record arrives
     * @property accountIcon   the account's icon from its own record, null until that record arrives
     */
    data class Input(
        val account: JointAccount,
        val invites: List<String>,
        val sharedInvites: Set<String>,
        val ownAddress: String?,
        val walletIconRes: Int? = null,
        val isActivating: Boolean = false,
        val accountName: AccountName? = null,
        val accountIcon: CryptoPortfolioIcon? = null,
    )

    /** Every side effect the converted state can trigger. */
    data class Callbacks(
        val onMemberInfoClick: (
            avatar: JointAccountMembersUM.MemberAvatarUM,
            name: TextReference,
            address: String,
        ) -> Unit,
        val onInviteClick: (inviteId: String) -> Unit,
        val onActivateClick: (cryptoAccountId: String) -> Unit,
        val onApprovalsInfoClick: (threshold: Int, total: Int) -> Unit,
        val onCloseClick: () -> Unit,
    )

    override fun convert(value: Input): JointAccountMembersUM {
        val account = value.account
        val invites = value.invites
        val ownAddress = value.ownAddress
        val phase = account.status.toPhase(
            isThisDeviceCreator = account.isThisDeviceCreator(ownAddress = ownAddress, invites = invites),
        )

        val joined = account.members.mapIndexed { index, member ->
            joinedMember(
                index = index,
                member = member,
                isOwn = member.address.equals(ownAddress, ignoreCase = true),
            )
        }
        val redeemedInvites = account.members
            .mapNotNull { member -> member.inviteId?.lowercase() }
            .toSet()

        val remainingInvites = invites
            .filterNot { invite -> invite.lowercase() in redeemedInvites }

        val freeSlotsCount = (account.membersCount - account.members.size).coerceAtLeast(minimumValue = 0)
        val freeSlots = List(size = freeSlotsCount) { index ->
            val invite = remainingInvites.getOrNull(index)
            freeSlot(
                index = index,
                number = account.members.size + index,
                invite = invite,
                isInvited = invite in value.sharedInvites,
                phase = phase,
            )
        }

        return JointAccountMembersUM(
            topBar = topBar(phase = phase),
            header = header(phase = phase),
            summaryCard = summaryCard(input = value),
            joined = joined.toImmutableList(),
            freeSlots = freeSlots.toImmutableList(),
            membersLabel = if (joined.isNotEmpty()) {
                resourceReference(CoreUiR.string.joint_account_members_title)
            } else {
                null
            },
            waitingLabel = if (freeSlots.isNotEmpty()) {
                resourceReference(CoreUiR.string.joint_account_invite_members_waiting_to_join)
            } else {
                null
            },
            footer = footer(phase = phase, input = value),
        )
    }

    private fun topBar(phase: MembersScreenPhase): JointAccountMembersUM.TopBarUM =
        if (phase.stage == MembersScreenPhase.Stage.Completed) {
            JointAccountMembersUM.TopBarUM.Titled(title = title(phase), onBackClick = callbacks.onCloseClick)
        } else {
            JointAccountMembersUM.TopBarUM.Dismissible(onCloseClick = callbacks.onCloseClick)
        }

    private fun title(phase: MembersScreenPhase): TextReference = when {
        phase.stage == MembersScreenPhase.Stage.Completed ->
            resourceReference(CoreUiR.string.joint_account_members_title)
        phase.isThisDeviceCreator -> resourceReference(CoreUiR.string.joint_account_invite_members_title)
        else -> resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title)
    }

    private fun header(phase: MembersScreenPhase): JointAccountMembersUM.HeaderUM? {
        if (phase.stage == MembersScreenPhase.Stage.Completed) return null

        return JointAccountMembersUM.HeaderUM(title = title(phase))
    }

    private fun summaryCard(input: Input): JointAccountMembersUM.SummaryCardUM? {
        val name = input.accountName ?: return null
        val icon = input.accountIcon ?: return null
        val account = input.account

        return JointAccountMembersUM.SummaryCardUM(
            icon = CryptoPortfolioIconConverter.convert(icon),
            name = name.toUM().value,
            progress = resourceReference(
                id = CoreUiR.string.joint_account_invite_members_joined_progress,
                formatArgs = wrappedList(account.members.size, account.membersCount),
            ),
            approvals = resourceReference(
                id = CoreUiR.string.joint_account_invite_members_approvals_value,
                formatArgs = wrappedList(account.threshold, account.membersCount),
            ),
            onApprovalsInfoClick = { callbacks.onApprovalsInfoClick(account.threshold, account.membersCount) },
        )
    }

    /** "Verify account" runs the activation until the verification flow exists. */
    private fun footer(phase: MembersScreenPhase, input: Input): JointAccountMembersUM.FooterUM? {
        if (phase.stage == MembersScreenPhase.Stage.Completed) return null

        val account = input.account
        val missingMembers = account.membersCount - account.members.size
        val hint = if (phase.stage == MembersScreenPhase.Stage.Collecting && missingMembers > 0) {
            pluralReference(
                id = CoreUiR.plurals.joint_account_invite_members_still_need_to_join,
                count = missingMembers,
                formatArgs = wrappedList(missingMembers),
            )
        } else {
            null
        }

        return JointAccountMembersUM.FooterUM(
            hint = hint,
            button = JointAccountMembersUM.VerifyButtonUM(
                isEnabled = phase.stage == MembersScreenPhase.Stage.ReadyToActivate && phase.isThisDeviceCreator,
                isLoading = input.isActivating,
                walletIconRes = input.walletIconRes,
                onClick = { callbacks.onActivateClick(account.cryptoAccountId) },
            ),
        )
    }

    private fun joinedMember(
        index: Int,
        member: JointAccount.Member,
        isOwn: Boolean,
    ): JointAccountMembersUM.MemberUM.Joined {
        val avatar = avatar(member = member, index = index)
        val name = stringReference(member.name)

        return JointAccountMembersUM.MemberUM.Joined(
            id = member.address,
            avatar = avatar,
            name = name,
            isYou = isOwn,
            isCreator = member.role == JointAccount.Role.CREATOR,
            status = resourceReference(CoreUiR.string.joint_account_members_role_joined),
            onInfoClick = { callbacks.onMemberInfoClick(avatar, name, member.address) },
        )
    }

    private fun avatar(member: JointAccount.Member, index: Int): JointAccountMembersUM.MemberAvatarUM {
        val colors = CryptoPortfolioIcon.Color.entries

        return JointAccountMembersUM.MemberAvatarUM(
            monogram = member.name.take(n = 1).uppercase(),
            color = colors[index % colors.size],
        )
    }

    private fun freeSlot(
        index: Int,
        number: Int,
        invite: String?,
        isInvited: Boolean,
        phase: MembersScreenPhase,
    ): JointAccountMembersUM.MemberUM.FreeSlot {
        val status = if (isInvited) {
            CoreUiR.string.joint_account_invite_members_invited
        } else {
            CoreUiR.string.joint_account_invite_members_not_invited
        }
        return JointAccountMembersUM.MemberUM.FreeSlot(
            id = "slot_$index",
            title = resourceReference(
                id = CoreUiR.string.joint_account_invite_members_member_number,
                formatArgs = wrappedList(number),
            ),
            status = resourceReference(status),
            action = slotAction(invite = invite, phase = phase),
        )
    }

    /**
     * Always [JointAccountMembersUM.SlotActionUM.Invite]: offering Resend needs to know that a shared link expired,
     * and neither the backend nor the local invite store tracks that yet.
     */
    private fun slotAction(invite: String?, phase: MembersScreenPhase): JointAccountMembersUM.SlotActionUM? {
        val canInvite = phase.stage != MembersScreenPhase.Stage.Completed && phase.isThisDeviceCreator
        if (!canInvite || invite == null) return null

        return JointAccountMembersUM.SlotActionUM.Invite(onClick = { callbacks.onInviteClick(invite) })
    }
}

/**
 * Where the account's composition stands, driving every visual difference on the screen. There is no
 * "reference mode" — every device lands in [Stage.Completed] once the account activates, regardless of how
 * the screen was opened.
 */
private data class MembersScreenPhase(val stage: Stage, val isThisDeviceCreator: Boolean) {

    enum class Stage {
        /** Free slots remain — [JointAccount.Status.PENDING]. */
        Collecting,

        /** Every slot filled, awaiting the creator's on-chain activation — [JointAccount.Status.CONFIRMING]. */
        ReadyToActivate,

        /**
         * The account is live: a plain read-only roster, same for the creator and every other member —
         * [JointAccount.Status.ACTIVE]. [JointAccount.Status.CANCELLED] and [JointAccount.Status.UNKNOWN] have
         * no design of their own yet and fall back here conservatively (read-only, no destructive actions
         * offered).
         */
        Completed,
    }
}

/**
 * The creator is the member whose address is this wallet's own owner address. Until that address is derived, the
 * invites the device holds stand in for it: only the creating device ever receives them.
 */
private fun JointAccount.isThisDeviceCreator(ownAddress: String?, invites: List<String>): Boolean {
    val creator = members.firstOrNull { it.role == JointAccount.Role.CREATOR }

    return if (ownAddress != null && creator != null) {
        creator.address.equals(ownAddress, ignoreCase = true)
    } else {
        invites.isNotEmpty()
    }
}

private fun JointAccount.Status.toPhase(isThisDeviceCreator: Boolean): MembersScreenPhase {
    val stage = when (this) {
        JointAccount.Status.PENDING -> MembersScreenPhase.Stage.Collecting
        JointAccount.Status.CONFIRMING -> MembersScreenPhase.Stage.ReadyToActivate
        JointAccount.Status.ACTIVE,
        JointAccount.Status.CANCELLED,
        JointAccount.Status.UNKNOWN,
        -> MembersScreenPhase.Stage.Completed
    }
    return MembersScreenPhase(stage = stage, isThisDeviceCreator = isThisDeviceCreator)
}