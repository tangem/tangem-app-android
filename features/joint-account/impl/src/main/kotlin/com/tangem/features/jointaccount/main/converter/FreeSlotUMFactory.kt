package com.tangem.features.jointaccount.main.converter

import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.SlotStatusUM
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds the free-slot rows. A slot whose invite was shared once offers Re-invite: it will ask the backend for a
 * fresh link, and until that endpoint exists it shares the same invite again.
 */
internal class FreeSlotUMFactory(
    private val onInviteClick: (inviteId: String, accountName: TextReference) -> Unit,
) {

    data class Input(
        val freeSlotsCount: Int,
        val firstSlotNumber: Int,
        val invites: List<JointAccountInvite>,
        val sharedInvites: Set<String>,
        val canInvite: Boolean,
        val accountName: TextReference?,
        val nowMillis: Long,
    )

    fun create(input: Input): List<JointAccountMembersUM.MemberUM.FreeSlot> = List(
        size = input.freeSlotsCount,
    ) { index ->
        val invite = input.invites.getOrNull(index)
        val status = if (input.canInvite) {
            slotStatus(invite = invite, isShared = invite?.id in input.sharedInvites, nowMillis = input.nowMillis)
        } else {
            SlotStatusUM.WaitingToJoin
        }

        JointAccountMembersUM.MemberUM.FreeSlot(
            id = "slot_$index",
            title = resourceReference(
                id = CoreUiR.string.joint_account_invite_members_member_number,
                formatArgs = wrappedList(input.firstSlotNumber + index),
            ),
            status = status,
            action = slotAction(invite = invite, status = status, input = input),
        )
    }

    private fun slotStatus(invite: JointAccountInvite?, isShared: Boolean, nowMillis: Long): SlotStatusUM {
        val expiresAtMillis = invite?.expiresAtMillis

        return when {
            expiresAtMillis != null && expiresAtMillis <= nowMillis -> SlotStatusUM.Expired
            !isShared -> SlotStatusUM.NotInvited
            expiresAtMillis == null -> SlotStatusUM.Invited(
                label = resourceReference(CoreUiR.string.joint_account_invite_members_invited),
            )
            else -> SlotStatusUM.Invited(label = untilLabel(expiresAtMillis = expiresAtMillis, nowMillis = nowMillis))
        }
    }

    private fun untilLabel(expiresAtMillis: Long, nowMillis: Long): TextReference {
        val expiresAt = DateTime(expiresAtMillis)
        val today = DateTime(nowMillis).toLocalDate()
        val time = DateTimeFormatters.timeFormatter.print(expiresAt)

        return when (expiresAt.toLocalDate()) {
            today -> resourceReference(CoreUiR.string.joint_account_invite_members_until_today, wrappedList(time))
            today.plusDays(1) -> resourceReference(
                id = CoreUiR.string.joint_account_invite_members_until_tomorrow,
                formatArgs = wrappedList(time),
            )
            else -> resourceReference(
                id = CoreUiR.string.joint_account_invite_members_until,
                formatArgs = wrappedList(dateTimeFormatter().print(expiresAt)),
            )
        }
    }

    private fun slotAction(
        invite: JointAccountInvite?,
        status: SlotStatusUM,
        input: Input,
    ): JointAccountMembersUM.SlotActionUM? {
        val accountName = input.accountName
        if (!input.canInvite || invite == null || accountName == null) return null

        val onClick = { onInviteClick(invite.id, accountName) }
        return if (status is SlotStatusUM.NotInvited) {
            JointAccountMembersUM.SlotActionUM.Invite(onClick = onClick)
        } else {
            JointAccountMembersUM.SlotActionUM.ReInvite(onClick = onClick)
        }
    }

    companion object {
        fun dateTimeFormatter(): DateTimeFormatter = DateTimeFormat.forStyle("MS").withLocale(Locale.getDefault())
    }
}