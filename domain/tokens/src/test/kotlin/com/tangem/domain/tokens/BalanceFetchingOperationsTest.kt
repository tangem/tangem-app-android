package com.tangem.domain.tokens

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BalanceFetchingOperationsTest {

    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher = mockk()
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher = mockk()
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()

    private val operations = BalanceFetchingOperations(
        multiNetworkStatusFetcher = multiNetworkStatusFetcher,
        multiQuoteStatusFetcher = multiQuoteStatusFetcher,
        multiStakingBalanceFetcher = multiStakingBalanceFetcher,
        stakingIdFactory = stakingIdFactory,
    )

    private val userWalletId = UserWalletId("011")
    private val ethereum = MockCryptoCurrencyFactory().ethereum
    private val jointEthereum = ethereum.onJointNetwork(ownerKeyIndex = 2)

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiNetworkStatusFetcher, multiQuoteStatusFetcher, multiStakingBalanceFetcher, stakingIdFactory)
    }

    @Test
    fun `GIVEN a joint and a personal currency WHEN fetchStaking THEN only the personal one gets a staking id`() =
        runTest {
            // Arrange
            val stakingId = StakingID(integrationId = "ethereum-eth-p2p-pool", address = "0x1")
            coEvery { stakingIdFactory.create(userWalletId, ethereum) } returns Either.Right(stakingId)
            coEvery { multiStakingBalanceFetcher(any()) } returns Either.Right(Unit)

            // Act
            val actual = operations.fetchStaking(userWalletId, listOf(jointEthereum, ethereum))

            // Assert
            assertThat(actual).isEqualTo(Either.Right(Unit))
            coVerify(exactly = 1) {
                multiStakingBalanceFetcher(
                    MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = setOf(stakingId)),
                )
            }
            coVerify(inverse = true) { stakingIdFactory.create(userWalletId, jointEthereum) }
        }

    @Test
    fun `GIVEN only joint currencies WHEN fetchStaking THEN nothing is asked and the result is success`() = runTest {
        // Act
        val actual = operations.fetchStaking(userWalletId, listOf(jointEthereum))

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        coVerify(inverse = true) {
            stakingIdFactory.create(any(), any<CryptoCurrency>())
            multiStakingBalanceFetcher(any())
        }
    }

    private fun CryptoCurrency.Coin.onJointNetwork(ownerKeyIndex: Int): CryptoCurrency.Coin {
        val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/$ownerKeyIndex")
        val jointNetwork = network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )

        return copy(network = jointNetwork)
    }
}