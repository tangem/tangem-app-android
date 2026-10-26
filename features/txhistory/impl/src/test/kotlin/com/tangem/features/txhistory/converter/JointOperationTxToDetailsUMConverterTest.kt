package com.tangem.features.txhistory.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.resolveCounterparty
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class JointOperationTxToDetailsUMConverterTest : TxDetailsConverterTestBase() {

    private val converter by lazy {
        JointOperationTxToDetailsUMConverter(
            currency = currency,
            onCopyAddress = copiedAddresses::add,
            menu = persistentListOf(),
            isBalanceHidden = false,
            resolveRecipient = { address ->
                lookupOf().resolveCounterparty(address = address, networkRawId = currency.network.id.rawId)
            },
            fiatConversion = FiatConversion.NONE,
        )
    }

    @Test
    fun `GIVEN failed operation carrying its relayer tx WHEN convert THEN the refund paid is the network fee`() {
        // Arrange
        val relayerTx = TxInfo.MultiTransfer(
            amount = BigDecimal.ONE,
            isOutgoing = true,
            type = TransactionType.Transfer,
            destinationType = TxInfo.DestinationType.Single(TxInfo.AddressType.User(USER_ADDRESS)),
            sourceType = TxInfo.SourceType.Single(USER_ADDRESS),
            interactionAddressType = null,
            transfers = listOf(
                txInfo(type = TransactionType.Transfer, isOutgoing = true, status = TxInfo.TransactionStatus.Failed),
                txInfo(type = TransactionType.GaslessFee, isOutgoing = true, amount = BigDecimal("0.002")),
            ),
        )
        val operation = jointOperation(FinishedJointOperation.Status.FAILED)
            .copy(txInfo = OnChainTx.BSDK(relayerTx))

        // Act
        val rows = converter.convert(operation).rows

        // Assert
        val feeRow = rows.single()
        assertThat(feeRow.label).isEqualTo(resourceReference(R.string.common_network_fee_title))
        assertThat(feeRow.value.resolveString()).contains("0.002")
    }

    @Test
    fun `GIVEN cancelled operation WHEN convert THEN no fee row`() {
        // Act
        val rows = converter.convert(jointOperation(FinishedJointOperation.Status.CANCELLED)).rows

        // Assert
        assertThat(rows).isEmpty()
    }

    private fun jointOperation(status: FinishedJointOperation.Status) = JointOperationTx(
        operation = FinishedJointOperation(
            operationId = "operation-1",
            chainId = 1,
            safeAddress = "0xsafe",
            safeTx = SafeTx(
                to = USER_ADDRESS,
                value = BigInteger.TEN.pow(18),
                data = "0x",
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = BigInteger.ONE,
                baseGas = BigInteger.ONE,
                gasPrice = BigInteger.ONE,
                gasToken = "0x0",
                refundReceiver = "0xrelayer",
                nonce = BigInteger.ONE,
            ),
            status = status,
            initiator = USER_ADDRESS,
            signatures = emptyList(),
            createdAt = "2026-09-08T09:20:37.000Z",
            finishedAtMillis = TIMESTAMP,
            txHash = TX_HASH.takeIf { status != FinishedJointOperation.Status.CANCELLED },
        ),
        transfer = JointOperationTransfer(
            currency = currency,
            intent = JointTransferIntent.Native(recipient = USER_ADDRESS, amountWei = BigInteger.TEN.pow(18)),
        ),
        txInfo = null,
    )
}