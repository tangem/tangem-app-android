package com.tangem.data.walletmanager

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.ethereum.ExternalAddressBalanceProvider
import com.tangem.blockchain.blockchains.ethereum.network.ContractCallData
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchainsdk.BlockchainSDKFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.jointaccount.address.JointAccountSafeAddressResolver
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import dagger.Lazy
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class DefaultJointNetworkReaderTest {

    private val providersStore: ExternalAddressBalanceProvidersStore = mockk()
    private val safeAddressResolver: JointAccountSafeAddressResolver = mockk()
    private val provider: ExternalAddressBalanceProvider = mockk()

    private val reader = DefaultJointNetworkReader(
        externalAddressBalanceProvidersStore = providersStore,
        jointAccountSafeAddressResolver = Lazy { safeAddressResolver },
        blockchainSDKFactory = mockk<BlockchainSDKFactory>(relaxed = true),
    )

    private val userWalletId = UserWalletId("0011223344556677")
    private val network: Network = MockCryptoCurrencyFactory().ethereum.network

    @BeforeEach
    fun resetMocks() {
        clearMocks(providersStore, safeAddressResolver, provider)
        every { provider.address } returns SAFE_ADDRESS
    }

    @Test
    fun `GIVEN ethereum WHEN getChainId THEN the EIP-155 id is returned`() = runTest {
        // Act
        val actual = reader.getChainId(network = network)

        // Assert
        assertThat(actual).isEqualTo(1L.right())
    }

    @Test
    fun `GIVEN a cached provider WHEN getCode THEN it is reused and its result is Right`() = runTest {
        // Arrange
        coEvery { providersStore.getSyncOrNull(userWalletId, network) } returns provider
        coEvery { provider.getCode(address = SAFE_ADDRESS) } returns Result.Success("0x6080")

        // Act
        val actual = reader.getCode(userWalletId = userWalletId, network = network)

        // Assert
        assertThat(actual).isEqualTo("0x6080".right())
        coVerify(exactly = 0) { safeAddressResolver.resolve(any(), any()) }
    }

    @Test
    fun `GIVEN the provider fails WHEN reading THEN the SDK error becomes Left`() = runTest {
        // Arrange
        coEvery { providersStore.getSyncOrNull(userWalletId, network) } returns provider
        coEvery { provider.getContractNonce(address = SAFE_ADDRESS) } returns
            Result.Failure(BlockchainSdkError.FailedToLoadFee)

        // Act
        val actual = reader.getContractNonce(userWalletId = userWalletId, network = network)

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(BlockchainSdkError.FailedToLoadFee)
    }

    @Test
    fun `GIVEN a cached provider WHEN estimating gas THEN the call is made from the safe address`() = runTest {
        // Arrange
        coEvery { providersStore.getSyncOrNull(userWalletId, network) } returns provider
        coEvery {
            provider.getGasLimit(to = RECIPIENT, from = SAFE_ADDRESS, value = "0xde0b6b3a7640000", data = null)
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
    fun `GIVEN a cached provider WHEN calling the safe contract THEN the call targets the safe itself`() = runTest {
        // Arrange
        coEvery { providersStore.getSyncOrNull(userWalletId, network) } returns provider
        coEvery {
            provider.callContract(data = ContractCallData(to = SAFE_ADDRESS, data = GET_OWNERS_SELECTOR))
        } returns Result.Success("0x00")

        // Act
        val actual = reader.callSafeContract(userWalletId = userWalletId, network = network, data = GET_OWNERS_SELECTOR)

        // Assert
        assertThat(actual).isEqualTo("0x00".right())
    }

    @Test
    fun `GIVEN no cached provider and no safe address WHEN reading THEN Left without touching the store`() = runTest {
        // Arrange
        coEvery { providersStore.getSyncOrNull(userWalletId, network) } returns null
        coEvery { safeAddressResolver.resolve(userWalletId, setOf(network)) } returns emptyMap()

        // Act
        val actual = reader.getCode(userWalletId = userWalletId, network = network)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(IllegalStateException::class.java)
        coVerify(exactly = 0) { providersStore.store(any(), any(), any()) }
    }

    private companion object {
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val GET_OWNERS_SELECTOR = "0xa0e67e2b"
    }
}