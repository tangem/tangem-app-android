package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.staking.StakeKitRepositoryFixtures.apiError
import com.tangem.data.staking.StakeKitRepositoryFixtures.enabledYields
import com.tangem.data.staking.StakeKitRepositoryFixtures.tokenDTO
import com.tangem.data.staking.StakeKitRepositoryFixtures.validatorDTO
import com.tangem.data.staking.StakeKitRepositoryFixtures.yieldDTO
import com.tangem.data.staking.converters.YieldConverter
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.staking.model.common.RewardInfo
import com.tangem.domain.staking.model.common.RewardType
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.models.response.EnabledYieldsResponse
import com.tangem.grow.datasource.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO
import com.tangem.grow.datasource.stakekit.models.response.model.YieldDTO.ValidatorDTO.ValidatorStatusDTO
import com.tangem.store.datasource.txhistory.db.dao.StakingValidatorDao
import com.tangem.store.datasource.txhistory.db.entity.staking.StakingValidatorEntity
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultStakeKitRepositoryYieldsTest {

    private val stakeKitApi: StakeKitApi = mockk()
    private val stakingYieldsStore: StakingYieldsStore = mockk()
    private val stakingValidatorDao: StakingValidatorDao = mockk()
    private val txHistoryFeatureToggle: TxHistoryFeatureToggles = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()

    private val repository = DefaultStakeKitRepository(
        stakeKitApi = stakeKitApi,
        stakingYieldsStore = stakingYieldsStore,
        stakingValidatorDao = stakingValidatorDao,
        txHistoryFeatureToggle = txHistoryFeatureToggle,
        dispatchers = TestingCoroutineDispatcherProvider(),
        walletManagersFacade = walletManagersFacade,
        moshi = StakeKitRepositoryFixtures.moshi,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitApi, stakingYieldsStore, stakingValidatorDao, txHistoryFeatureToggle, walletManagersFacade)
    }

    private fun stubAllNetworks(response: ApiResponse<EnabledYieldsResponse>) {
        coEvery {
            stakeKitApi.getEnabledYields(any(), any(), any(), any(), any(), any(), any(), any())
        } returns response
    }

    private fun stubNetwork(network: String, response: ApiResponse<EnabledYieldsResponse>) {
        coEvery {
            stakeKitApi.getEnabledYields(any(), any(), any(), any(), any(), any(), network, any())
        } returns response
    }

    private fun stubStore() {
        coEvery { stakingYieldsStore.store(any(), any()) } just Runs
    }

    private fun stubStoredYields(vararg yields: YieldDTO) {
        coEvery { stakingYieldsStore.getSync() } returns yields.toList()
    }

    private fun coinId(rawId: String = "solana") = CryptoCurrency.ID(
        CryptoCurrency.ID.Prefix.COIN_PREFIX,
        CryptoCurrency.ID.Body.NetworkId(rawId),
        CryptoCurrency.ID.Suffix.RawID(rawId),
    )

    private fun customTokenId() = CryptoCurrency.ID(
        CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
        CryptoCurrency.ID.Body.NetworkId("solana"),
        CryptoCurrency.ID.Suffix.ContractAddress("0xcustom"),
    )

    private fun cosmosDto() = yieldDTO(
        id = MockStaking.COSMOS_ID,
        token = tokenDTO(NetworkTypeDTO.COSMOS, "ATOM", 6, "cosmos"),
    )

    private fun validatorEntity(address: String, commission: Double? = null) = StakingValidatorEntity(
        address = address,
        status = "ACTIVE",
        name = "Validator 1",
        image = null,
        website = null,
        apr = "0.05",
        commission = commission,
        stakedBalance = null,
        votingPower = null,
        isPreferred = true,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchYields {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stakeKit coin integration WHEN fetchYields THEN requests yields by network without yieldId`(
            model: CoinRequestModel,
        ) = runTest {
            // Arrange
            stubAllNetworks(ApiResponse.Success(enabledYields()))
            stubStore()
            every { txHistoryFeatureToggle.isNewTxHistoryEnabled } returns false

            // Act
            repository.fetchYields()

            // Assert
            coVerify(exactly = 1) {
                stakeKitApi.getEnabledYields(false, null, null, null, null, null, model.network, null)
            }
        }

        @Test
        fun `GIVEN polygon ethereum token integration WHEN fetchYields THEN requests yields with yieldId and ethereum network`() =
            runTest {
                // Arrange
                stubAllNetworks(ApiResponse.Success(enabledYields()))
                stubStore()
                every { txHistoryFeatureToggle.isNewTxHistoryEnabled } returns false

                // Act
                repository.fetchYields()

                // Assert
                coVerify(exactly = 1) {
                    stakeKitApi.getEnabledYields(
                        false,
                        null,
                        null,
                        "ethereum-matic-native-staking",
                        null,
                        null,
                        "ethereum",
                        null,
                    )
                }
                coVerify(exactly = 7) {
                    stakeKitApi.getEnabledYields(any(), any(), any(), any(), any(), any(), any(), any())
                }
            }

        @Test
        fun `GIVEN all responses successful WHEN fetchYields THEN stores available yields with force true`() = runTest {
            // Arrange
            val tonDto = yieldDTO(id = MockStaking.TON_ID, token = tokenDTO(NetworkTypeDTO.TON, "TON", 9, "ton"))
            val solanaDto = yieldDTO(id = MockStaking.SOLANA_ID)
            stubAllNetworks(ApiResponse.Success(enabledYields()))
            stubNetwork("ton", ApiResponse.Success(enabledYields(tonDto)))
            stubNetwork("solana", ApiResponse.Success(enabledYields(solanaDto)))
            stubStore()
            every { txHistoryFeatureToggle.isNewTxHistoryEnabled } returns false

            // Act
            repository.fetchYields()

            // Assert
            coVerify { stakingYieldsStore.store(listOf(tonDto, solanaDto), true) }
        }

        @Test
        fun `GIVEN one network fails WHEN fetchYields THEN stores remaining yields with force false`() = runTest {
            // Arrange
            val solanaDto = yieldDTO()
            stubAllNetworks(ApiResponse.Success(enabledYields()))
            stubNetwork("solana", ApiResponse.Success(enabledYields(solanaDto)))
            stubNetwork("tron", apiError(ApiResponseError.NetworkException()))
            stubStore()
            every { txHistoryFeatureToggle.isNewTxHistoryEnabled } returns false

            // Act
            repository.fetchYields()

            // Assert
            coVerify { stakingYieldsStore.store(listOf(solanaDto), false) }
        }

        @Test
        fun `GIVEN yields with isAvailable false or null WHEN fetchYields THEN only available yields stored`() =
            runTest {
                // Arrange
                val available = yieldDTO(id = "a", isAvailable = true)
                stubAllNetworks(ApiResponse.Success(enabledYields()))
                stubNetwork(
                    "solana",
                    ApiResponse.Success(
                        enabledYields(
                            available,
                            yieldDTO(id = "b", isAvailable = false),
                            yieldDTO(id = "c", isAvailable = null),
                        ),
                    ),
                )
                stubStore()
                every { txHistoryFeatureToggle.isNewTxHistoryEnabled } returns false

                // Act
                repository.fetchYields()

                // Assert
                coVerify { stakingYieldsStore.store(listOf(available), true) }
            }

        @Test
        fun `GIVEN new tx history enabled WHEN fetchYields THEN validators upserted and addressless dropped`() =
            runTest {
                // Arrange
                stubAllNetworks(ApiResponse.Success(enabledYields()))
                stubNetwork(
                    "solana",
                    ApiResponse.Success(
                        enabledYields(
                            yieldDTO(
                                validators = listOf(
                                    validatorDTO(address = "v1", commission = 0.1),
                                    validatorDTO(address = null, status = ValidatorStatusDTO.INACTIVE),
                                ),
                            ),
                        ),
                    ),
                )
                stubNetwork(
                    "cosmos",
                    ApiResponse.Success(
                        enabledYields(
                            yieldDTO(id = MockStaking.COSMOS_ID, validators = listOf(validatorDTO(address = "v2"))),
                        ),
                    ),
                )
                stubStore()
                coEvery { stakingValidatorDao.upsert(any()) } just Runs
                every { txHistoryFeatureToggle.isNewTxHistoryEnabled } returns true

                // Act
                repository.fetchYields()

                // Assert
                coVerify {
                    stakingValidatorDao.upsert(listOf(validatorEntity("v1", commission = 0.1), validatorEntity("v2")))
                }
            }

        @Test
        fun `GIVEN new tx history disabled WHEN fetchYields THEN validator dao untouched`() = runTest {
            // Arrange
            stubAllNetworks(ApiResponse.Success(enabledYields()))
            stubNetwork("solana", ApiResponse.Success(enabledYields(yieldDTO())))
            stubStore()
            every { txHistoryFeatureToggle.isNewTxHistoryEnabled } returns false

            // Act
            repository.fetchYields()

            // Assert
            coVerify(exactly = 0) { stakingValidatorDao.upsert(any()) }
        }

        private fun provideTestModels() = listOf(
            CoinRequestModel("ton"),
            CoinRequestModel("solana"),
            CoinRequestModel("cosmos"),
            CoinRequestModel("tron"),
            CoinRequestModel("binance"),
            CoinRequestModel("cardano"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetYieldByCurrency {

        @Test
        fun `GIVEN yield token matches coinGeckoId and symbol WHEN getYield THEN returns converted yield`() = runTest {
            // Arrange
            stubStoredYields(yieldDTO())

            // Act
            val actual = repository.getYield(coinId("solana"), "SOL")

            // Assert
            assertThat(actual).isEqualTo(YieldConverter.convert(yieldDTO()))
        }

        @Test
        fun `GIVEN several yields WHEN getYield THEN returns the one whose tokens match`() = runTest {
            // Arrange
            stubStoredYields(cosmosDto(), yieldDTO())

            // Act
            val actual = repository.getYield(coinId("solana"), "SOL")

            // Assert
            assertThat(actual).isEqualTo(YieldConverter.convert(yieldDTO()))
        }

        @Test
        fun `GIVEN symbol differs in case WHEN getYield THEN throws staking unavailable`() = runTest {
            // Arrange
            stubStoredYields(yieldDTO())

            // Act
            val error = runCatching { repository.getYield(coinId("solana"), "sol") }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Staking is unavailable")
        }

        @Test
        fun `GIVEN coinGeckoId differs WHEN getYield THEN throws staking unavailable`() = runTest {
            // Arrange
            stubStoredYields(yieldDTO())

            // Act
            val error = runCatching { repository.getYield(coinId("cosmos"), "SOL") }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Staking is unavailable")
        }

        // Pins current behaviour: lookup scans yield.tokens only, never yield.token (differs from C7)
        @Test
        fun `GIVEN match only in main token not in tokens list WHEN getYield THEN throws staking unavailable`() =
            runTest {
                // Arrange
                stubStoredYields(yieldDTO(tokens = emptyList()))

                // Act
                val error = runCatching { repository.getYield(coinId("solana"), "SOL") }.exceptionOrNull()

                // Assert
                assertThat(error).isInstanceOf(IllegalStateException::class.java)
                assertThat(error).hasMessageThat().isEqualTo("Staking is unavailable")
            }

        @Test
        fun `GIVEN custom token without raw id WHEN getYield THEN throws custom tokens not available`() = runTest {
            // Arrange
            stubStoredYields(yieldDTO())

            // Act
            val error = runCatching { repository.getYield(customTokenId(), "SOL") }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Staking custom tokens is not available")
            coVerify(exactly = 0) { stakingYieldsStore.getSync() }
        }

        @Test
        fun `GIVEN store has a broken dto WHEN getYield THEN broken item ignored and valid returned`() = runTest {
            // Arrange
            stubStoredYields(yieldDTO(id = null), yieldDTO())

            // Act
            val actual = repository.getYield(coinId("solana"), "SOL")

            // Assert
            assertThat(actual).isEqualTo(YieldConverter.convert(yieldDTO()))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetYieldById {

        @Test
        fun `GIVEN yield with id stored WHEN getYield by id THEN returns it`() = runTest {
            // Arrange
            stubStoredYields(
                yieldDTO(id = MockStaking.TON_ID, token = tokenDTO(NetworkTypeDTO.TON, "TON", 9, "ton")),
                yieldDTO(),
            )

            // Act
            val actual = repository.getYield(MockStaking.SOLANA_ID)

            // Assert
            assertThat(actual).isEqualTo(YieldConverter.convert(yieldDTO()))
        }

        @Test
        fun `GIVEN no yield with id WHEN getYield by id THEN throws staking unavailable`() = runTest {
            // Arrange
            stubStoredYields(yieldDTO())

            // Act
            val error = runCatching { repository.getYield("unknown") }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Staking is unavailable")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetEntryInfo {

        @Test
        fun `GIVEN matching yield WHEN getEntryInfo THEN returns token symbol of yield`() = runTest {
            // Arrange
            stubStoredYields(yieldDTO())

            // Act
            val actual = repository.getEntryInfo(coinId("solana"), "SOL")

            // Assert
            assertThat(actual).isEqualTo(StakingEntryInfo(tokenSymbol = "SOL"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PersistedValidators {

        @Test
        fun `GIVEN persisted entity without name WHEN getPersistedValidatorsFlow THEN address used as name and gross reward computed`() =
            runTest {
                // Arrange
                val entity = StakingValidatorEntity(
                    address = "v1",
                    status = "ACTIVE",
                    name = null,
                    image = null,
                    website = null,
                    apr = "0.05",
                    commission = 0.1,
                    stakedBalance = "100",
                    votingPower = null,
                    isPreferred = null,
                )
                every { stakingValidatorDao.getAllAsFlow() } returns flowOf(listOf(entity))

                // Act
                val actual = repository.getPersistedValidatorsFlow().first()

                // Assert
                val expected = listOf(
                    Yield.Validator(
                        address = "v1",
                        status = Yield.Validator.ValidatorStatus.ACTIVE,
                        name = "v1",
                        image = null,
                        website = null,
                        rewardInfo = RewardInfo(BigDecimal("0.05555556"), RewardType.UNKNOWN),
                        commission = 0.1,
                        stakedBalance = "100",
                        votingPower = null,
                        isPreferred = false,
                        isStrategicPartner = false,
                    ),
                )
                assertThat(actual).isEqualTo(expected)
            }

        @Test
        fun `GIVEN unknown status and partner address WHEN getPersistedValidatorsFlow THEN status unknown and strategic partner`() =
            runTest {
                // Arrange
                val address = "cosmosvaloper1wrx0x9m9ykdhw9sg04v7uljme53wuj03aa5d4f"
                val entity = StakingValidatorEntity(
                    address = address,
                    status = "weird",
                    name = "X",
                    image = null,
                    website = null,
                    apr = null,
                    commission = null,
                    stakedBalance = null,
                    votingPower = null,
                    isPreferred = true,
                )
                every { stakingValidatorDao.getAllAsFlow() } returns flowOf(listOf(entity))

                // Act
                val actual = repository.getPersistedValidatorsFlow().first()

                // Assert
                val expected = listOf(
                    Yield.Validator(
                        address = address,
                        status = Yield.Validator.ValidatorStatus.UNKNOWN,
                        name = "X",
                        image = null,
                        website = null,
                        rewardInfo = null,
                        commission = null,
                        stakedBalance = null,
                        votingPower = null,
                        isPreferred = true,
                        isStrategicPartner = true,
                    ),
                )
                assertThat(actual).isEqualTo(expected)
            }
    }

    internal data class CoinRequestModel(val network: String)
}