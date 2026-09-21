package com.tangem.data.walletmanager

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.SafeEthereumWalletManager
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.Lazy
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

/**
 * Every read goes through the Safe wallet manager the facade keeps for the joint network; without one — the account
 * has no Safe address yet — the read fails as a state error rather than reaching the network.
 */
internal class DefaultJointNetworkReaderTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val manager: SafeEthereumWalletManager = mockk()

    private val reader = DefaultJointNetworkReader(walletManagersFacade = Lazy { walletManagersFacade })

    private val userWalletId = UserWalletId("0011223344556677")
    private val network: Network = MockCryptoCurrencyFactory().ethereum.network

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, manager)
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns manager
    }

    @Test
    fun `GIVEN ethereum WHEN getChainId THEN the EIP-155 id is returned`() = runTest {
        // Act
        val actual = reader.getChainId(network = network)

        // Assert
        assertThat(actual).isEqualTo(1L.right())
    }

    @Test
    fun `GIVEN a Safe manager WHEN getCode THEN its answer is Right`() = runTest {
        // Arrange
        coEvery { manager.getCode() } returns Result.Success("0x6080")

        // Act
        val actual = reader.getCode(userWalletId = userWalletId, network = network)

        // Assert
        assertThat(actual).isEqualTo("0x6080".right())
    }

    @Test
    fun `GIVEN the manager fails WHEN reading THEN the SDK error becomes Left`() = runTest {
        // Arrange
        coEvery { manager.getContractNonce() } returns Result.Failure(BlockchainSdkError.FailedToLoadFee)

        // Act
        val actual = reader.getContractNonce(userWalletId = userWalletId, network = network)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(BlockchainSdkError.FailedToLoadFee)
    }

    @Test
    fun `GIVEN a native transfer WHEN estimating gas THEN the value is hex and empty calldata is dropped`() = runTest {
        // Arrange
        coEvery {
            manager.estimateGasLimit(to = RECIPIENT, value = "0xde0b6b3a7640000", data = null)
        } returns Result.Success(BigInteger.valueOf(21_000))

        // Act
        val actual = reader.estimateGasLimit(
            userWalletId = userWalletId,
            network = network,
            to = RECIPIENT,
            value = BigInteger("1000000000000000000"),
            data = "0x",
        )

        // Assert
        assertThat(actual).isEqualTo(BigInteger.valueOf(21_000).right())
    }

    @Test
    fun `GIVEN a Safe method WHEN calling the safe contract THEN the manager calls the Safe itself`() = runTest {
        // Arrange
        coEvery { manager.callSafeContract(data = GET_OWNERS_SELECTOR) } returns Result.Success("0x00")

        // Act
        val actual = reader.callSafeContract(userWalletId = userWalletId, network = network, data = GET_OWNERS_SELECTOR)

        // Assert
        assertThat(actual).isEqualTo("0x00".right())
    }

    @Test
    fun `GIVEN no manager yet WHEN reading THEN Left with a state error`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns null

        // Act
        val actual = reader.getCode(userWalletId = userWalletId, network = network)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `GIVEN a manager that is not a Safe one WHEN reading THEN Left with a state error`() = runTest {
        // Arrange
        coEvery { walletManagersFacade.getOrCreateWalletManager(userWalletId, network) } returns mockk<WalletManager>()

        // Act
        val actual = reader.getGasPrice(userWalletId = userWalletId, network = network)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    private companion object {
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val GET_OWNERS_SELECTOR = "0xa0e67e2b"
    }
}