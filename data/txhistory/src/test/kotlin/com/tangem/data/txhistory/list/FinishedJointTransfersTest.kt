package com.tangem.data.txhistory.list

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.repository.JointOperationHistoryRepository
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class FinishedJointTransfersTest {

    private val accountListSupplier: SingleAccountListSupplier = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val transferResolver: JointOperationTransferResolver = mockk()
    private val repository: JointOperationHistoryRepository = mockk()

    private val jointNetwork: Network = mockk {
        every { derivationPath } returns mockk<Network.DerivationPath.Joint>()
    }
    private val currency: CryptoCurrency.Coin = mockk { every { network } returns jointNetwork }
    private val account = MockAccounts.createJointAccount(cryptoCurrencies = listOf(currency))

    private val finishedJointTransfers = FinishedJointTransfers(
        accountListSupplier = accountListSupplier,
        networkReader = networkReader,
        transferResolver = transferResolver,
        repository = repository,
    )

    @BeforeEach
    fun setup() {
        clearMocks(accountListSupplier, networkReader, transferResolver, repository)
        every { accountListSupplier(MockAccounts.userWalletId) } returns flowOf(
            MockAccounts.createAccountList(activeAccounts = 1, jointAccounts = 0).let { list ->
                mockk { every { accounts } returns list.accounts + account }
            },
        )
        coEvery { networkReader.getChainId(jointNetwork) } returns CHAIN_ID.right()
    }

    @Test
    fun `GIVEN operations of the account WHEN observe THEN keeps only transfers of the viewed currency`() = runTest {
        // Arrange
        val transfer = createOperation(operationId = "transfer")
        val other = createOperation(operationId = "other")
        val resolved = JointOperationTransfer(
            currency = currency,
            intent = JointTransferIntent.Native(recipient = "0xrecipient", amountWei = BigInteger.ONE),
        )
        every { repository.getFinishedOperations(account.accountId, CHAIN_ID, fromFinishedAtMillis = 10) } returns
            flowOf(listOf(transfer, other))
        every { transferResolver.resolve(transfer.safeTx, listOf(currency), jointNetwork, "0xsafe") } returns resolved
        every { transferResolver.resolve(other.safeTx, listOf(currency), jointNetwork, "0xsafe") } returns null

        // Act
        val actual = finishedJointTransfers.observe(MockAccounts.userWalletId, currency, fromFinishedAtMillis = 10).first()

        // Assert
        assertThat(actual).containsExactly(JointOperationTx(operation = transfer, transfer = resolved, txInfo = null))
    }

    private fun createOperation(operationId: String) = FinishedJointOperation(
        operationId = operationId,
        chainId = CHAIN_ID,
        safeAddress = "0xsafe",
        safeTx = SafeTx(
            to = "0x$operationId",
            value = BigInteger.ONE,
            data = "0x",
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger.ONE,
            baseGas = BigInteger.ONE,
            gasPrice = BigInteger.ONE,
            gasToken = "0x0",
            refundReceiver = "0xrelayer",
            nonce = BigInteger.ONE,
        ),
        status = FinishedJointOperation.Status.CANCELLED,
        initiator = "0xalice",
        signatures = emptyList(),
        createdAt = "2026-09-08T09:20:37.000Z",
        finishedAtMillis = 20,
        txHash = null,
    )

    private companion object {
        const val CHAIN_ID = 8453L
    }
}