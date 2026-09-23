package com.tangem.features.txhistory.joint.operation.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class JointOperationStateTest {

    @Test
    fun `GIVEN this member is still waiting WHEN they answered locally THEN the list shows them signed`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN, answer = Answer.WAITING), signer(address = OTHER)))

        // Act
        val actual = state.withOwnAnswer(answeredLocally = Answer.SIGNED)

        // Assert
        assertThat(actual.signers.map { it.answer }).containsExactly(Answer.SIGNED, Answer.WAITING).inOrder()
    }

    @Test
    fun `GIVEN this member is still waiting WHEN they answered locally THEN the progress counts their signature`() {
        // Arrange
        val state = state(
            signers = listOf(
                signer(address = OWN, answer = Answer.WAITING),
                signer(address = OTHER, answer = Answer.CREATED),
            ),
        )

        // Act
        val actual = state.withOwnAnswer(answeredLocally = Answer.SIGNED)

        // Assert
        assertThat(actual.collected).isEqualTo(2)
    }

    @Test
    fun `GIVEN this member is still waiting WHEN they have not answered locally THEN the list is untouched`() {
        // Arrange
        val signers = listOf(signer(address = OWN, answer = Answer.WAITING))
        val state = state(signers = signers)

        // Act
        val actual = state.withOwnAnswer(answeredLocally = null)

        // Assert
        assertThat(actual.signers).isEqualTo(signers)
    }

    @Test
    fun `GIVEN the refresh already carried the answer WHEN answered locally THEN the dated answer survives`() {
        // Arrange
        val signers = listOf(signer(address = OWN, answer = Answer.SIGNED, at = AT))
        val state = state(signers = signers)

        // Act
        val actual = state.withOwnAnswer(answeredLocally = Answer.SIGNED)

        // Assert
        assertThat(actual.signers).isEqualTo(signers)
    }

    @Test
    fun `GIVEN the owner address is not resolvable WHEN answered locally THEN no one is marked signed`() {
        // Arrange
        val signers = listOf(signer(address = OWN, answer = Answer.WAITING))
        val state = state(signers = signers, ownAddress = null)

        // Act
        val actual = state.withOwnAnswer(answeredLocally = Answer.SIGNED)

        // Assert
        assertThat(actual.signers).isEqualTo(signers)
    }

    @Test
    fun `GIVEN the roster spells the address in another case WHEN state built THEN it is still this member`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN.uppercase(), answer = Answer.DECLINED)))

        // Act
        val actual = state.ownAnswer

        // Assert
        assertThat(actual).isEqualTo(Answer.DECLINED)
    }

    @Test
    fun `GIVEN no roster entry for this member WHEN state built THEN there is no own answer`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OTHER)))

        // Act
        val actual = state.ownAnswer

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN this member is still waiting WHEN they declined locally THEN the list shows them declined`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN, answer = Answer.WAITING)))

        // Act
        val actual = state.withOwnAnswer(answeredLocally = Answer.DECLINED)

        // Assert
        assertThat(actual.signers.single().answer).isEqualTo(Answer.DECLINED)
        assertThat(actual.ownAnswer).isEqualTo(Answer.DECLINED)
    }

    @Test
    fun `GIVEN the initiator spelled in another case WHEN state built THEN this member is the initiator`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN, answer = Answer.CREATED)), initiator = OWN.uppercase())

        // Act
        val actual = state.role

        // Assert
        assertThat(actual).isEqualTo(JointOperationState.Role.INITIATOR)
    }

    @Test
    fun `GIVEN another member created the operation WHEN state built THEN this member is a plain member`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN)), initiator = OTHER)

        // Act
        val actual = state.role

        // Assert
        assertThat(actual).isEqualTo(JointOperationState.Role.MEMBER)
    }

    @Test
    fun `GIVEN the owner address is not resolvable WHEN state built THEN the role is unknown`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN, answer = Answer.CREATED)), ownAddress = null)

        // Act
        val actual = state.role

        // Assert
        assertThat(actual).isEqualTo(JointOperationState.Role.UNKNOWN)
    }

    @Test
    fun `GIVEN the backend named no initiator WHEN state built THEN the role is unknown`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN, answer = Answer.CREATED)), initiator = null)

        // Act
        val actual = state.role

        // Assert
        assertThat(actual).isEqualTo(JointOperationState.Role.UNKNOWN)
    }

    @Test
    fun `GIVEN this member waits for their turn WHEN state built THEN they may refuse the operation`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN, answer = Answer.WAITING)))

        // Act
        val actual = state.canRefuse

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN this owner address is outside the roster WHEN state built THEN they may not refuse the operation`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OTHER, answer = Answer.CREATED)))

        // Act
        val actual = state.canRefuse

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN this member already declined WHEN state built THEN they may not refuse the operation again`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN, answer = Answer.DECLINED)))

        // Act
        val actual = state.canRefuse

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN the backend named no initiator WHEN state built THEN they may not refuse the operation`() {
        // Arrange
        val state = state(signers = listOf(signer(address = OWN)), initiator = null)

        // Act
        val actual = state.canRefuse

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN as many declines as the threshold allows WHEN state built THEN it is still reachable`() {
        // Arrange — 3 members, threshold 2: one decline leaves exactly two who can sign
        val state = state(
            signers = listOf(
                signer(address = OTHER, answer = Answer.CREATED),
                signer(address = OWN, answer = Answer.DECLINED),
                signer(address = THIRD),
            ),
        )

        // Act
        val actual = state.isReachable

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN one decline too many WHEN state built THEN the threshold is out of reach`() {
        // Arrange
        val state = state(
            signers = listOf(
                signer(address = OTHER, answer = Answer.CREATED),
                signer(address = OWN, answer = Answer.DECLINED),
                signer(address = THIRD, answer = Answer.DECLINED),
            ),
        )

        // Act
        val actual = state.isReachable

        // Assert
        assertThat(actual).isFalse()
    }

    private fun state(
        signers: List<JointOperationSignerState>,
        ownAddress: String? = OWN,
        initiator: String? = OTHER,
    ) = JointOperationState(
        account = MockAccounts.createJointAccount(),
        displayed = JointTransferIntent.Native(recipient = OTHER, amountWei = BigInteger.ONE),
        feeWei = BigInteger.ZERO,
        feeCoin = null,
        threshold = 2,
        membersCount = signers.size,
        signers = signers,
        canSign = true,
        ownAddress = ownAddress,
        initiator = initiator,
    )

    private fun signer(address: String, answer: Answer = Answer.WAITING, at: String? = null) =
        JointOperationSignerState(
            member = JointAccount.Member(name = "Isaac", address = address, role = JointAccount.Role.MEMBER),
            answer = answer,
            at = at,
        )

    private companion object {
        const val OWN = "0xowner"
        const val OTHER = "0xother"
        const val THIRD = "0xthird"
        const val AT = "2026-09-01T12:00:00Z"
    }
}