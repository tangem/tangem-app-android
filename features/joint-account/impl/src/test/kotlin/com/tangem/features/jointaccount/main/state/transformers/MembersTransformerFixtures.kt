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
    title: TextReference = TextReference.EMPTY,
    progress: TextReference = TextReference.EMPTY,
    shareSafely: JointAccountMembersUM.ShareSafelyUM = JointAccountMembersUM.ShareSafelyUM(
        title = TextReference.EMPTY,
        description = TextReference.EMPTY,
        onClick = {},
    ),
    members: ImmutableList<JointAccountMembersUM.MemberUM> = persistentListOf(),
    otherMembersLabel: TextReference? = null,
    canArchive: Boolean = false,
    activation: JointAccountMembersUM.ActivationUM? = null,
    onArchiveClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
): JointAccountMembersUM = JointAccountMembersUM(
    title = title,
    progress = progress,
    shareSafely = shareSafely,
    members = members,
    otherMembersLabel = otherMembersLabel,
    canArchive = canArchive,
    activation = activation,
    onArchiveClick = onArchiveClick,
    onCloseClick = onCloseClick,
)

internal fun createActivation(
    confirmation: JointAccountMembersUM.ActivationUM.ConfirmationUM? = null,
    onActivateClick: () -> Unit = {},
): JointAccountMembersUM.ActivationUM = JointAccountMembersUM.ActivationUM(
    onActivateClick = onActivateClick,
    confirmation = confirmation,
)

internal fun createConfirmation(
    walletInteractionIconRes: Int? = null,
    isConfirming: Boolean = false,
    onConfirmClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
): JointAccountMembersUM.ActivationUM.ConfirmationUM = JointAccountMembersUM.ActivationUM.ConfirmationUM(
    walletInteractionIconRes = walletInteractionIconRes,
    isConfirming = isConfirming,
    onConfirmClick = onConfirmClick,
    onCancelClick = onCancelClick,
)