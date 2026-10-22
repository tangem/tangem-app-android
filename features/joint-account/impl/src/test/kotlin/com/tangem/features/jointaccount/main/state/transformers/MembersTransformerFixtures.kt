package com.tangem.features.jointaccount.main.state.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.features.jointaccount.main.JointAccountMembersUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal const val FIXTURE_ACCOUNT_ID = "a1"
internal const val FIXTURE_CREATOR_NAME = "Ivan"
internal const val FIXTURE_MEMBER_NAME = "Dasha"

internal val fixtureCreator = JointAccount.Member(
    name = FIXTURE_CREATOR_NAME,
    address = "0xc0ffee",
    role = JointAccount.Role.CREATOR,
)

internal val fixtureMember = JointAccount.Member(
    name = FIXTURE_MEMBER_NAME,
    address = "0xda5ha",
    role = JointAccount.Role.MEMBER,
)

internal fun createJointAccount(
    cryptoAccountId: String = FIXTURE_ACCOUNT_ID,
    membersCount: Int = 3,
    threshold: Int = 2,
    safeAddress: String? = null,
    status: JointAccount.Status = JointAccount.Status.PENDING,
    members: List<JointAccount.Member> = listOf(fixtureCreator),
    source: StatusSource = StatusSource.ACTUAL,
): JointAccount = JointAccount(
    cryptoAccountId = cryptoAccountId,
    membersCount = membersCount,
    threshold = threshold,
    safeAddress = safeAddress,
    status = status,
    members = members,
    source = source,
)

internal fun createMembersUM(
    topBar: JointAccountMembersUM.TopBarUM = JointAccountMembersUM.TopBarUM.Dismissible(onCloseClick = {}),
    header: JointAccountMembersUM.HeaderUM? = null,
    summaryCard: JointAccountMembersUM.SummaryCardUM? = null,
    joined: ImmutableList<JointAccountMembersUM.MemberUM.Joined> = persistentListOf(),
    freeSlots: ImmutableList<JointAccountMembersUM.MemberUM.FreeSlot> = persistentListOf(),
    membersLabel: TextReference? = null,
    waitingLabel: TextReference? = null,
    footer: JointAccountMembersUM.FooterUM? = null,
): JointAccountMembersUM = JointAccountMembersUM(
    topBar = topBar,
    header = header,
    summaryCard = summaryCard,
    joined = joined,
    freeSlots = freeSlots,
    membersLabel = membersLabel,
    waitingLabel = waitingLabel,
    footer = footer,
)

internal fun JointAccountMembersUM.SlotActionUM?.click() = when (this) {
    is JointAccountMembersUM.SlotActionUM.Invite -> onClick()
    is JointAccountMembersUM.SlotActionUM.ReInvite -> onClick()
    is JointAccountMembersUM.SlotActionUM.Loading, null -> error("The slot offers no button to press")
}