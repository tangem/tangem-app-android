package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * A joint account is a Safe contract: the facade must never derive a key or build a wallet manager for its network,
 * whichever door the request comes through.
 */
internal class DefaultWalletManagersFacadeTest {

    private val walletManagersStore: WalletManagersStore = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val facade = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = mockk(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = mockk(),
        blockchainSDKFactory = mockk(),
    )

    private val userWalletId = UserWalletId("011")
    private val ownerKeyPath = "m/44'/60'/888888'/0/2"
    private val jointNetwork = MockCryptoCurrencyFactory().ethereum.network.let { network ->
        val derivationPath = Network.DerivationPath.Joint(value = ownerKeyPath)

        network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )
    }

    @Test
    fun `GIVEN joint network WHEN update THEN unreachable and nothing is looked up or derived`() = runTest {
        // Act
        val actual = facade.update(
            userWalletId = userWalletId,
            network = jointNetwork,
            extraTokens = emptySet(),
            xpub = null,
        )

        // Assert
        assertThat(actual).isEqualTo(UpdateWalletManagerResult.Unreachable())
        verify {
            userWalletsListRepository wasNot Called
            walletManagersStore wasNot Called
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `GIVEN owner key path WHEN getOrCreateWalletManager THEN no manager is created`() = runTest {
        // Act
        val actual = facade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = Blockchain.Ethereum,
            derivationPath = ownerKeyPath,
        )

        // Assert
        assertThat(actual).isNull()
        verify {
            userWalletsListRepository wasNot Called
            walletManagersStore wasNot Called
        }
    }
}