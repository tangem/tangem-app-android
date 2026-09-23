package com.tangem.features.txhistory.joint.operation.signers.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM
import org.junit.jupiter.api.Test
import com.tangem.core.ui.R as CoreUiR

internal class JointOperationSignersUMConverterTest {

    private val converter = JointOperationSignersUMConverter()

    @Test
    fun `GIVEN the initiator WHEN converted THEN the created status is dated and marked answered`() {
        // Act
        val actual = convert(signer(answer = Answer.CREATED, at = AT))

        // Assert
        assertThat(actual.statusResId()).isEqualTo(CoreUiR.string.joint_operation_created_at)
        assertThat(actual.mark).isEqualTo(JointOperationSignersUM.Mark.Signed)
    }

    @Test
    fun `GIVEN a member who signed WHEN converted THEN the signed status is dated and marked answered`() {
        // Act
        val actual = convert(signer(answer = Answer.SIGNED, at = AT))

        // Assert
        assertThat(actual.statusResId()).isEqualTo(CoreUiR.string.joint_operation_signed_at)
        assertThat(actual.mark).isEqualTo(JointOperationSignersUM.Mark.Signed)
    }

    @Test
    fun `GIVEN a member who declined WHEN converted THEN the declined status is dated and marked declined`() {
        // Act
        val actual = convert(signer(answer = Answer.DECLINED, at = AT))

        // Assert
        assertThat(actual.statusResId()).isEqualTo(CoreUiR.string.joint_operation_declined_at)
        assertThat(actual.mark).isEqualTo(JointOperationSignersUM.Mark.Declined)
    }

    @Test
    fun `GIVEN a member who declined without a time WHEN converted THEN the status is undated`() {
        // Act
        val actual = convert(signer(answer = Answer.DECLINED, at = null))

        // Assert
        assertThat(actual.statusResId()).isEqualTo(CoreUiR.string.joint_operation_declined)
        assertThat(actual.mark).isEqualTo(JointOperationSignersUM.Mark.Declined)
    }

    @Test
    fun `GIVEN a member yet to answer WHEN converted THEN the waiting status carries no mark`() {
        // Act
        val actual = convert(signer(answer = Answer.WAITING, at = null))

        // Assert
        assertThat(actual.statusResId()).isEqualTo(CoreUiR.string.joint_operation_waiting_to_sign)
        assertThat(actual.mark).isNull()
    }

    @Test
    fun `GIVEN an answer the backend reported without a time WHEN converted THEN the status stays empty`() {
        // Act
        val actual = convert(signer(answer = Answer.SIGNED, at = null))

        // Assert
        assertThat(actual.statusText).isEqualTo(TextReference.EMPTY)
        assertThat(actual.mark).isEqualTo(JointOperationSignersUM.Mark.Signed)
    }

    @Test
    fun `GIVEN the roster WHEN converted THEN every member keeps its place and takes its own avatar letter`() {
        // Act
        val actual = converter.convert(
            value = JointOperationSignersUMConverter.Input(
                membersCount = 2,
                signers = listOf(
                    signer(answer = Answer.CREATED, at = AT, name = "Isaac"),
                    signer(answer = Answer.WAITING, at = null, name = "daniel"),
                ),
            ),
        )

        // Assert
        assertThat(actual.membersCount).isEqualTo(2)
        assertThat(actual.signers.map { it.name }).containsExactly("Isaac", "daniel").inOrder()
        assertThat(actual.signers.map { it.avatar.monogram }).containsExactly("I", "D").inOrder()
    }

    private fun convert(signer: JointOperationSignerState): JointOperationSignersUM.SignerUM {
        return converter.convert(
            value = JointOperationSignersUMConverter.Input(
                membersCount = 1,
                signers = listOf(signer),
            ),
        ).signers.first()
    }

    private fun signer(answer: Answer, at: String?, name: String = "Isaac") = JointOperationSignerState(
        member = JointAccount.Member(name = name, address = "0xowner", role = JointAccount.Role.MEMBER),
        answer = answer,
        at = at,
    )

    private fun JointOperationSignersUM.SignerUM.statusResId(): Int? = (statusText as? TextReference.Res)?.id

    private companion object {
        const val AT = "2026-09-01T12:00:00Z"
    }
}