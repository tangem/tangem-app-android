package com.tangem.features.txhistory.joint.operation.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.cosign.SignatureProgressUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.joint.operation.model.JointOperationFiat
import com.tangem.features.txhistory.joint.operation.model.JointOperationState
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger
import com.tangem.core.ui.R as CoreUiR

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
        assertThat(actual.cosign).isNotNull()
        assertThat(actual.cosignAction?.isEnabled).isFalse()
        assertThat(actual.cosignAction?.caption).isNull()
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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN wallet type and operation state WHEN converted THEN only a hot wallet that can sign holds to confirm`(
        model: HoldToConfirmModel,
    ) {
        // Act
        val actual = converter.convert(model.input)

        // Assert
        assertThat(actual.cosignAction?.isHoldToConfirm).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN threshold below the roster WHEN converted THEN the signatures counter runs to the threshold`() {
        // Act
        val actual = converter.convert(input(state(isSending = false)))

        // Assert
        val row = actual.cosign!!
        assertThat(row.title).isEqualTo(
            resourceReference(CoreUiR.string.joint_operation_signatures_progress, wrappedList(1, 2)),
        )
        assertThat(row.progress).isEqualTo(SignatureProgressUM(collected = 1, total = 2))
    }

    @Test
    fun `GIVEN a rate WHEN converted THEN the amount is priced in the app currency and the fee is approximate`() {
        // Act
        val actual = converter.convert(input(state(isSending = false), fiatRate = BigDecimal("2000")))

        // Assert
        assertThat(actual.amountBlock.fiatAmount).isEqualTo(
            stringReference(BigDecimal("2000").format { fiat(fiatCurrencyCode = "USD", fiatCurrencySymbol = "$") }),
        )
        assertThat(actual.rows.single().value).isEqualTo(
            stringReference("~" + BigDecimal.ZERO.format { crypto(cryptoCurrency = feeCurrency, ignoreSymbolPosition = true) }),
        )
    }

    @Test
    fun `GIVEN a token refund WHEN converted THEN the fee is shown in the gas token`() {
        // Arrange
        val usdc = MockCryptoCurrencyFactory().createToken(Blockchain.Polygon).copy(symbol = "USDC", decimals = 6)

        // Act
        val actual = converter.convert(
            input(state(isSending = false, feeAmount = BigInteger.valueOf(200_000), feeCurrency = usdc)),
        )

        // Assert
        assertThat(actual.rows.single().value).isEqualTo(
            stringReference("~" + BigDecimal("0.2").format { crypto(cryptoCurrency = usdc, ignoreSymbolPosition = true) }),
        )
    }

    @Test
    fun `GIVEN a gas token outside the account WHEN converted THEN there is no fee row`() {
        // Act
        val actual = converter.convert(input(state(isSending = false, feeCurrency = null)))

        // Assert
        assertThat(actual.rows).isEmpty()
    }

    @Test
    fun `GIVEN zero rate WHEN converted THEN the amount has no fiat line`() {
        // Act
        val actual = converter.convert(input(state(isSending = false), fiatRate = BigDecimal.ZERO))

        // Assert
        assertThat(actual.amountBlock.fiatAmount).isNull()
    }

    @Test
    fun `GIVEN no rate WHEN converted THEN the amount has no fiat line`() {
        // Act
        val actual = converter.convert(input(state(isSending = false), fiatRate = null))

        // Assert
        assertThat(actual.amountBlock.fiatAmount).isNull()
    }

    @Test
    fun `GIVEN a long recipient address WHEN converted THEN the card shows it whole for the row to cut`() {
        // Arrange
        val state = state(isSending = false).copy(
            displayed = JointTransferIntent.Native(recipient = LONG_RECIPIENT, amountWei = BigInteger.TEN.pow(18)),
        )

        // Act
        val actual = converter.convert(input(state))

        // Assert
        assertThat(actual.counterparty?.title).isEqualTo(TxHistoryDetailsUM.CounterpartyTitle.Address(LONG_RECIPIENT))
    }

    data class HoldToConfirmModel(val description: String, val input: JointOperationUMConverter.Input, val expected: Boolean) {
        override fun toString() = description
    }

    private fun provideTestModels() = listOf(
        HoldToConfirmModel(
            description = "hot wallet can sign",
            input = input(state(isSending = false), isColdWallet = false),
            expected = true,
        ),
        HoldToConfirmModel(
            description = "cold wallet signs with a card tap",
            input = input(state(isSending = false), isColdWallet = true),
            expected = false,
        ),
        HoldToConfirmModel(
            description = "hot wallet already answered",
            input = input(state(isSending = false), hasAnswered = true, isColdWallet = false),
            expected = false,
        ),
        HoldToConfirmModel(
            description = "hot wallet, operation already on its way",
            input = input(state(isSending = true), isColdWallet = false),
            expected = false,
        ),
    )

    private fun input(
        state: JointOperationState,
        hasAnswered: Boolean = false,
        isColdWallet: Boolean = true,
        fiatRate: BigDecimal? = null,
    ) = JointOperationUMConverter.Input(
        state = state,
        isSigning = false,
        isRefusing = false,
        hasAnswered = hasAnswered,
        canCreateContact = true,
        isBalanceHidden = false,
        isColdWallet = isColdWallet,
        fiat = JointOperationFiat(rate = fiatRate, appCurrency = AppCurrency(name = "US Dollar", code = "USD", symbol = "$")),
    )

    private fun state(
        isSending: Boolean,
        ownAnswer: Answer = Answer.WAITING,
        feeAmount: BigInteger = BigInteger.ZERO,
        feeCurrency: CryptoCurrency? = this.feeCurrency,
    ) = JointOperationState(
        account = MockAccounts.createJointAccount(),
        displayed = JointTransferIntent.Native(recipient = OTHER, amountWei = BigInteger.TEN.pow(18)),
        feeAmount = feeAmount,
        feeCurrency = feeCurrency,
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

    private val feeCurrency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)

    private companion object {
        const val OWN = "0xowner"
        const val OTHER = "0xother"
        const val LONG_RECIPIENT = "0xaea826671a6bd1cc5b4b8e6a7f6a0e1d015c5ebb"
    }
}