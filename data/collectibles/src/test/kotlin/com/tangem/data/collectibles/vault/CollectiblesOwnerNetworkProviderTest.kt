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
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectiblesOwnerNetworkProviderTest {

    private val networkFactory: NetworkFactory = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()

    private val provider = CollectiblesOwnerNetworkProvider(
        networkFactory = networkFactory,
        ownerDeriver = ownerDeriver,
    )

    private val userWalletId = UserWalletId("011")
    private val userWallet: UserWallet = mockColdWallet(walletId = userWalletId)
    private val network: Network = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(networkFactory, ownerDeriver)
    }

    /** The owner address is the vault's `cold_owner`, so it must be built on the same chain the vault is. */
    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the owner key is stored WHEN create THEN the network is built on the card's chain and owner path`(
        model: VaultChainModel,
    ) = runTest {
        // Arrange
        val wallet = mockColdWallet(walletId = userWalletId, isTestCard = model.isTestCard)
        coEvery { ownerDeriver.storedOwner(userWalletId) } returns OWNER
        stubNetwork(userWallet = wallet, blockchain = model.blockchain, network = network)

        // Act
        val actual = provider.create(wallet)

        // Assert
        assertThat(actual).isEqualTo(network.right())
        verify(exactly = 1) {
            networkFactory.create(
                blockchain = model.blockchain,
                extraDerivationPath = COLLECTIBLES_OWNER_DERIVATION_PATH,
                userWallet = wallet,
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
        stubNetwork(userWallet = userWallet, blockchain = Blockchain.Solana, network = null)

        // Act
        val actual = provider.create(userWallet)

        // Assert
        assertThat(actual).isEqualTo(GachaVaultError.WalletUnavailable.left())
    }

    private fun stubNetwork(userWallet: UserWallet, blockchain: Blockchain, network: Network?) {
        every {
            networkFactory.create(
                blockchain = blockchain,
                extraDerivationPath = COLLECTIBLES_OWNER_DERIVATION_PATH,
                userWallet = userWallet,
            )
        } returns network
    }

    private fun provideTestModels() = vaultChainModels()

    private companion object {

        const val OWNER = "5ZWj7a1f8tWkjBESHKgrLmXshuXxqeY9SYcfbshpAqPG"
    }
}