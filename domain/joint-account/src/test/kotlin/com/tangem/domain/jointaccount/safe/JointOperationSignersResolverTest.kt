package com.tangem.domain.jointaccount.safe

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.StatusSource
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class JointOperationSignersResolverTest {

    private val resolver = JointOperationSignersResolver()

    private val alice = JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR)
    private val bob = JointAccount.Member(name = "Bob", address = BOB, role = JointAccount.Role.MEMBER)
    private val carol = JointAccount.Member(name = "Carol", address = CAROL, role = JointAccount.Role.MEMBER)
    private val dave = JointAccount.Member(name = "Dave", address = DAVE, role = JointAccount.Role.MEMBER)

    @Test
    fun `GIVEN answers from some members WHEN signers are resolved THEN every member gets a standing in roster order`() {
        // Arrange
        val operation = operation(
            signatures = listOf(
                signature(address = ALICE.uppercase().replaceFirst("0X", "0x"), state = SIGNED, at = "2026-09-08T15:34:00Z"),
                signature(address = BOB, state = SIGNED, at = "2026-09-08T15:36:00Z"),
                signature(address = CAROL, state = DECLINED, at = "2026-09-08T15:40:00Z"),
            ),
        )

        // Act
        val actual = resolver.signers(jointAccount = jointAccount(), operation = operation)

        // Assert
        assertThat(actual).containsExactly(
            JointOperationSignerState(member = alice, answer = Answer.CREATED, at = "2026-09-08T15:34:00Z"),
            JointOperationSignerState(member = bob, answer = Answer.SIGNED, at = "2026-09-08T15:36:00Z"),
            JointOperationSignerState(member = carol, answer = Answer.DECLINED, at = "2026-09-08T15:40:00Z"),
            JointOperationSignerState(member = dave, answer = Answer.WAITING, at = null),
        ).inOrder()
    }

    @Test
    fun `GIVEN the initiator has no vote record WHEN signers are resolved THEN they are dated by the creation time`() {
        // Arrange
        val operation = operation(signatures = emptyList(), createdAt = "2026-09-08T15:30:00Z")

        // Act
        val actual = resolver.signers(jointAccount = jointAccount(), operation = operation)

        // Assert
        assertThat(actual.single { it.member == alice })
            .isEqualTo(JointOperationSignerState(member = alice, answer = Answer.CREATED, at = "2026-09-08T15:30:00Z"))
    }

    @Test
    fun `GIVEN an unknown signature state WHEN signers are resolved THEN the member is still waiting`() {
        // Arrange
        val operation = operation(signatures = listOf(signature(address = BOB, state = UNKNOWN, at = null)))

        // Act
        val actual = resolver.signers(jointAccount = jointAccount(), operation = operation)

        // Assert
        assertThat(actual.single { it.member == bob }.answer).isEqualTo(Answer.WAITING)
    }

    @Test
    fun `GIVEN a member who has not answered WHEN the operation awaits signatures THEN sign is offered`() {
        // Arrange
        val operation = operation(signatures = listOf(signature(address = ALICE, state = SIGNED, at = null)))

        // Act
        val actual = resolver.canSign(jointAccount = jointAccount(), operation = operation, ownerAddress = BOB.uppercase().replaceFirst("0X", "0x"))

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN a member who already answered WHEN sign is checked THEN it is not offered`() {
        // Arrange
        val operation = operation(signatures = listOf(signature(address = CAROL, state = DECLINED, at = null)))

        // Act
        val actual = resolver.canSign(jointAccount = jointAccount(), operation = operation, ownerAddress = CAROL)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN the initiator with no signature record WHEN sign is checked THEN it is not offered again`() {
        // Act
        val actual = resolver.canSign(jointAccount = jointAccount(), operation = operation(), ownerAddress = ALICE)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN an operation past signature collection or no owner address WHEN sign is checked THEN it is not offered`() {
        // Act
        val pending = resolver.canSign(jointAccount = jointAccount(), operation = operation(status = JointOperation.Status.PENDING), ownerAddress = BOB)
        val unknown = resolver.canSign(jointAccount = jointAccount(), operation = operation(status = JointOperation.Status.UNKNOWN), ownerAddress = BOB)
        val noAddress = resolver.canSign(jointAccount = jointAccount(), operation = operation(), ownerAddress = null)

        // Assert
        assertThat(listOf(pending, unknown, noAddress)).containsExactly(false, false, false)
    }

    @Test
    fun `GIVEN a signature state this build does not know WHEN signers are resolved THEN the member waits undated`() {
        // Arrange
        val operation = operation(
            signatures = listOf(signature(address = BOB, state = UNKNOWN, at = "2026-09-08T15:36:00Z")),
        )

        // Act
        val actual = resolver.signers(jointAccount = jointAccount(), operation = operation)

        // Assert
        assertThat(actual.first { it.member == bob })
            .isEqualTo(JointOperationSignerState(member = bob, answer = Answer.WAITING, at = null))
    }

    @Test
    fun `GIVEN an owner address outside the roster WHEN sign is checked THEN it is not offered`() {
        // Act
        val actual = resolver.canSign(
            jointAccount = jointAccount(),
            operation = operation(),
            ownerAddress = "0xdddddddddddddddddddddddddddddddddddddddd".replace("d", "e"),
        )

        // Assert
        assertThat(actual).isFalse()
    }

    private fun jointAccount() = JointAccount(
        cryptoAccountId = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
        membersCount = 4,
        threshold = 3,
        safeAddress = "0x5afe000000000000000000000000000000000001",
        status = JointAccount.Status.ACTIVE,
        members = listOf(alice, bob, carol, dave),
        source = StatusSource.ACTUAL,
    )

    private fun operation(
        status: JointOperation.Status = JointOperation.Status.AWAITING_SIGNATURES,
        signatures: List<JointOperation.MemberSignature> = emptyList(),
        createdAt: String? = null,
    ) = JointOperation(
        operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
        chainId = 137,
        safeTx = mockk<SafeTx>(),
        status = status,
        initiator = ALICE,
        signatures = signatures,
        createdAt = createdAt,
    )

    private fun signature(address: String, state: JointOperation.MemberSignature.State, at: String?) =
        JointOperation.MemberSignature(address = address, state = state, at = at)

    private companion object {
        const val ALICE = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val BOB = "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        const val CAROL = "0xcccccccccccccccccccccccccccccccccccccccc"
        const val DAVE = "0xdddddddddddddddddddddddddddddddddddddddd"
        val SIGNED = JointOperation.MemberSignature.State.SIGNED
        val DECLINED = JointOperation.MemberSignature.State.DECLINED
        val UNKNOWN = JointOperation.MemberSignature.State.UNKNOWN
    }
}