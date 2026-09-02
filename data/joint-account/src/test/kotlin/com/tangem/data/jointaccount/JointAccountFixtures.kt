package com.tangem.data.jointaccount

import com.tangem.store.datasource.jointaccount.models.JointAccountConfigDto
import com.tangem.store.datasource.jointaccount.models.JointAccountDto
import com.tangem.store.datasource.jointaccount.models.JointAccountInvitePreviewDto
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.models.StatusSource

internal const val FIXTURE_CRYPTO_ACCOUNT_ID = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"
internal const val FIXTURE_ALICE_ADDRESS = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
internal const val FIXTURE_BOB_ADDRESS = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"
internal const val FIXTURE_BOB_INVITE_ID = "AA11BB22CC33DD44EE55FF66AA77BB88CC99DD00EE11FF22AA33BB44CC55DD66"

internal fun createJointAccount(
    cryptoAccountId: String = FIXTURE_CRYPTO_ACCOUNT_ID,
    membersCount: Int = 3,
    threshold: Int = 2,
    safeAddress: String? = null,
    status: JointAccount.Status = JointAccount.Status.PENDING,
    members: List<JointAccount.Member> = listOf(
        JointAccount.Member(name = "Alice", address = FIXTURE_ALICE_ADDRESS, role = JointAccount.Role.CREATOR),
        JointAccount.Member(
            name = "Bob",
            address = FIXTURE_BOB_ADDRESS,
            role = JointAccount.Role.MEMBER,
            inviteId = FIXTURE_BOB_INVITE_ID,
        ),
    ),
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

internal fun createJointAccountDto(
    cryptoAccountId: String = FIXTURE_CRYPTO_ACCOUNT_ID,
    membersCount: Int = 3,
    threshold: Int = 2,
    safeAddress: String? = null,
    status: String = "pending",
    members: List<JointAccountDto.Member> = listOf(
        JointAccountDto.Member(name = "Alice", address = FIXTURE_ALICE_ADDRESS, role = "creator"),
        JointAccountDto.Member(
            name = "Bob",
            address = FIXTURE_BOB_ADDRESS,
            role = "member",
            inviteId = FIXTURE_BOB_INVITE_ID,
        ),
    ),
    invites: List<JointAccountDto.Invite>? = null,
): JointAccountDto = JointAccountDto(
    cryptoAccountId = cryptoAccountId,
    membersCount = membersCount,
    threshold = threshold,
    safeAddress = safeAddress,
    status = status,
    members = members,
    invites = invites,
)

internal fun createJointAccountInvitePreviewDto(
    name: String = "Family savings",
    icon: String = "Family",
    iconColor: String = "Azure",
    membersCount: Int = 3,
    threshold: Int = 2,
    creatorName: String = "Alice",
    creatorAddress: String = FIXTURE_ALICE_ADDRESS,
): JointAccountInvitePreviewDto = JointAccountInvitePreviewDto(
    config = JointAccountConfigDto(
        name = name,
        icon = icon,
        iconColor = iconColor,
        membersCount = membersCount,
        threshold = threshold,
    ),
    creator = JointAccountInvitePreviewDto.Creator(name = creatorName, address = creatorAddress),
)

internal fun createJointAccountInvitePreview(
    name: String = "Family savings",
    icon: String = "Family",
    iconColor: String = "Azure",
    membersCount: Int = 3,
    threshold: Int = 2,
    creatorName: String = "Alice",
    creatorAddress: String = FIXTURE_ALICE_ADDRESS,
): JointAccountInvitePreview = JointAccountInvitePreview(
    config = JointAccountConfig(
        name = name,
        icon = icon,
        iconColor = iconColor,
        membersCount = membersCount,
        threshold = threshold,
    ),
    creator = JointAccountInvitePreview.Creator(name = creatorName, address = creatorAddress),
)