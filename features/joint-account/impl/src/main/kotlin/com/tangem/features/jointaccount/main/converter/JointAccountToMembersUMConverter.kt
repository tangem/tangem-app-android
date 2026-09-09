package com.tangem.features.jointaccount.main.converter

import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Maps a [JointAccount] (plus the invites this device holds) to [JointAccountMembersUM].
 *
 * Every visual difference on the screen — title, progress line, the "Share invites safely" banner, the
 * archive menu, the activation CTA — is driven by two things only: [JointAccount.status] and whether this
 * device holds the account's invites (only the creating device ever does — see [MembersScreenPhase]). There
 * is no separate "mode": the wallet row, account settings and the deep link all push the same route, and
 * whichever phase the account's own data resolves to is what renders, regardless of entry point.
 */
internal class JointAccountToMembersUMConverter(
    private val callbacks: Callbacks,
) : Converter<JointAccountToMembersUMConverter.Input, JointAccountMembersUM> {

    data class Input(val account: JointAccount, val invites: List<String>)

    /** Every side effect the converted state can trigger. */
    data class Callbacks(
        val onMemberInfoClick: (
            avatar: JointAccountMembersUM.MemberAvatarUM,
            name: TextReference,
            address: String,
        ) -> Unit,
        val onInviteClick: (inviteId: String) -> Unit,
        val onActivateClick: (cryptoAccountId: String) -> Unit,
        val onShareSafelyClick: () -> Unit,
        val onArchiveClick: (cryptoAccountId: String) -> Unit,
        val onCloseClick: () -> Unit,
    )

    override fun convert(value: Input): JointAccountMembersUM {
        val (account, invites) = value
        val phase = account.status.toPhase(isThisDeviceCreator = invites.isNotEmpty())

        val joined = account.members.mapIndexed { index, member ->
            joinedMember(index = index, member = member, phase = phase)
        }
        val redeemedInvites = account.members
            .mapNotNull { member -> member.inviteId?.lowercase() }
            .toSet()

        val remainingInvites = invites
            .filterNot { invite -> invite.lowercase() in redeemedInvites }

        val freeSlots = List(size = (account.membersCount - account.members.size).coerceAtLeast(minimumValue = 0)) {
            freeSlot(index = it, invite = remainingInvites.getOrNull(it), phase = phase)
        }

        return JointAccountMembersUM(
            topBar = topBar(phase = phase, cryptoAccountId = account.cryptoAccountId),
            header = header(phase = phase, joined = account.members.size, total = account.membersCount),
            shareSafely = shareSafely(phase),
            joined = joined.toImmutableList(),
            freeSlots = freeSlots.toImmutableList(),
            otherMembersLabel = if (freeSlots.isNotEmpty()) {
                resourceReference(CoreUiR.string.joint_account_invite_members_other_members)
            } else {
                null
            },
            onActivateClick = onActivateClick(phase = phase, cryptoAccountId = account.cryptoAccountId),
        )
    }

    private fun topBar(phase: MembersScreenPhase, cryptoAccountId: String): JointAccountMembersUM.TopBarUM =
        if (phase.stage == MembersScreenPhase.Stage.Completed) {
            JointAccountMembersUM.TopBarUM.Titled(title = title(phase), onBackClick = callbacks.onCloseClick)
        } else {
            JointAccountMembersUM.TopBarUM.Dismissible(
                canArchive = phase.isThisDeviceCreator,
                onArchiveClick = { callbacks.onArchiveClick(cryptoAccountId) },
                onCloseClick = callbacks.onCloseClick,
            )
        }

    private fun title(phase: MembersScreenPhase): TextReference = when {
        phase.stage == MembersScreenPhase.Stage.Completed ->
            resourceReference(CoreUiR.string.joint_account_members_title)
        phase.isThisDeviceCreator -> resourceReference(CoreUiR.string.joint_account_invite_members_title)
        else -> resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title)
    }

    private fun header(phase: MembersScreenPhase, joined: Int, total: Int): JointAccountMembersUM.HeaderUM? {
        if (phase.stage == MembersScreenPhase.Stage.Completed) return null

        return JointAccountMembersUM.HeaderUM(
            title = title(phase),
            progress = resourceReference(
                id = CoreUiR.string.joint_account_invite_members_subtitle,
                formatArgs = wrappedList(joined, total),
            ),
        )
    }

    private fun shareSafely(phase: MembersScreenPhase): JointAccountMembersUM.ShareSafelyUM? {
        if (phase.stage != MembersScreenPhase.Stage.Collecting || !phase.isThisDeviceCreator) return null

        return JointAccountMembersUM.ShareSafelyUM(
            title = resourceReference(CoreUiR.string.joint_account_invite_members_safety_title),
            description = resourceReference(CoreUiR.string.joint_account_invite_members_safety_subtitle),
            onClick = callbacks.onShareSafelyClick,
        )
    }

    private fun onActivateClick(phase: MembersScreenPhase, cryptoAccountId: String): (() -> Unit)? {
        if (phase.stage != MembersScreenPhase.Stage.ReadyToActivate || !phase.isThisDeviceCreator) return null

        return { callbacks.onActivateClick(cryptoAccountId) }
    }

    private fun joinedMember(
        index: Int,
        member: JointAccount.Member,
        phase: MembersScreenPhase,
    ): JointAccountMembersUM.MemberUM.Joined {
        val avatar = avatar(member = member, index = index)
        val name = stringReference(member.name)

        return JointAccountMembersUM.MemberUM.Joined(
            id = member.address,
            avatar = avatar,
            name = name,
            role = role(member = member, isThisDeviceCreator = phase.isThisDeviceCreator),
            onInfoClick = { callbacks.onMemberInfoClick(avatar, name, member.address) },
        )
    }

    private fun role(member: JointAccount.Member, isThisDeviceCreator: Boolean): TextReference {
        val isCreator = member.role == JointAccount.Role.CREATOR

        return when {
            isCreator && isThisDeviceCreator -> combinedReference(
                resourceReference(CoreUiR.string.joint_account_members_role_you),
                stringReference(" ${StringsSigns.DOT} "),
                resourceReference(CoreUiR.string.joint_account_invite_members_creator),
            )
            isCreator -> resourceReference(CoreUiR.string.joint_account_invite_members_creator)
            else -> resourceReference(CoreUiR.string.joint_account_members_role_joined)
        }
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
        invite: String?,
        phase: MembersScreenPhase,
    ): JointAccountMembersUM.MemberUM.FreeSlot {
        return JointAccountMembersUM.MemberUM.FreeSlot(
            id = "slot_$index",
            title = resourceReference(CoreUiR.string.joint_account_invite_members_member),
            status = resourceReference(CoreUiR.string.joint_account_invite_members_not_invited),
            canInvite = phase.stage != MembersScreenPhase.Stage.Completed &&
                phase.isThisDeviceCreator &&
                invite != null,
            onInviteClick = { invite?.let(callbacks.onInviteClick) },
        )
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