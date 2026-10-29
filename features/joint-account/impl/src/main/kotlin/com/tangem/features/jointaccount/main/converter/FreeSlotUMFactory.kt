package com.tangem.features.jointaccount.main.converter

import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.jointaccount.model.JointAccountInvite
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.SlotActionUM
import com.tangem.features.jointaccount.main.JointAccountMembersUM.SlotStatusUM
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.util.Locale

/** Builds the free-slot rows; an expired invite offers Re-invite like a live one */
internal class FreeSlotUMFactory(
    private val onInviteClick: (slot: Int, accountName: TextReference) -> Unit,
) {

    /** @property issuingSlot the slot whose invite is being issued */
    data class Input(
        val slots: List<Int>,
        val invites: List<JointAccountInvite>,
        val canInvite: Boolean,
        val accountName: TextReference?,
        val issuingSlot: Int?,
        val nowMillis: Long,
    )

    fun create(input: Input): List<JointAccountMembersUM.MemberUM.FreeSlot> = input.slots.map { slot ->
        val invite = input.invites.firstOrNull { it.slot == slot }
        val status = if (input.canInvite) {
            slotStatus(invite = invite, nowMillis = input.nowMillis)
        } else {
            SlotStatusUM.WaitingToJoin
        }

        JointAccountMembersUM.MemberUM.FreeSlot(
            id = "slot_$slot",
            title = resourceReference(
                id = CoreUiR.string.joint_account_invite_members_member_number,
                formatArgs = wrappedList(slot),
            ),
            status = status,
            action = slotAction(slot = slot, status = status, input = input),
        )
    }

    private fun slotStatus(invite: JointAccountInvite?, nowMillis: Long): SlotStatusUM = when {
        invite == null -> SlotStatusUM.NotInvited
        invite.isExpired(nowMillis = nowMillis) -> SlotStatusUM.Expired
        else -> SlotStatusUM.Invited(
            label = untilLabel(expiresAtMillis = invite.expiresAtMillis, nowMillis = nowMillis),
        )
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

    private fun slotAction(slot: Int, status: SlotStatusUM, input: Input): SlotActionUM? {
        val accountName = input.accountName
        if (!input.canInvite || accountName == null) return null

        val isLoading = input.issuingSlot == slot
        val onClick = { onInviteClick(slot, accountName) }
        return if (status is SlotStatusUM.NotInvited) {
            SlotActionUM.Invite(onClick = onClick, isLoading = isLoading)
        } else {
            SlotActionUM.ReInvite(onClick = onClick, isLoading = isLoading)
        }
    }

    companion object {
        fun dateTimeFormatter(): DateTimeFormatter = DateTimeFormat.forStyle("MS").withLocale(Locale.getDefault())
    }
}