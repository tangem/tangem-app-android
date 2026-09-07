package com.tangem.data.collectibles.vault

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CollectiblesOwnerNetworkProviderTest {

    private val networkFactory: NetworkFactory = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()

    private val provider = CollectiblesOwnerNetworkProvider(
        networkFactory = networkFactory,
        ownerDeriver = ownerDeriver,
    )

    private val userWalletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
    private val network: Network = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(networkFactory, ownerDeriver)
    }

    @Test
    fun `GIVEN the owner key is stored WHEN create THEN the network is built on the owner derivation path`() =
        runTest {
            // Arrange
            coEvery { ownerDeriver.storedOwner(userWalletId) } returns OWNER
            stubNetwork(network)

            // Act
            val actual = provider.create(userWallet)

            // Assert
            assertThat(actual).isEqualTo(network.right())
            verify(exactly = 1) {
                networkFactory.create(
                    blockchain = Blockchain.SolanaTestnet,
                    extraDerivationPath = COLLECTIBLES_OWNER_DERIVATION_PATH,
                    userWallet = userWallet,
                )
            }
        }

    @Test
    fun `GIVEN no owner key WHEN create THEN wallet is unavailable and no network is built`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(userWalletId) } returns null

        // Act
        val actual = provider.create(userWallet)

        // Assert
        assertThat(actual).isEqualTo(GachaVaultError.WalletUnavailable.left())
        verify(exactly = 0) { networkFactory.create(any(), any(), any<UserWallet>(), any()) }
    }

    @Test
    fun `GIVEN no network WHEN create THEN wallet is unavailable`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(userWalletId) } returns OWNER
        stubNetwork(null)

        // Act
        val actual = provider.create(userWallet)

        // Assert
        assertThat(actual).isEqualTo(GachaVaultError.WalletUnavailable.left())
    }

    private fun stubNetwork(network: Network?) {
        every {
            networkFactory.create(
                blockchain = Blockchain.SolanaTestnet,
                extraDerivationPath = COLLECTIBLES_OWNER_DERIVATION_PATH,
                userWallet = userWallet,
            )
        } returns network
    }

    private companion object {

        const val OWNER = "5ZWj7a1f8tWkjBESHKgrLmXshuXxqeY9SYcfbshpAqPG"
    }
}