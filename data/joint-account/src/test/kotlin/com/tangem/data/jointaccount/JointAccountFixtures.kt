package com.tangem.data.jointaccount

import com.tangem.store.datasource.jointaccount.models.JointAccountConfigDto
import com.tangem.store.datasource.jointaccount.models.JointAccountDto
import com.tangem.store.datasource.jointaccount.models.JointAccountInvitePreviewDto
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountInvitePreview
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.StatusSource
import java.math.BigInteger

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
        JointAccount.Member(
            name = "Alice",
            address = FIXTURE_ALICE_ADDRESS.lowercase(),
            role = JointAccount.Role.CREATOR,
        ),
        JointAccount.Member(
            name = "Bob",
            address = FIXTURE_BOB_ADDRESS.lowercase(),
            role = JointAccount.Role.MEMBER,
            inviteId = FIXTURE_BOB_INVITE_ID,
        ),
    ),
    source: StatusSource = StatusSource.ACTUAL,
    inFlightOperations: Map<Long, JointOperation> = emptyMap(),
): JointAccount = JointAccount(
    cryptoAccountId = cryptoAccountId,
    membersCount = membersCount,
    threshold = threshold,
    safeAddress = safeAddress,
    status = status,
    members = members,
    source = source,
    inFlightOperations = inFlightOperations,
)

internal fun createJointOperation(
    operationId: String = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
    chainId: Long = 137,
    safeTx: SafeTx = createSafeTx(),
    status: JointOperation.Status = JointOperation.Status.AWAITING_SIGNATURES,
    initiator: String? = FIXTURE_ALICE_ADDRESS.lowercase(),
    signatures: List<JointOperation.MemberSignature> = listOf(
        JointOperation.MemberSignature(
            address = FIXTURE_ALICE_ADDRESS.lowercase(),
            state = JointOperation.MemberSignature.State.SIGNED,
            at = "2026-09-07T10:00:00Z",
        ),
    ),
): JointOperation = JointOperation(
    operationId = operationId,
    chainId = chainId,
    safeTx = safeTx,
    status = status,
    initiator = initiator,
    signatures = signatures,
)

internal fun createSafeTx(
    to: String = "0x1111111111111111111111111111111111111111",
    value: BigInteger = BigInteger("1000000000000000000"),
    data: String = "0x",
    operation: Int = SafeTx.OPERATION_CALL,
    safeTxGas: BigInteger = BigInteger("112500"),
    baseGas: BigInteger = BigInteger("410000"),
    gasPrice: BigInteger = BigInteger("60000000000"),
    gasToken: String = "0x0000000000000000000000000000000000000000",
    refundReceiver: String = "0x2222222222222222222222222222222222222222",
    nonce: BigInteger = BigInteger.valueOf(4),
): SafeTx = SafeTx(
    to = to,
    value = value,
    data = data,
    operation = operation,
    safeTxGas = safeTxGas,
    baseGas = baseGas,
    gasPrice = gasPrice,
    gasToken = gasToken,
    refundReceiver = refundReceiver,
    nonce = nonce,
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