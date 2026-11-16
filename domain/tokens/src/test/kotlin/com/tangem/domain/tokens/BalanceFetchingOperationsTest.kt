package com.tangem.domain.tokens

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BalanceFetchingOperationsTest {

    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher = mockk()
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher = mockk()
    private val firstContributionFetcher: BalanceContributionFetcher = mockk()
    private val secondContributionFetcher: BalanceContributionFetcher = mockk()
    private val tokensFeatureToggles: TokensFeatureToggles = mockk()
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()

    private val operations = BalanceFetchingOperations(
        multiNetworkStatusFetcher = multiNetworkStatusFetcher,
        multiQuoteStatusFetcher = multiQuoteStatusFetcher,
        contributionFetchers = setOf(firstContributionFetcher, secondContributionFetcher),
        tokensFeatureToggles = tokensFeatureToggles,
        multiStakingBalanceFetcher = multiStakingBalanceFetcher,
        stakingIdFactory = stakingIdFactory,
    )

    private val userWalletId = UserWalletId("011")
    private val ethereum = MockCryptoCurrencyFactory().ethereum
    private val jointEthereum = ethereum.onJointNetwork(ownerKeyIndex = 2)
    private val currencies = listOf(ethereum)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            multiNetworkStatusFetcher,
            multiQuoteStatusFetcher,
            firstContributionFetcher,
            secondContributionFetcher,
            tokensFeatureToggles,
            multiStakingBalanceFetcher,
            stakingIdFactory,
        )
        coEvery { multiNetworkStatusFetcher(any()) } returns Either.Right(Unit)
        coEvery { multiQuoteStatusFetcher(any()) } returns Either.Right(Unit)
        coEvery { firstContributionFetcher.fetch(any(), any()) } returns Either.Right(Unit)
        coEvery { secondContributionFetcher.fetch(any(), any()) } returns Either.Right(Unit)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToggleOn {

        @BeforeEach
        fun enableToggle() {
            every { tokensFeatureToggles.isPluggableBalanceSourcesEnabled } returns true
        }

        @Test
        fun `GIVEN contributions requested WHEN fetchAll THEN every contribution fetcher is called`() = runTest {
            // Act
            val actual = operations.fetchAll(userWalletId, currencies, setOf(FetchingSource.CONTRIBUTIONS))

            // Assert
            assertThat(actual).isEmpty()
            coVerify(exactly = 1) {
                firstContributionFetcher.fetch(userWalletId, currencies)
                secondContributionFetcher.fetch(userWalletId, currencies)
            }
            coVerify(exactly = 0) {
                multiNetworkStatusFetcher(any())
                multiQuoteStatusFetcher(any())
                multiStakingBalanceFetcher(any())
            }
        }

        @Test
        fun `GIVEN contributions not requested WHEN fetchAll THEN no contribution fetcher is called`() = runTest {
            // Act
            operations.fetchAll(userWalletId, currencies, setOf(FetchingSource.NETWORK, FetchingSource.QUOTE))

            // Assert
            coVerify(exactly = 1) {
                multiNetworkStatusFetcher(any())
                multiQuoteStatusFetcher(any())
            }
            coVerify(exactly = 0) {
                firstContributionFetcher.fetch(any(), any())
                secondContributionFetcher.fetch(any(), any())
            }
        }

        @Test
        fun `GIVEN one contribution and network fail WHEN fetchAll THEN errors keyed by their names`() = runTest {
            // Arrange
            val contributionError = IllegalStateException("contribution")
            val networkError = IllegalStateException("network")
            coEvery { firstContributionFetcher.fetch(any(), any()) } returns Either.Left(contributionError)
            coEvery { multiNetworkStatusFetcher(any()) } returns Either.Left(networkError)

            // Act
            val actual = operations.fetchAll(
                userWalletId = userWalletId,
                currencies = currencies,
                sources = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.CONTRIBUTIONS),
            )

            // Assert
            assertThat(actual).containsExactly(
                FetchingSource.NETWORK.name, networkError,
                firstContributionFetcher.javaClass.simpleName, contributionError,
            )
            coVerify(exactly = 1) { secondContributionFetcher.fetch(userWalletId, currencies) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToggleOff {

        @BeforeEach
        fun disableToggle() {
            every { tokensFeatureToggles.isPluggableBalanceSourcesEnabled } returns false
        }

        @Test
        fun `GIVEN contributions requested WHEN fetchAll THEN legacy staking is fetched instead of the set`() =
            runTest {
                // Arrange
                val stakingId = StakingID(integrationId = "ethereum-eth-p2p-pool", address = "0x1")
                val error = IllegalStateException("staking")
                coEvery { stakingIdFactory.create(userWalletId, ethereum) } returns Either.Right(stakingId)
                coEvery { multiStakingBalanceFetcher(any()) } returns Either.Left(error)

                // Act
                val actual = operations.fetchAll(userWalletId, currencies, setOf(FetchingSource.CONTRIBUTIONS))

                // Assert
                assertThat(actual).containsExactly("STAKING", error)
                coVerify(exactly = 0) {
                    firstContributionFetcher.fetch(any(), any())
                    secondContributionFetcher.fetch(any(), any())
                }
            }

        @Test
        fun `GIVEN a joint and a personal currency WHEN fetchAll THEN only the personal one gets a staking id`() =
            runTest {
                // Arrange
                val stakingId = StakingID(integrationId = "ethereum-eth-p2p-pool", address = "0x1")
                coEvery { stakingIdFactory.create(userWalletId, ethereum) } returns Either.Right(stakingId)
                coEvery { multiStakingBalanceFetcher(any()) } returns Either.Right(Unit)

                // Act
                val actual = operations.fetchAll(
                    userWalletId = userWalletId,
                    currencies = listOf(jointEthereum, ethereum),
                    sources = setOf(FetchingSource.CONTRIBUTIONS),
                )

                // Assert
                assertThat(actual).isEmpty()
                coVerify(exactly = 1) {
                    multiStakingBalanceFetcher(
                        MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = setOf(stakingId)),
                    )
                }
                coVerify(exactly = 0) { stakingIdFactory.create(userWalletId, jointEthereum) }
            }

        @Test
        fun `GIVEN only joint currencies WHEN fetchAll THEN nothing is asked and the result is success`() = runTest {
            // Act
            val actual = operations.fetchAll(userWalletId, listOf(jointEthereum), setOf(FetchingSource.CONTRIBUTIONS))

            // Assert
            assertThat(actual).isEmpty()
            coVerify(exactly = 0) {
                stakingIdFactory.create(any(), any<CryptoCurrency>())
                multiStakingBalanceFetcher(any())
            }
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