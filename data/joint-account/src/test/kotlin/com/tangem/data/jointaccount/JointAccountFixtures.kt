package com.tangem.data.jointaccount

import com.tangem.datasource.api.jointaccount.models.JointAccountDto
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource

internal const val FIXTURE_CRYPTO_ACCOUNT_ID = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"
internal const val FIXTURE_ALICE_ADDRESS = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
internal const val FIXTURE_BOB_ADDRESS = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"

internal fun createJointAccount(
    cryptoAccountId: String = FIXTURE_CRYPTO_ACCOUNT_ID,
    membersCount: Int = 3,
    threshold: Int = 2,
    address: String? = null,
    status: JointAccount.Status = JointAccount.Status.PENDING,
    members: List<JointAccount.Member> = listOf(
        JointAccount.Member(name = "Alice", address = FIXTURE_ALICE_ADDRESS, role = JointAccount.Role.CREATOR),
        JointAccount.Member(name = "Bob", address = FIXTURE_BOB_ADDRESS, role = JointAccount.Role.MEMBER),
    ),
    source: StatusSource = StatusSource.ACTUAL,
): JointAccount = JointAccount(
    cryptoAccountId = cryptoAccountId,
    membersCount = membersCount,
    threshold = threshold,
    address = address,
    status = status,
    members = members,
    source = source,
)

internal fun createJointAccountDto(
    cryptoAccountId: String = FIXTURE_CRYPTO_ACCOUNT_ID,
    membersCount: Int = 3,
    threshold: Int = 2,
    address: String? = null,
    status: String = "pending",
    members: List<JointAccountDto.Member> = listOf(
        JointAccountDto.Member(name = "Alice", address = FIXTURE_ALICE_ADDRESS, role = "creator"),
        JointAccountDto.Member(name = "Bob", address = FIXTURE_BOB_ADDRESS, role = "member"),
    ),
): JointAccountDto = JointAccountDto(
    cryptoAccountId = cryptoAccountId,
    membersCount = membersCount,
    threshold = threshold,
    address = address,
    status = status,
    members = members,
    invites = null,
)