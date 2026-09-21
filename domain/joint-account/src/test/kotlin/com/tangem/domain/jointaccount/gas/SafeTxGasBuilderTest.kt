package com.tangem.domain.jointaccount.gas

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.SafeInnerCall
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.assertEitherLeft
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SafeTxGasBuilderTest {

    private val networkReader: JointNetworkReader = mockk()
    private val network: Network = mockk()
    private val userWalletId = UserWalletId("0011223344556677")

    private val builder = SafeTxGasBuilder(
        networkReader = networkReader,
        refundReceiverProvider = { TREASURY },
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(networkReader)
        coEvery { networkReader.getCode(userWalletId, network) } returns DEPLOYED_CODE.right()
        coEvery { networkReader.getContractNonce(userWalletId, network) } returns BigInteger.valueOf(5).right()
        coEvery {
            networkReader.estimateGasLimit(userWalletId, network, to = any(), value = any(), data = any())
        } returns BigInteger.valueOf(100_000).right()
        coEvery { networkReader.getGasPrice(userWalletId, network) } returns GAS_PRICE_ESTIMATE.right()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun build(model: BuildModel) = runTest {
        // Arrange
        coEvery { networkReader.getCode(userWalletId, network) } returns model.code.right()

        // Act
        val actual = builder.build(userWalletId = userWalletId, network = network, call = CALL)

        // Assert
        val expected = SafeTx(
            to = CALL.to,
            value = CALL.value,
            data = CALL.data,
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger.valueOf(150_000),
            baseGas = model.expectedBaseGas,
            gasPrice = EXPECTED_GAS_PRICE,
            gasToken = SafeTxGasBuilder.NATIVE_GAS_TOKEN,
            refundReceiver = TREASURY,
            nonce = model.expectedNonce,
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    internal data class BuildModel(
        val code: String,
        val expectedBaseGas: BigInteger,
        val expectedNonce: BigInteger,
    )

    private fun provideTestModels() = listOf(
        // A deployed Safe: the contract's own nonce, no deployment gas
        BuildModel(
            code = DEPLOYED_CODE,
            expectedBaseGas = BigInteger.valueOf(110_000),
            expectedNonce = BigInteger.valueOf(5),
        ),
        // An undeployed Safe: nonce 0, the first operation also pays the deployment
        BuildModel(
            code = "0x",
            expectedBaseGas = BigInteger.valueOf(410_000),
            expectedNonce = BigInteger.ZERO,
        ),
    )

    @Test
    fun `GIVEN undeployed safe WHEN build THEN the contract nonce is never requested`() = runTest {
        // Arrange
        coEvery { networkReader.getCode(userWalletId, network) } returns "0x".right()

        // Act
        builder.build(userWalletId = userWalletId, network = network, call = CALL)

        // Assert
        coVerify(exactly = 0) { networkReader.getContractNonce(any(), any()) }
    }

    @Test
    fun `GIVEN odd estimate WHEN build THEN the safe tx gas margin is rounded up`() = runTest {
        // Arrange
        coEvery {
            networkReader.estimateGasLimit(userWalletId, network, to = any(), value = any(), data = any())
        } returns BigInteger.valueOf(100_001).right()

        // Act
        val actual = builder.build(userWalletId = userWalletId, network = network, call = CALL)

        // Assert
        assertThat(actual.getOrNull()?.safeTxGas).isEqualTo(BigInteger.valueOf(150_002))
    }

    @Test
    fun `GIVEN estimate fails WHEN build THEN the failure is returned`() = runTest {
        // Arrange
        val cause = IllegalStateException("node down")
        coEvery {
            networkReader.estimateGasLimit(userWalletId, network, to = any(), value = any(), data = any())
        } returns cause.left()

        // Act
        val actual = builder.build(userWalletId = userWalletId, network = network, call = CALL)

        // Assert
        assertEitherLeft(actual, cause)
    }

    @Test
    fun `GIVEN a checksummed fee recipient WHEN build THEN the refund receiver is lowercased`() = runTest {
        // Arrange
        val checksummedBuilder = SafeTxGasBuilder(
            networkReader = networkReader,
            refundReceiverProvider = { "0xFC719364BCCDC92D055D8C3164EF1AB4F5A9182C" },
        )

        // Act
        val actual = checksummedBuilder.build(userWalletId = userWalletId, network = network, call = CALL)

        // Assert
        assertThat(actual.getOrNull()?.refundReceiver).isEqualTo(TREASURY)
    }

    @Test
    fun `GIVEN a fee recipient that is not an address WHEN build THEN the failure is returned before any tap`() =
        runTest {
            // Arrange
            val invalidBuilder = SafeTxGasBuilder(
                networkReader = networkReader,
                refundReceiverProvider = { "not-an-address" },
            )

            // Act
            val actual = invalidBuilder.build(userWalletId = userWalletId, network = network, call = CALL)

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(IllegalStateException::class.java)
        }

    @Test
    fun `GIVEN the fee recipient endpoint fails WHEN build THEN the failure is returned before any tap`() = runTest {
        // Arrange
        val cause = IllegalStateException("Gasless service returned unsuccessful response")
        val failingBuilder = SafeTxGasBuilder(
            networkReader = networkReader,
            refundReceiverProvider = { throw cause },
        )

        // Act
        val actual = failingBuilder.build(userWalletId = userWalletId, network = network, call = CALL)

        // Assert
        assertEitherLeft(actual, cause)
    }

    @Test
    fun `GIVEN inner call WHEN build THEN the estimate is requested from the safe with the call fields`() = runTest {
        // Act
        builder.build(userWalletId = userWalletId, network = network, call = CALL)

        // Assert
        coVerify(exactly = 1) {
            networkReader.estimateGasLimit(
                userWalletId = userWalletId,
                network = network,
                to = CALL.to,
                value = CALL.value,
                data = CALL.data,
            )
        }
    }

    private companion object {
        val CALL = SafeInnerCall(
            to = "0x1111111111111111111111111111111111111111",
            value = BigInteger("1000000000000000000"),
            data = "0x",
        )
        val GAS_PRICE_ESTIMATE: BigInteger = BigInteger.valueOf(30_000_000_000)

        /** +100 % of the estimate, by requirement */
        val EXPECTED_GAS_PRICE: BigInteger = BigInteger.valueOf(60_000_000_000)
        const val TREASURY = "0xfc719364bccdc92d055d8c3164ef1ab4f5a9182c"
        const val DEPLOYED_CODE = "0x6080604052"
    }
}