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
 * Builds the free-slot rows. The slot state is the backend's: an issued invite shows its deadline, an expired one
 * reads "Invite expired". No row offers a button yet: a code can only come from issuing one.
 */
internal class FreeSlotUMFactory {

    /** @property slots the slot numbers to draw, in order; [invites] are looked up by slot */
    data class Input(
        val slots: List<Int>,
        val invites: List<JointAccountInvite>,
        val canInvite: Boolean,
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
            action = null,
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

    companion object {
        fun dateTimeFormatter(): DateTimeFormatter = DateTimeFormat.forStyle("MS").withLocale(Locale.getDefault())
    }
}