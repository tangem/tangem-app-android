package com.tangem.domain.staking.contribution

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class StakingContributionFetcherTest {

    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()

    private val fetcher = StakingContributionFetcher(
        multiStakingBalanceFetcher = multiStakingBalanceFetcher,
        stakingIdFactory = stakingIdFactory,
    )

    private val userWalletId = UserWalletId("011")
    private val ethereum = MockCryptoCurrencyFactory().ethereum
    private val jointEthereum = ethereum.onJointNetwork(ownerKeyIndex = 2)
    private val stakingId = StakingID(integrationId = "ethereum-eth-p2p-pool", address = "0x1")

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiStakingBalanceFetcher, stakingIdFactory)
    }

    @Test
    fun `GIVEN staking fetcher WHEN name read THEN it is the STAKING error log key`() {
        assertThat(fetcher.name).isEqualTo("STAKING")
    }

    @Test
    fun `GIVEN a joint and a personal currency WHEN fetch THEN only the personal one gets a staking id`() = runTest {
        // Arrange
        coEvery { stakingIdFactory.create(userWalletId, ethereum) } returns Either.Right(stakingId)
        coEvery { multiStakingBalanceFetcher(any()) } returns Either.Right(Unit)

        // Act
        val actual = fetcher.fetch(userWalletId, listOf(jointEthereum, ethereum))

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 1) {
            multiStakingBalanceFetcher(
                MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = setOf(stakingId)),
            )
        }
        coVerify(exactly = 0) { stakingIdFactory.create(userWalletId, jointEthereum) }
    }

    @Test
    fun `GIVEN only joint currencies WHEN fetch THEN nothing is asked and the result is success`() = runTest {
        // Act
        val actual = fetcher.fetch(userWalletId, listOf(jointEthereum))

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 0) {
            stakingIdFactory.create(any(), any<CryptoCurrency>())
            multiStakingBalanceFetcher(any())
        }
    }

    @Test
    fun `GIVEN unsupported currency WHEN fetch THEN balances not fetched and the result is success`() = runTest {
        // Arrange
        coEvery { stakingIdFactory.create(userWalletId, ethereum) } returns
            Either.Left(StakingIdFactory.Error.UnsupportedCurrency)

        // Act
        val actual = fetcher.fetch(userWalletId, listOf(ethereum))

        // Assert
        assertThat(actual).isEqualTo(Either.Right(Unit))
        coVerify(exactly = 0) { multiStakingBalanceFetcher(any()) }
    }

    @Test
    fun `GIVEN balance fetch fails WHEN fetch THEN the error is returned`() = runTest {
        // Arrange
        val error = IllegalStateException("staking is down")
        coEvery { stakingIdFactory.create(userWalletId, ethereum) } returns Either.Right(stakingId)
        coEvery { multiStakingBalanceFetcher(any()) } returns Either.Left(error)

        // Act
        val actual = fetcher.fetch(userWalletId, listOf(ethereum))

        // Assert
        assertThat(actual).isEqualTo(Either.Left(error))
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