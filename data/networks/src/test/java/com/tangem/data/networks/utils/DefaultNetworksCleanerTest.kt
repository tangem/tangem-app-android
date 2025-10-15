package com.tangem.data.networks.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.networks.store.NetworksStatusesStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultNetworksCleanerTest {

    private val networksStatusesStore = mockk<NetworksStatusesStore>(relaxed = true)
    private val walletManagersFacade = mockk<WalletManagersFacade>(relaxed = true)
    private val cleaner = DefaultNetworksCleaner(
        networksStatusesStore = networksStatusesStore,
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )
    private val userWalletId = UserWalletId("011")
    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val network = cryptoCurrencyFactory.ethereum.network
    private val coin = cryptoCurrencyFactory.ethereum
    private val token = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)

    @BeforeEach
    fun setUp() {
        clearMocks(networksStatusesStore, walletManagersFacade)
    }

    @Test
    fun `should clear networks and remove managers and tokens when called`() = runTest {
        // Arrange
        val currencies = listOf(coin, token)

        // Act
        cleaner(userWalletId = userWalletId, currencies = currencies)

        // Assert
        coVerifyOrder {
            networksStatusesStore.clear(userWalletId, setOf(network))
            walletManagersFacade.remove(userWalletId = userWalletId, networks = setOf(network))
            walletManagersFacade.removeTokens(userWalletId = userWalletId, tokens = setOf(token))
        }
    }

    @Test
    fun `should handle empty currencies`() = runTest {
        // Act
        cleaner(userWalletId = userWalletId, currencies = emptyList())

        // Assert
        coVerifyOrder(inverse = true) {
            networksStatusesStore.clear(userWalletId = any(), networks = any())
            walletManagersFacade.remove(userWalletId = any(), networks = any())
            walletManagersFacade.removeTokens(userWalletId = any(), tokens = any())
        }
    }

    @Test
    fun `should clear only networks when there are no tokens`() = runTest {
        val currencies = listOf(coin)

        cleaner(userWalletId = userWalletId, currencies = currencies)

        coVerifyOrder {
            networksStatusesStore.clear(userWalletId, setOf(network))
            walletManagersFacade.remove(userWalletId = userWalletId, networks = setOf(network))
        }

        coVerifyOrder(inverse = true) {
            walletManagersFacade.removeTokens(userWalletId = any(), tokens = any())
        }
    }

    @Test
    fun `should clear only tokens when there are no networks`() = runTest {
        // Arrange
        val currencies = listOf(token)

        // Act
        cleaner(userWalletId = userWalletId, currencies = currencies)

        // Assert
        coVerifyOrder {
            walletManagersFacade.removeTokens(userWalletId = userWalletId, tokens = setOf(token))
        }

        coVerifyOrder(inverse = true) {
            networksStatusesStore.clear(userWalletId = any(), networks = any())
            walletManagersFacade.remove(userWalletId = any(), networks = any())
        }
    }
}