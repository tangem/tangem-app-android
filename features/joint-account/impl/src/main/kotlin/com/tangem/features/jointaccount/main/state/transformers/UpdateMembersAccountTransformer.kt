package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.entity.MemberCardConfig
import com.tangem.utils.StringsSigns
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

/**
 * Projects the domain [account] and the [invites] this device holds onto the screen: the joined members, the
 * free slots with their invite actions, and the activation call to action. Only the creating device ever gets
 * invites, so holding them is what makes this device the creator's.
 */
internal class UpdateMembersAccountTransformer(
    private val account: JointAccount,
    private val invites: List<String>,
    private val isInviteMode: Boolean,
    private val onShareInviteClick: (String) -> Unit,
    private val onMemberInfoClick: (MemberCardConfig) -> Unit,
    private val onActivateClick: (String) -> Unit,
) : Transformer<JointAccountMembersUM> {

    private val isThisDeviceCreator = invites.isNotEmpty()

    override fun transform(prevState: JointAccountMembersUM): JointAccountMembersUM {
        val joined = account.members.mapIndexed(::joinedMember)
        val redeemedInvites = account.members
            .mapNotNull { member -> member.inviteId?.lowercase() }
            .toSet()
        val remainingInvites = invites.filterNot { invite -> invite.lowercase() in redeemedInvites }
        val freeSlots = List(size = (account.membersCount - account.members.size).coerceAtLeast(minimumValue = 0)) {
            freeSlot(index = it, invite = remainingInvites.getOrNull(it))
        }

        return prevState.copy(
            progress = resourceReference(
                id = if (isInviteMode) {
                    CoreUiR.string.joint_account_invite_members_subtitle
                } else {
                    CoreUiR.string.joint_account_members_progress
                },
                formatArgs = wrappedList(account.members.size, account.membersCount),
            ),
            members = (joined + freeSlots).toImmutableList(),
            otherMembersLabel = if (freeSlots.isNotEmpty()) {
                resourceReference(CoreUiR.string.joint_account_invite_members_other_members)
            } else {
                null
            },
            canArchive = isInviteMode,
            activation = activation(),
        )
    }

    private fun joinedMember(index: Int, member: JointAccount.Member): JointAccountMembersUM.MemberUM.Joined {
        val avatar = avatar(member = member, index = index)
        val name = stringReference(member.name)

        return JointAccountMembersUM.MemberUM.Joined(
            id = member.address,
            avatar = avatar,
            name = name,
            role = role(member = member),
            onInfoClick = {
                onMemberInfoClick(MemberCardConfig(avatar = avatar, name = name, address = member.address))
            },
        )
    }

    private fun role(member: JointAccount.Member): TextReference {
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

    private fun freeSlot(index: Int, invite: String?): JointAccountMembersUM.MemberUM.FreeSlot {
        return JointAccountMembersUM.MemberUM.FreeSlot(
            id = "slot_$index",
            title = resourceReference(CoreUiR.string.joint_account_invite_members_new_member),
            status = resourceReference(CoreUiR.string.joint_account_invite_members_not_invited),
            canInvite = isInviteMode && invite != null,
            onInviteClick = { invite?.let(onShareInviteClick) },
        )
    }

    private fun activation(): JointAccountMembersUM.ActivationUM? {
        val canActivate = isInviteMode && isThisDeviceCreator && account.status == JointAccount.Status.CONFIRMING

        if (!canActivate) return null

        return JointAccountMembersUM.ActivationUM(
            onActivateClick = { onActivateClick(account.cryptoAccountId) },
            confirmation = null,
        )
    }
}