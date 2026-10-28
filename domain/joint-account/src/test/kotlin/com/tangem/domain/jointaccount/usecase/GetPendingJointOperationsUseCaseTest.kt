package com.tangem.domain.jointaccount.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.address.JointOwnerAddressResolver
import com.tangem.domain.jointaccount.gas.SafeTxGasBuilder
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationSignerState
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.JointOperationSignersResolver
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.jointaccount.verifier.JointOperationDisplayGate
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class GetPendingJointOperationsUseCaseTest {

    private val jointAccountListSupplier: SingleJointAccountListSupplier = mockk()
    private val transferResolver: JointOperationTransferResolver = mockk()
    private val signersResolver: JointOperationSignersResolver = mockk()
    private val ownerAddressResolver: JointOwnerAddressResolver = mockk()
    private val networkReader: JointNetworkReader = mockk()
    private val displayGate: JointOperationDisplayGate = mockk()

    private val network: Network = mockk()
    private val coin: CryptoCurrency.Coin = mockk {
        every { this@mockk.network } returns this@GetPendingJointOperationsUseCaseTest.network
    }
    private val account = MockAccounts.createJointAccount(ownerKeyIndex = 0, cryptoCurrencies = listOf(coin))
    private val jointAccount = JointAccount(
        cryptoAccountId = account.accountId.value,
        membersCount = 2,
        threshold = 2,
        safeAddress = SAFE_ADDRESS,
        status = JointAccount.Status.ACTIVE,
        members = listOf(ALICE_MEMBER, BOB_MEMBER),
        source = StatusSource.ACTUAL,
        inFlightOperations = mapOf(CHAIN_ID to OPERATION),
    )
    private val transfer = JointOperationTransfer(currency = coin, intent = DISPLAYED)

    private val useCase = GetPendingJointOperationsUseCase(
        jointAccountListSupplier = jointAccountListSupplier,
        transferResolver = transferResolver,
        signersResolver = signersResolver,
        ownerAddressResolver = ownerAddressResolver,
        networkReader = networkReader,
        displayGate = displayGate,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            jointAccountListSupplier,
            transferResolver,
            signersResolver,
            ownerAddressResolver,
            networkReader,
            displayGate,
        )
        every { jointAccountListSupplier(userWalletId = account.userWalletId) } returns flowOf(listOf(jointAccount))
        every {
            transferResolver.resolve(safeTx = any(), currencies = any(), network = network, safeAddress = SAFE_ADDRESS)
        } returns transfer
        every { signersResolver.signers(jointAccount = jointAccount, operation = OPERATION) } returns SIGNERS
        coEvery { ownerAddressResolver.resolve(userWalletId = any(), ownerKeyIndex = any()) } returns ALICE.right()
        coEvery { networkReader.getChainId(network = network) } returns CHAIN_ID.right()
        coEvery { displayGate.isShown(any(), any(), any(), any(), any()) } returns true
    }

    @Test
    fun `GIVEN the operation passes the display gate WHEN invoke THEN it is listed`() = runTest {
        // Act
        val actual = useCase(userWalletId = account.userWalletId, accounts = listOf(account)).first()

        // Assert
        val expected = JointPendingOperation(
            account = account,
            operationId = OPERATION.operationId,
            transfer = transfer,
            initiatorName = ALICE_MEMBER.name,
            initiatorAddress = ALICE,
            isOwn = true,
            ownAnswer = JointOperationSignerState.Answer.CREATED,
            collected = 1,
            threshold = 2,
        )
        assertThat(actual).containsExactly(expected)
        coVerify(exactly = 1) {
            displayGate.isShown(
                account = account,
                jointAccount = jointAccount,
                operation = OPERATION,
                network = network,
                transfer = transfer,
            )
        }
    }

    @Test
    fun `GIVEN the operation fails the display gate WHEN invoke THEN nothing is listed`() = runTest {
        // Arrange
        coEvery { displayGate.isShown(any(), any(), any(), any(), any()) } returns false

        // Act
        val actual = useCase(userWalletId = account.userWalletId, accounts = listOf(account)).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    private companion object {
        const val CHAIN_ID = 137L
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val BOB = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        val AMOUNT: BigInteger = BigInteger("1000000000000000000")
        val DISPLAYED = JointTransferIntent.Native(recipient = RECIPIENT, amountWei = AMOUNT)
        val ALICE_MEMBER = JointAccount.Member(name = "Alice", address = ALICE, role = JointAccount.Role.CREATOR)
        val BOB_MEMBER = JointAccount.Member(name = "Bob", address = BOB, role = JointAccount.Role.MEMBER)
        val SIGNERS = listOf(
            JointOperationSignerState(
                member = ALICE_MEMBER,
                answer = JointOperationSignerState.Answer.CREATED,
                at = "2026-09-07T10:00:00Z",
            ),
            JointOperationSignerState(member = BOB_MEMBER, answer = JointOperationSignerState.Answer.WAITING, at = null),
        )
        val OPERATION = JointOperation(
            operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
            chainId = CHAIN_ID,
            safeTx = SafeTx(
                to = RECIPIENT,
                value = AMOUNT,
                data = "0x",
                operation = SafeTx.OPERATION_CALL,
                safeTxGas = BigInteger("150000"),
                baseGas = SafeTxGasBuilder.SAFE_BASE_GAS,
                gasPrice = BigInteger("60000000000"),
                gasToken = SafeTxGasBuilder.NATIVE_GAS_TOKEN,
                refundReceiver = "0xfee0000000000000000000000000000000000001",
                nonce = BigInteger.ZERO,
            ),
            status = JointOperation.Status.AWAITING_SIGNATURES,
            initiator = ALICE,
            signatures = emptyList(),
        )
    }
}