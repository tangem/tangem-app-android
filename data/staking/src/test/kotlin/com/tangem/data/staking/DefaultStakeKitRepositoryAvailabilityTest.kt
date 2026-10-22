package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.data.staking.StakeKitRepositoryFixtures.tokenDTO
import com.tangem.data.staking.StakeKitRepositoryFixtures.yieldDTO
import com.tangem.data.staking.converters.YieldConverter
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.store.datasource.txhistory.db.dao.StakingValidatorDao
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStakeKitRepositoryAvailabilityTest {

    private val stakeKitApi: StakeKitApi = mockk()
    private val stakingYieldsStore: StakingYieldsStore = mockk()
    private val stakingValidatorDao: StakingValidatorDao = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()

    private val repository = DefaultStakeKitRepository(
        stakeKitApi = stakeKitApi,
        stakingYieldsStore = stakingYieldsStore,
        stakingValidatorDao = stakingValidatorDao,
        dispatchers = TestingCoroutineDispatcherProvider(),
        walletManagersFacade = walletManagersFacade,
        moshi = StakeKitRepositoryFixtures.moshi,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitApi, stakingYieldsStore, stakingValidatorDao, walletManagersFacade)
    }

    private fun cosmosDto() = yieldDTO(
        id = MockStaking.COSMOS_ID,
        token = tokenDTO(NetworkTypeDTO.COSMOS, "ATOM", 6, "cosmos"),
    )

    private fun available(integrationId: StakingIntegrationID.StakeKit, dto: YieldDTO = yieldDTO()) =
        StakingAvailability.Available(StakingOption.StakeKit(integrationId, YieldConverter.convert(dto)))

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvailabilityFlow {

        @Test
        fun `GIVEN no yields WHEN getStakingAvailability THEN temporary unavailable`() = runTest {
            // Arrange
            every { stakingYieldsStore.get() } returns flowOf(emptyList())

            // Act
            val actual = repository.getStakingAvailability(StakingIntegrationID.StakeKit.Coin.Solana, SOL, "SOL")
                .first()

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.TemporaryUnavailable)
        }

        @Test
        fun `GIVEN yields without match WHEN getStakingAvailability THEN temporary unavailable`() = runTest {
            // Arrange
            every { stakingYieldsStore.get() } returns flowOf(listOf(cosmosDto()))

            // Act
            val actual = repository.getStakingAvailability(StakingIntegrationID.StakeKit.Coin.Solana, SOL, "SOL")
                .first()

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.TemporaryUnavailable)
        }

        @Test
        fun `GIVEN matching yield WHEN getStakingAvailability THEN available with stakeKit option`() = runTest {
            // Arrange
            every { stakingYieldsStore.get() } returns flowOf(listOf(yieldDTO()))

            // Act
            val actual = repository.getStakingAvailability(StakingIntegrationID.StakeKit.Coin.Solana, SOL, "SOL")
                .first()

            // Assert
            assertThat(actual).isEqualTo(available(StakingIntegrationID.StakeKit.Coin.Solana))
        }

        // Pins current behaviour: yield is found by coinGeckoId + symbol, integrationId is not compared
        @Test
        fun `GIVEN yield of another chain matching coinGeckoId and symbol WHEN getStakingAvailability THEN option carries requested integration id`() =
            runTest {
                // Arrange
                every { stakingYieldsStore.get() } returns flowOf(listOf(yieldDTO()))

                // Act
                val actual = repository.getStakingAvailability(StakingIntegrationID.StakeKit.Coin.Ton, SOL, "SOL")
                    .first()

                // Assert
                assertThat(actual).isEqualTo(available(StakingIntegrationID.StakeKit.Coin.Ton))
            }

        @Test
        fun `GIVEN the same yields emitted twice WHEN getStakingAvailability THEN emits once`() = runTest {
            // Arrange
            every { stakingYieldsStore.get() } returns flowOf(listOf(yieldDTO()), listOf(yieldDTO()))

            // Act
            val actual = repository.getStakingAvailability(StakingIntegrationID.StakeKit.Coin.Solana, SOL, "SOL")
                .toList()

            // Assert
            assertThat(actual).containsExactly(available(StakingIntegrationID.StakeKit.Coin.Solana))
        }

        @Test
        fun `GIVEN two different non matching lists WHEN getStakingAvailability THEN emits temporary unavailable twice`() =
            runTest {
                // Arrange
                every { stakingYieldsStore.get() } returns flowOf(emptyList(), listOf(cosmosDto()))

                // Act
                val actual = repository.getStakingAvailability(StakingIntegrationID.StakeKit.Coin.Solana, SOL, "SOL")
                    .toList()

                // Assert
                assertThat(actual)
                    .containsExactly(StakingAvailability.TemporaryUnavailable, StakingAvailability.TemporaryUnavailable)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Sync {

        @Test
        fun `GIVEN no yields WHEN getStakingAvailabilitySync THEN temporary unavailable`() = runTest {
            // Arrange
            coEvery { stakingYieldsStore.getSync() } returns emptyList()

            // Act
            val actual = repository.getStakingAvailabilitySync(StakingIntegrationID.StakeKit.Coin.Solana, SOL, "SOL")

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.TemporaryUnavailable)
        }

        @Test
        fun `GIVEN no match WHEN getStakingAvailabilitySync THEN temporary unavailable`() = runTest {
            // Arrange
            coEvery { stakingYieldsStore.getSync() } returns listOf(cosmosDto())

            // Act
            val actual = repository.getStakingAvailabilitySync(StakingIntegrationID.StakeKit.Coin.Solana, SOL, "SOL")

            // Assert
            assertThat(actual).isEqualTo(StakingAvailability.TemporaryUnavailable)
        }

        @Test
        fun `GIVEN matching yield WHEN getStakingAvailabilitySync THEN available with stakeKit option`() = runTest {
            // Arrange
            coEvery { stakingYieldsStore.getSync() } returns listOf(yieldDTO())

            // Act
            val actual = repository.getStakingAvailabilitySync(StakingIntegrationID.StakeKit.Coin.Solana, SOL, "SOL")

            // Assert
            assertThat(actual).isEqualTo(available(StakingIntegrationID.StakeKit.Coin.Solana))
        }

        // Pins current behaviour: yield is found by coinGeckoId + symbol, integrationId is not compared
        @Test
        fun `GIVEN yield of another chain matching coinGeckoId and symbol WHEN getStakingAvailabilitySync THEN option carries requested integration id`() =
            runTest {
                // Arrange
                coEvery { stakingYieldsStore.getSync() } returns listOf(yieldDTO())

                // Act
                val actual = repository.getStakingAvailabilitySync(
                    integrationId = StakingIntegrationID.StakeKit.Coin.Cardano,
                    rawCurrencyId = SOL,
                    symbol = "SOL",
                )

                // Assert
                assertThat(actual).isEqualTo(available(StakingIntegrationID.StakeKit.Coin.Cardano))
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EnabledYields {

        @Test
        fun `GIVEN valid and broken dtos WHEN getEnabledYields THEN only valid converted`() = runTest {
            // Arrange
            every { stakingYieldsStore.get() } returns flowOf(listOf(yieldDTO(id = null), yieldDTO()))

            // Act
            val actual = repository.getEnabledYields().first()

            // Assert
            assertThat(actual).containsExactly(YieldConverter.convert(yieldDTO()))
        }
    }

    private companion object {

        val SOL = CryptoCurrency.RawID("solana")
    }
}