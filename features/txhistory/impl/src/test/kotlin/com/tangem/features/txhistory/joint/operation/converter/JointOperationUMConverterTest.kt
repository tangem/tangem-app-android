package com.tangem.features.txhistory.joint.operation.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.features.txhistory.joint.operation.model.JointOperationState
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import java.math.BigInteger
import com.tangem.core.ui.R as CoreUiR

internal class JointOperationUMConverterTest {

    private val converter = JointOperationUMConverter(
        currency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum),
        onSignClick = {},
        onSignersClick = {},
        onCopyRecipientClick = {},
        onCreateContactClick = {},
        onRefuseClick = {},
    )

    @Test
    fun `GIVEN operation awaits signatures WHEN converted THEN it is a pending send with the refusal in the menu`() {
        // Act
        val actual = converter.convert(input(state(isSending = false)))

        // Assert
        assertThat(actual.header.status).isEqualTo(TransactionItemUM.Content.Status.Confirmed)
        assertThat(actual.header.subtitle).isEqualTo(resourceReference(CoreUiR.string.transaction_history_pending))
        assertThat(actual.header.menu.map { it.title }).contains(resourceReference(CoreUiR.string.common_decline))
        assertThat(actual.cosign).isNotNull()
        assertThat(actual.cosignAction?.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN operation is on its way to the network WHEN converted THEN nothing is left to sign or refuse`() {
        // Act
        val actual = converter.convert(input(state(isSending = true)))

        // Assert
        assertThat(actual.header.status).isEqualTo(TransactionItemUM.Content.Status.Unconfirmed)
        assertThat(actual.header.title).isEqualTo(resourceReference(CoreUiR.string.common_sending))
        assertThat(actual.header.menu.map { it.title }).containsExactly(
            resourceReference(CoreUiR.string.common_copy_address),
            resourceReference(CoreUiR.string.address_book_add_contact),
        )
        assertThat(actual.cosign).isNull()
        assertThat(actual.cosignAction?.isEnabled).isFalse()
        assertThat(actual.cosignAction?.buttonText).isEqualTo(resourceReference(CoreUiR.string.common_sign))
    }

    @Test
    fun `GIVEN this member signed WHEN the operation is sent THEN the button says signed`() {
        // Act
        val actual = converter.convert(input(state(isSending = true, ownAnswer = Answer.SIGNED)))

        // Assert
        assertThat(actual.cosignAction?.buttonText).isEqualTo(resourceReference(CoreUiR.string.joint_operation_signed))
    }

    @Test
    fun `GIVEN this member declined WHEN the operation is sent THEN the button does not say signed`() {
        // Arrange
        val declinedLocally = input(state(isSending = true, ownAnswer = Answer.DECLINED), hasAnswered = true)

        // Act
        val actual = converter.convert(declinedLocally)

        // Assert
        assertThat(actual.cosignAction?.buttonText).isEqualTo(resourceReference(CoreUiR.string.common_sign))
    }

    @Test
    fun `GIVEN cold wallet can sign WHEN converted THEN the sign button carries the card icon`() {
        // Act
        val actual = converter.convert(input(state(isSending = false), isColdWallet = true))

        // Assert
        assertThat(actual.cosignAction?.iconEnd).isNotNull()
    }

    @Test
    fun `GIVEN hot wallet can sign WHEN converted THEN the sign button has no icon`() {
        // Act
        val actual = converter.convert(input(state(isSending = false), isColdWallet = false))

        // Assert
        assertThat(actual.cosignAction?.isEnabled).isTrue()
        assertThat(actual.cosignAction?.iconEnd).isNull()
    }

    private fun input(
        state: JointOperationState,
        hasAnswered: Boolean = false,
        isColdWallet: Boolean = true,
    ) = JointOperationUMConverter.Input(
        state = state,
        isSigning = false,
        isRefusing = false,
        hasAnswered = hasAnswered,
        canCreateContact = true,
        isBalanceHidden = false,
        isColdWallet = isColdWallet,
    )

    private fun state(isSending: Boolean, ownAnswer: Answer = Answer.WAITING) = JointOperationState(
        account = MockAccounts.createJointAccount(),
        displayed = JointTransferIntent.Native(recipient = OTHER, amountWei = BigInteger.TEN.pow(18)),
        feeWei = BigInteger.ZERO,
        feeCoin = null,
        threshold = 2,
        membersCount = 3,
        signers = listOf(
            signer(address = OTHER, answer = Answer.CREATED),
            signer(address = OWN, answer = ownAnswer),
        ),
        canSign = !isSending && ownAnswer == Answer.WAITING,
        ownAddress = OWN,
        initiator = OTHER,
        isSending = isSending,
    )

    private fun signer(address: String, answer: Answer) = JointOperationSignerState(
        member = JointAccount.Member(name = "Isaac", address = address, role = JointAccount.Role.MEMBER),
        answer = answer,
        at = null,
    )

    private companion object {
        const val OWN = "0xowner"
        const val OTHER = "0xother"
    }
}