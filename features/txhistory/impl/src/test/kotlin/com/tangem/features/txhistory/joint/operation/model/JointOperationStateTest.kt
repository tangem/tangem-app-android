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
        val actual = state.signersWithOwnAnswer(answeredLocally = true)

        // Assert
        assertThat(actual.map { it.answer }).containsExactly(Answer.SIGNED, Answer.WAITING).inOrder()
    }

    @Test
    fun `GIVEN this member is still waiting WHEN they have not answered locally THEN the list is untouched`() {
        // Arrange
        val signers = listOf(signer(address = OWN, answer = Answer.WAITING))
        val state = state(signers = signers)

        // Act
        val actual = state.signersWithOwnAnswer(answeredLocally = false)

        // Assert
        assertThat(actual).isEqualTo(signers)
    }

    @Test
    fun `GIVEN the refresh already carried the answer WHEN answered locally THEN the dated answer survives`() {
        // Arrange
        val signers = listOf(signer(address = OWN, answer = Answer.SIGNED, at = AT))
        val state = state(signers = signers)

        // Act
        val actual = state.signersWithOwnAnswer(answeredLocally = true)

        // Assert
        assertThat(actual).isEqualTo(signers)
    }

    @Test
    fun `GIVEN the owner address is not resolvable WHEN answered locally THEN no one is marked signed`() {
        // Arrange
        val signers = listOf(signer(address = OWN, answer = Answer.WAITING))
        val state = state(signers = signers, ownAddress = null)

        // Act
        val actual = state.signersWithOwnAnswer(answeredLocally = true)

        // Assert
        assertThat(actual).isEqualTo(signers)
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

    private fun state(
        signers: List<JointOperationSignerState>,
        ownAddress: String? = OWN,
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
        const val AT = "2026-09-01T12:00:00Z"
    }
}