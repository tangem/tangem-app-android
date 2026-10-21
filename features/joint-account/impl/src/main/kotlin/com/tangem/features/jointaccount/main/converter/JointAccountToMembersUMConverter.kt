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
import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.MemberStatusUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.MemberUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.SlotStatusUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Maps a [JointAccount] (plus the invites this device holds) to [JointAccountMembersUM]. Everything on the screen
 * follows [JointAccount.status], whether this wallet is the creator and whether it has confirmed the account
 * (see [MembersScreenPhase]); there is no separate "mode" per entry point.
 *
 * The wallet recognises itself among the members by [Input.ownAddress], derived from the keys on the device: the
 * backend marks nobody as "you".
 */
internal class JointAccountToMembersUMConverter(
    private val callbacks: Callbacks,
) : Converter<JointAccountToMembersUMConverter.Input, JointAccountMembersUM> {

    /**
     * @property invites    the invites this device holds; only the creating device ever receives them
     * @property ownAddress this wallet's owner address in the account, null while its key is not derived yet
     * @property nowMillis  the moment an invite's expiry is judged against
     */
    data class Input(
        val account: JointAccount,
        val invites: List<JointAccountInvite>,
        val sharedInvites: Set<String>,
        val ownAddress: String?,
        val isActivating: Boolean = false,
        val accountName: AccountName? = null,
        val accountIcon: CryptoPortfolioIcon? = null,
        val nowMillis: Long = System.currentTimeMillis(),
    )

    data class Callbacks(
        val onMemberInfoClick: (
            avatar: JointAccountMembersUM.MemberAvatarUM,
            name: TextReference,
            address: String,
        ) -> Unit,
        val onInviteClick: (inviteId: String, accountName: TextReference) -> Unit,
        val onActivateClick: (cryptoAccountId: String) -> Unit,
        val onApprovalsInfoClick: (threshold: Int, total: Int) -> Unit,
        val onCloseClick: () -> Unit,
    )

    private val freeSlotFactory = FreeSlotUMFactory(onInviteClick = callbacks.onInviteClick)

    override fun convert(value: Input): JointAccountMembersUM {
        val account = value.account
        val phase = account.toPhase(ownAddress = value.ownAddress, invites = value.invites)

        val joined = account.members.mapIndexed { index, member ->
            joinedMember(
                index = index,
                member = member,
                isOwn = member.address.equals(value.ownAddress, ignoreCase = true),
                phase = phase,
            )
        }
        val freeSlots = freeSlots(input = value, phase = phase)

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
            footer = footer(phase = phase, input = value, freeSlots = freeSlots),
        )
    }

    private fun topBar(phase: MembersScreenPhase): JointAccountMembersUM.TopBarUM =
        if (phase.stage == MembersScreenPhase.Stage.Completed) {
            JointAccountMembersUM.TopBarUM.Titled(
                title = resourceReference(CoreUiR.string.joint_account_members_title),
                onBackClick = callbacks.onCloseClick,
            )
        } else {
            JointAccountMembersUM.TopBarUM.Dismissible(onCloseClick = callbacks.onCloseClick)
        }

    private fun header(phase: MembersScreenPhase): JointAccountMembersUM.HeaderUM? = when (phase.stage) {
        MembersScreenPhase.Stage.Completed -> null
        MembersScreenPhase.Stage.Confirming -> JointAccountMembersUM.HeaderUM(
            title = resourceReference(CoreUiR.string.joint_account_confirm_account_title),
            subtitle = resourceReference(CoreUiR.string.joint_account_confirm_account_subtitle),
        )
        MembersScreenPhase.Stage.Collecting -> JointAccountMembersUM.HeaderUM(
            title = if (phase.isThisDeviceCreator) {
                resourceReference(CoreUiR.string.joint_account_invite_members_title)
            } else {
                resourceReference(CoreUiR.string.joint_account_invite_members_waiting_title)
            },
        )
    }

    private fun summaryCard(input: Input): JointAccountMembersUM.SummaryCardUM? {
        val name = input.accountName ?: return null
        val icon = input.accountIcon ?: return null
        val account = input.account

        return JointAccountMembersUM.SummaryCardUM(
            icon = CryptoPortfolioIconConverter.convert(icon),
            name = name.toUM().value,
            membersCount = pluralReference(
                id = CoreUiR.plurals.joint_account_members_count,
                count = account.membersCount,
                formatArgs = wrappedList(account.membersCount),
            ),
            approvals = resourceReference(
                id = CoreUiR.string.joint_account_invite_members_approvals_value,
                formatArgs = wrappedList(account.threshold, account.membersCount),
            ),
            onApprovalsInfoClick = { callbacks.onApprovalsInfoClick(account.threshold, account.membersCount) },
        )
    }

    /** The button runs the creator's activation until the verification flow exists, so only the creator can press it */
    private fun footer(
        phase: MembersScreenPhase,
        input: Input,
        freeSlots: List<MemberUM.FreeSlot>,
    ): JointAccountMembersUM.FooterUM? {
        val account = input.account
        val button = JointAccountMembersUM.ConfirmButtonUM(
            isEnabled = phase.stage == MembersScreenPhase.Stage.Confirming && phase.isThisDeviceCreator,
            isLoading = input.isActivating,
            onClick = { callbacks.onActivateClick(account.cryptoAccountId) },
        )

        return when (phase.stage) {
            MembersScreenPhase.Stage.Completed -> null
            MembersScreenPhase.Stage.Collecting -> JointAccountMembersUM.FooterUM(
                hint = collectingHint(phase = phase, freeSlots = freeSlots),
                button = button,
            )
            MembersScreenPhase.Stage.Confirming -> if (phase.isOwnVerified) {
                val notConfirmed = account.members.count { member -> !member.isVerified }
                JointAccountMembersUM.FooterUM(
                    hint = pluralReference(
                        id = CoreUiR.plurals.joint_account_members_still_need_to_confirm,
                        count = notConfirmed,
                        formatArgs = wrappedList(notConfirmed),
                    ),
                    button = null,
                )
            } else {
                JointAccountMembersUM.FooterUM(hint = null, button = button)
            }
        }
    }

    private fun collectingHint(phase: MembersScreenPhase, freeSlots: List<MemberUM.FreeSlot>): TextReference {
        if (freeSlots.isEmpty()) return resourceReference(CoreUiR.string.joint_account_invite_members_everyone_joined)

        val notInvited = freeSlots.count { slot -> slot.status is SlotStatusUM.NotInvited }
        return if (phase.isThisDeviceCreator && notInvited > 0) {
            pluralReference(
                id = CoreUiR.plurals.joint_account_invite_members_invite_more,
                count = notInvited,
                formatArgs = wrappedList(notInvited),
            )
        } else {
            pluralReference(
                id = CoreUiR.plurals.joint_account_invite_members_awaiting_to_join,
                count = freeSlots.size,
                formatArgs = wrappedList(freeSlots.size),
            )
        }
    }

    private fun joinedMember(
        index: Int,
        member: JointAccount.Member,
        isOwn: Boolean,
        phase: MembersScreenPhase,
    ): MemberUM.Joined {
        val avatar = avatar(member = member, index = index)
        val name = stringReference(member.name)

        return MemberUM.Joined(
            id = member.address,
            avatar = avatar,
            name = name,
            isYou = isOwn,
            isCreator = member.role == JointAccount.Role.CREATOR,
            status = when {
                phase.stage != MembersScreenPhase.Stage.Confirming -> MemberStatusUM.Joined
                member.isVerified -> MemberStatusUM.Confirmed
                else -> MemberStatusUM.NotConfirmed
            },
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

    private fun freeSlots(input: Input, phase: MembersScreenPhase): List<MemberUM.FreeSlot> {
        val account = input.account
        val redeemedInvites = account.members
            .mapNotNull { member -> member.inviteId?.lowercase() }
            .toSet()

        return freeSlotFactory.create(
            input = FreeSlotUMFactory.Input(
                freeSlotsCount = (account.membersCount - account.members.size).coerceAtLeast(minimumValue = 0),
                firstSlotNumber = account.members.size,
                invites = input.invites.filterNot { invite -> invite.id.lowercase() in redeemedInvites },
                sharedInvites = input.sharedInvites,
                canInvite = phase.stage != MembersScreenPhase.Stage.Completed && phase.isThisDeviceCreator,
                accountName = input.accountName?.toUM()?.value,
                nowMillis = input.nowMillis,
            ),
        )
    }
}

/** @property isOwnVerified this wallet has already confirmed the account; only meaningful in [Stage.Confirming] */
private data class MembersScreenPhase(
    val stage: Stage,
    val isThisDeviceCreator: Boolean,
    val isOwnVerified: Boolean,
) {

    enum class Stage {
        /** Free slots remain. */
        Collecting,

        /** Every slot filled, each member confirms the shared address. */
        Confirming,

        /** A read-only roster; CANCELLED and UNKNOWN have no design of their own and fall back here. */
        Completed,
    }
}

/** Until the own address is derived, the invites the device holds mark the creator: only its device receives them */
private fun JointAccount.toPhase(ownAddress: String?, invites: List<JointAccountInvite>): MembersScreenPhase {
    val stage = when (status) {
        JointAccount.Status.PENDING -> MembersScreenPhase.Stage.Collecting
        JointAccount.Status.CONFIRMING -> MembersScreenPhase.Stage.Confirming
        JointAccount.Status.ACTIVE,
        JointAccount.Status.CANCELLED,
        JointAccount.Status.UNKNOWN,
        -> MembersScreenPhase.Stage.Completed
    }
    val creator = members.firstOrNull { it.role == JointAccount.Role.CREATOR }
    val own = ownAddress?.let { address -> members.firstOrNull { it.address.equals(address, ignoreCase = true) } }
    val isThisDeviceCreator = if (ownAddress != null && creator != null) {
        creator.address.equals(ownAddress, ignoreCase = true)
    } else {
        invites.isNotEmpty()
    }

    return MembersScreenPhase(
        stage = stage,
        isThisDeviceCreator = isThisDeviceCreator,
        isOwnVerified = own?.isVerified == true,
    )
}