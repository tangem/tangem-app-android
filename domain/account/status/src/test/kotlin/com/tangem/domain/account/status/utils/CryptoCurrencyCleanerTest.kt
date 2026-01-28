package com.tangem.domain.account.status.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.nft.utils.NFTCleaner
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CryptoCurrencyCleanerTest {

    private val networksCleaner: NetworksCleaner = mockk(relaxUnitFun = true)
    private val stakingCleaner: StakingCleaner = mockk(relaxUnitFun = true)
    private val nftCleaner: NFTCleaner = mockk(relaxUnitFun = true)

    private val cleaner = CryptoCurrencyMetadataCleaner(
        networksCleaner = networksCleaner,
        stakingCleaner = stakingCleaner,
        nftCleaner = nftCleaner,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val coin = cryptoCurrencyFactory.ethereum
    private val token = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)

    @AfterEach
    fun tearDown() {
        clearMocks(networksCleaner, stakingCleaner, nftCleaner)
    }

    @Test
    fun `should not call cleaners when currencies list is empty`() = runTest {
        // Act
        cleaner(userWalletId = userWalletId, currencies = emptyList())

        // Assert
        coVerify(inverse = true) {
            networksCleaner(userWalletId = any(), currencies = any())
            stakingCleaner(userWalletId = any(), currencies = any())
            nftCleaner(userWalletId = any(), networks = any())
        }
    }

    @Test
    fun `should call both cleaners when currencies list is not empty`() = runTest {
        // Arrange
        val currencies = listOf(coin, token)

        // Act
        cleaner(userWalletId = userWalletId, currencies = currencies)

        // Assert
        coVerify {
            networksCleaner(userWalletId = userWalletId, currencies = currencies)
            stakingCleaner(userWalletId = userWalletId, currencies = currencies)
            nftCleaner(userWalletId = userWalletId, networks = setOf(coin.network, token.network))
        }
    }
}