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
    topBar: JointAccountMembersUM.TopBarUM = JointAccountMembersUM.TopBarUM.Dismissible(
        canArchive = false,
        onArchiveClick = {},
        onCloseClick = {},
    ),
    header: JointAccountMembersUM.HeaderUM? = null,
    shareSafely: JointAccountMembersUM.ShareSafelyUM? = null,
    joined: ImmutableList<JointAccountMembersUM.MemberUM.Joined> = persistentListOf(),
    freeSlots: ImmutableList<JointAccountMembersUM.MemberUM.FreeSlot> = persistentListOf(),
    otherMembersLabel: TextReference? = null,
    onActivateClick: (() -> Unit)? = null,
): JointAccountMembersUM = JointAccountMembersUM(
    topBar = topBar,
    header = header,
    shareSafely = shareSafely,
    joined = joined,
    freeSlots = freeSlots,
    otherMembersLabel = otherMembersLabel,
    onActivateClick = onActivateClick,
)