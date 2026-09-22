package com.tangem.data.jointaccount.blockaid

import arrow.core.left
import arrow.core.right
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.domain.blockaid.models.transaction.ValidationResult
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.data.jointaccount.safe.SafeTxTypedDataBuilder
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointOperationBlockaidGateTest {

    private val blockAidVerifier: BlockAidVerifier = mockk()
    private val network: Network = mockk()

    private val gate = DefaultJointOperationBlockaidGate(
        blockAidVerifier = blockAidVerifier,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(blockAidVerifier, network)
        every { network.rawId } returns "polygon-pos"
    }

    @ParameterizedTest
    @ProvideTestModels
    fun verdicts(model: VerdictModel) = runTest {
        // Arrange
        coEvery { blockAidVerifier.verifyTransaction(data = any()) } returns
            CheckTransactionResult(
                validation = model.validation,
                simulation = SimulationResult.FailedToSimulate,
            ).right()

        // Act
        val actual = gate.verify(
            network = network,
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
            ownerAddress = OWNER,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class VerdictModel(
        val validation: ValidationResult,
        val expected: JointOperationBlockaidGate.Verdict,
    )

    private fun provideTestModels() = listOf(
        VerdictModel(ValidationResult.SAFE, JointOperationBlockaidGate.Verdict.SAFE),
        VerdictModel(ValidationResult.WARNING, JointOperationBlockaidGate.Verdict.WARNING),
        VerdictModel(ValidationResult.UNSAFE, JointOperationBlockaidGate.Verdict.MALICIOUS),
        VerdictModel(ValidationResult.FAILED_TO_VALIDATE, JointOperationBlockaidGate.Verdict.UNKNOWN),
    )

    @Test
    fun `GIVEN a joint operation WHEN verify THEN blockaid receives the exact signed payload`() = runTest {
        // Arrange
        val dataSlot = slot<TransactionData>()
        coEvery { blockAidVerifier.verifyTransaction(data = capture(dataSlot)) } returns
            CheckTransactionResult(
                validation = ValidationResult.SAFE,
                simulation = SimulationResult.FailedToSimulate,
            ).right()

        // Act
        gate.verify(
            network = network,
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
            ownerAddress = OWNER,
        )

        // Assert
        val captured = dataSlot.captured
        assertThat(captured.chain).isEqualTo("polygon")
        assertThat(captured.accountAddress).isEqualTo(OWNER)
        assertThat(captured.method).isEqualTo("eth_signTypedData_v4")
        val typedDataJson = SafeTxTypedDataBuilder.buildJson(SAFE_TX, CHAIN_ID, SAFE_ADDRESS)
        val params = (captured.params as TransactionParams.Evm).params
        assertThat(params).contains(OWNER)
        // The typed data rides as one escaped JSON string element — the same bytes the digest is built from
        assertThat(params).contains(typedDataJson.replace("\"", "\\\""))
    }

    @Test
    fun `GIVEN blockaid transport fails WHEN verify THEN UNAVAILABLE, apart from a verdict it could not give`() = runTest {
        // Arrange
        coEvery { blockAidVerifier.verifyTransaction(data = any()) } returns
            IllegalStateException("blockaid down").left()

        // Act
        val actual = gate.verify(
            network = network,
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
            ownerAddress = OWNER,
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationBlockaidGate.Verdict.UNAVAILABLE)
    }

    @Test
    fun `GIVEN a payload the builder refuses WHEN verify THEN UNKNOWN without a request`() = runTest {
        // Act
        val actual = gate.verify(
            network = network,
            safeTx = SAFE_TX.copy(data = "0xabc"),
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
            ownerAddress = OWNER,
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationBlockaidGate.Verdict.UNKNOWN)
        coVerify(exactly = 0) { blockAidVerifier.verifyTransaction(data = any()) }
    }

    @Test
    fun `GIVEN a chain blockaid does not cover WHEN verify THEN UNKNOWN without a request`() = runTest {
        // Arrange
        every { network.rawId } returns "some-unknown-network"

        // Act
        val actual = gate.verify(
            network = network,
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
            ownerAddress = OWNER,
        )

        // Assert
        assertThat(actual).isEqualTo(JointOperationBlockaidGate.Verdict.UNKNOWN)
        coVerify(exactly = 0) { blockAidVerifier.verifyTransaction(data = any()) }
    }

    private companion object {
        const val CHAIN_ID = 137L
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val OWNER = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"

        val SAFE_TX = SafeTx(
            to = "0x1111111111111111111111111111111111111111",
            value = BigInteger("1000000000000000000"),
            data = "0x",
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger("150000"),
            baseGas = BigInteger("110000"),
            gasPrice = BigInteger("60000000000"),
            gasToken = "0x0000000000000000000000000000000000000000",
            refundReceiver = "0x0000000000000000000000000000000000000000",
            nonce = BigInteger.ZERO,
        )
    }
}