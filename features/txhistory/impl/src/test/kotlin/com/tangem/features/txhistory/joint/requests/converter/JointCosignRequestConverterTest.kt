package com.tangem.features.txhistory.joint.requests.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.features.txhistory.converter.toPortfolioAvatar
import com.tangem.features.txhistory.joint.requests.model.JointCosignRequest
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class JointCosignRequestConverterTest {

    private val currency = MockCryptoCurrencyFactory().ethereum

    private val converter = JointCosignRequestConverter(currency = currency, onClick = {})

    @Test
    fun `GIVEN the recipient is an own account WHEN converted THEN the row names it with its icon`() {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 0)
        val name = account.accountName.toUM().value

        // Act
        val actual = converter.convert(request(recipient = ResolvedOwner.OwnAccount(account)))

        // Assert
        assertThat((actual as TransactionItemUM.Content).subtitle).isEqualTo(
            ContentSubtitle.OwnAccount(
                direction = ContentSubtitle.Direction.TO,
                accountName = name,
                avatar = account.icon.toPortfolioAvatar(name = name),
            ),
        )
    }

    @Test
    fun `GIVEN an external recipient WHEN converted THEN the row shows its address`() {
        // Act
        val actual = converter.convert(request(recipient = ResolvedOwner.External(RECIPIENT)))

        // Assert
        assertThat((actual as TransactionItemUM.Content).subtitle).isEqualTo(
            ContentSubtitle.ExternalAddress(direction = ContentSubtitle.Direction.TO, rawAddress = RECIPIENT),
        )
    }

    private fun request(recipient: ResolvedOwner) = JointCosignRequest(
        operation = JointOperation(
            operationId = "operation-1",
            chainId = 1L,
            safeTx = SafeTx(
                to = RECIPIENT,
                value = BigInteger.TEN,
                data = "0x",
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = BigInteger.ZERO,
                baseGas = BigInteger.ZERO,
                gasPrice = BigInteger.ZERO,
                gasToken = ZERO_ADDRESS,
                refundReceiver = ZERO_ADDRESS,
                nonce = BigInteger.ZERO,
            ),
            status = JointOperation.Status.AWAITING_SIGNATURES,
            initiator = null,
            signatures = emptyList(),
        ),
        transfer = JointOperationTransfer(
            currency = currency,
            intent = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = BigInteger.TEN),
        ),
        recipient = recipient,
    )

    private companion object {
        const val RECIPIENT = "0xrecipient"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    }
}