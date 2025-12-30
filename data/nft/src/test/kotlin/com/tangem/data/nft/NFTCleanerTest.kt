package com.tangem.data.nft

import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.local.nft.NFTPersistenceStore
import com.tangem.datasource.local.nft.NFTPersistenceStoreFactory
import com.tangem.datasource.local.nft.NFTRuntimeStore
import com.tangem.datasource.local.nft.NFTRuntimeStoreFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NFTCleanerTest {

    private val nftPersistenceStoreFactory: NFTPersistenceStoreFactory = mockk()
    private val nftRuntimeStoreFactory: NFTRuntimeStoreFactory = mockk()

    private val nftCleaner = DefaultNFTRepository(
        nftPersistenceStoreFactory = nftPersistenceStoreFactory,
        nftRuntimeStoreFactory = nftRuntimeStoreFactory,
        walletManagersFacade = mockk(),
        dispatchers = mockk(),
        userWalletsStore = mockk(),
        networkFactory = mockk(),
        excludedBlockchains = mockk(),
        context = mockk(),
    )

    private val userWalletId = UserWalletId("011")

    @AfterEach
    fun tearDown() {
        clearMocks(nftPersistenceStoreFactory, nftRuntimeStoreFactory)
    }

    @Test
    fun `should call invoke with multiple networks`() = runTest {
        // Arrange
        val mockCryptoCurrencyFactory = MockCryptoCurrencyFactory()
        val networks = mockCryptoCurrencyFactory.ethereumAndStellar.map(CryptoCurrency.Coin::network)
        val persistenceByNetwork = networks.associateWith { mockk<NFTPersistenceStore>(relaxUnitFun = true) }
        val runtimeByNetwork = networks.associateWith { mockk<NFTRuntimeStore>(relaxUnitFun = true) }

        networks.forEach { network ->
            every { nftPersistenceStoreFactory.provide(userWalletId, network) } returns persistenceByNetwork[network]!!
            every { nftRuntimeStoreFactory.provide(network) } returns runtimeByNetwork[network]!!
        }

        // Act
        nftCleaner.invoke(userWalletId = userWalletId, networks = networks.toSet())

        // Assert
        coVerifyOrder {
            networks.forEach { network ->
                nftPersistenceStoreFactory.provide(userWalletId, network)
                persistenceByNetwork[network]!!.clear()
                // nftRuntimeStoreFactory.provide(network)
                // runtimeByNetwork[network]!!.clear()
            }
        }
    }

    @Test
    fun `should handle empty networks set`() = runTest {
        // Act
        nftCleaner.invoke(userWalletId, emptySet())

        // Assert
        coVerify(inverse = true) {
            nftPersistenceStoreFactory.provide(userWalletId = any(), network = any())
            nftRuntimeStoreFactory.provide(network = any())
        }
    }
}