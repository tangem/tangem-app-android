package com.tangem.domain.nft.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class NFTCleanerCryptoCurrencyDataCleanerTest {

    private val userWalletId = UserWalletId("011")

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    @Test
    fun `GIVEN currencies WHEN invoke THEN nft cleared once for their networks`() = runTest {
        // Arrange
        val cleaner = RecordingNFTCleaner()
        val coin = cryptoCurrencyFactory.ethereum
        val token = cryptoCurrencyFactory.createToken(Blockchain.Ethereum)
        val otherCoin = cryptoCurrencyFactory.createCoin(Blockchain.Polygon)

        // Act
        cleaner(userWalletId = userWalletId, currencies = listOf(coin, token, otherCoin))

        // Assert
        assertThat(cleaner.calls).containsExactly(
            userWalletId to setOf(coin.network, token.network, otherCoin.network),
        )
    }

    @Test
    fun `GIVEN empty currencies WHEN invoke THEN nft not cleared`() = runTest {
        // Arrange
        val cleaner = RecordingNFTCleaner()

        // Act
        cleaner(userWalletId = userWalletId, currencies = emptyList())

        // Assert
        assertThat(cleaner.calls).isEmpty()
    }

    @Test
    fun `GIVEN wallets WHEN clear THEN nft not cleared`() = runTest {
        // Arrange
        val cleaner = RecordingNFTCleaner()

        // Act
        cleaner.clear(userWalletIds = listOf(userWalletId))

        // Assert
        assertThat(cleaner.calls).isEmpty()
    }

    private class RecordingNFTCleaner : NFTCleaner {

        val calls = mutableListOf<Pair<UserWalletId, Set<Network>>>()

        override suspend fun invoke(userWalletId: UserWalletId, networks: Set<Network>) {
            calls += userWalletId to networks
        }
    }
}