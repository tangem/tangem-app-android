package com.tangem.data.jointaccount.safe

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.domain.jointaccount.network.JointNetworkReader
import com.tangem.domain.jointaccount.safe.SafeCompositionChecker
import com.tangem.domain.jointaccount.safe.SafeDeployment
import com.tangem.domain.jointaccount.safe.SafeSingleton
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SafeCompositionCheckerTest {

    private val networkReader: JointNetworkReader = mockk()
    private val network: Network = mockk()
    private val userWalletId = UserWalletId("0011223344556677")

    private val checker = SafeCompositionChecker(networkReader = networkReader, safeAddressCalculator = DefaultSafeAddressCalculator())

    @ParameterizedTest
    @ProvideTestModels
    fun deployed(model: DeployedModel) = runTest {
        // Arrange
        clearMocks(networkReader)
        coEvery { networkReader.getCode(userWalletId, network) } returns DEPLOYED_CODE.right()
        coEvery {
            networkReader.callSafeContract(userWalletId, network, data = GET_OWNERS_SELECTOR)
        } returns encodeAddressArray(model.onChainOwners).right()
        coEvery {
            networkReader.callSafeContract(userWalletId, network, data = GET_THRESHOLD_SELECTOR)
        } returns encodeUint(model.onChainThreshold).right()

        // Act
        val actual = checker.ensureMatches(
            userWalletId = userWalletId,
            network = network,
            safeAddress = COMPUTED_SAFE,
            owners = listOf(ALICE, BOB),
            threshold = 2,
        )

        // Assert
        assertThat(actual.isRight()).isEqualTo(model.expectedMatch)
        if (!model.expectedMatch) {
            assertThat(actual.leftOrNull()).isEqualTo(JointOperationCreateError.CompositionMismatch)
        }
    }

    internal data class DeployedModel(
        val onChainOwners: List<String>,
        val onChainThreshold: Int,
        val expectedMatch: Boolean,
    )

    private fun provideTestModels() = listOf(
        DeployedModel(onChainOwners = listOf(ALICE, BOB), onChainThreshold = 2, expectedMatch = true),
        // getOwners answers in the Safe's linked-list order — the comparison must not depend on it
        DeployedModel(onChainOwners = listOf(BOB, ALICE), onChainThreshold = 2, expectedMatch = true),
        DeployedModel(onChainOwners = listOf(ALICE, MALLORY), onChainThreshold = 2, expectedMatch = false),
        DeployedModel(onChainOwners = listOf(ALICE, BOB, MALLORY), onChainThreshold = 2, expectedMatch = false),
        DeployedModel(onChainOwners = listOf(ALICE), onChainThreshold = 2, expectedMatch = false),
        DeployedModel(onChainOwners = listOf(ALICE, BOB), onChainThreshold = 1, expectedMatch = false),
    )

    @Test
    fun `GIVEN deployed safe at a foreign address WHEN ensureMatches THEN mismatch blocks before the chain is read`() =
        runTest {
            // Arrange
            clearMocks(networkReader)

            // Act
            val actual = checker.ensureMatches(
                userWalletId = userWalletId,
                network = network,
                safeAddress = SAFE_ADDRESS,
                owners = listOf(ALICE, BOB),
                threshold = 2,
            )

            // Assert
            assertThat(actual.leftOrNull()).isEqualTo(JointOperationCreateError.CompositionMismatch)
            coVerify(exactly = 0) { networkReader.getCode(any(), any()) }
        }

    @Test
    fun `GIVEN an unreadable getOwners answer WHEN ensureMatches THEN Failed instead of a crash`() = runTest {
        // Arrange
        clearMocks(networkReader)
        coEvery { networkReader.getCode(userWalletId, network) } returns DEPLOYED_CODE.right()
        coEvery {
            networkReader.callSafeContract(userWalletId, network, data = GET_OWNERS_SELECTOR)
        } returns ("0x" + "0".repeat(64) + "zz".padStart(length = 64, padChar = '0')).right()

        // Act
        val actual = checker.ensureMatches(
            userWalletId = userWalletId,
            network = network,
            safeAddress = COMPUTED_SAFE,
            owners = listOf(ALICE, BOB),
            threshold = 2,
        )

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointOperationCreateError.Failed::class.java)
    }

    @Test
    fun `GIVEN an empty getOwners answer WHEN ensureMatches THEN Failed instead of a mismatch`() = runTest {
        // Arrange
        clearMocks(networkReader)
        coEvery { networkReader.getCode(userWalletId, network) } returns DEPLOYED_CODE.right()
        coEvery {
            networkReader.callSafeContract(userWalletId, network, data = GET_OWNERS_SELECTOR)
        } returns "0x".right()

        // Act
        val actual = checker.ensureMatches(
            userWalletId = userWalletId,
            network = network,
            safeAddress = COMPUTED_SAFE,
            owners = listOf(ALICE, BOB),
            threshold = 2,
        )

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointOperationCreateError.Failed::class.java)
    }

    @Test
    fun `GIVEN a composition that cannot yield an address WHEN ensureMatches THEN Failed, not a mismatch`() = runTest {
        // Arrange
        clearMocks(networkReader)

        // Act
        val actual = checker.ensureMatches(
            userWalletId = userWalletId,
            network = network,
            safeAddress = COMPUTED_SAFE,
            owners = listOf(ALICE, ALICE),
            threshold = 2,
        )

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(JointOperationCreateError.Failed::class.java)
        coVerify(exactly = 0) { networkReader.getCode(any(), any()) }
    }

    @Test
    fun `GIVEN undeployed safe with matching address WHEN ensureMatches THEN composition confirmed`() = runTest {
        // Arrange
        clearMocks(networkReader)
        coEvery { networkReader.getCode(userWalletId, network) } returns "0x".right()

        // Act
        val actual = checker.ensureMatches(
            userWalletId = userWalletId,
            network = network,
            safeAddress = COMPUTED_SAFE.lowercase(),
            owners = listOf(ALICE, BOB),
            threshold = 2,
        )

        // Assert
        assertThat(actual).isEqualTo(SafeDeployment.NOT_DEPLOYED.right())
    }

    @Test
    fun `GIVEN undeployed safe with foreign address WHEN ensureMatches THEN mismatch blocks`() = runTest {
        // Arrange
        clearMocks(networkReader)
        coEvery { networkReader.getCode(userWalletId, network) } returns "0x".right()

        // Act
        val actual = checker.ensureMatches(
            userWalletId = userWalletId,
            network = network,
            safeAddress = SAFE_ADDRESS,
            owners = listOf(ALICE, BOB),
            threshold = 2,
        )

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(JointOperationCreateError.CompositionMismatch)
    }

    private fun encodeAddressArray(addresses: List<String>): String {
        return "0x" +
            BigInteger.valueOf(32).toWordHex() +
            BigInteger.valueOf(addresses.size.toLong()).toWordHex() +
            addresses.joinToString(separator = "") { address ->
                address.removePrefix(prefix = "0x").lowercase().padStart(length = 64, padChar = '0')
            }
    }

    private fun encodeUint(value: Int): String = "0x" + BigInteger.valueOf(value.toLong()).toWordHex()

    private fun BigInteger.toWordHex(): String = toString(16).padStart(length = 64, padChar = '0')

    private companion object {
        const val ALICE = "0x7e5f4552091a69125d5DfCb7b8C2659029395Bdf"
        const val BOB = "0x2B5AD5c4795c026514f8317c7a215E218DcCD6cF"
        const val MALLORY = "0x3333333333333333333333333333333333333333"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val DEPLOYED_CODE = "0x6080604052"
        const val GET_OWNERS_SELECTOR = "0xa0e67e2b"
        const val GET_THRESHOLD_SELECTOR = "0xe75235b8"

        val COMPUTED_SAFE: String = DefaultSafeAddressCalculator()
            .compute(owners = listOf(ALICE, BOB), threshold = 2, singleton = SafeSingleton.SAFE_L2)
            .getOrNull()!!
    }
}