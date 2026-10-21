package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.staking.StakeKitRepositoryFixtures.apiError
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.grow.datasource.ethpool.P2PEthPoolApi
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolErrorDetailsDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolVaultDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolNetworkDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolVaultsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.data.staking.store.P2PEthPoolRegionBlockedStore
import com.tangem.datasource.local.token.DefaultP2PEthPoolVaultsStore
import com.tangem.data.staking.store.P2PVaultLimitsStore
import com.tangem.store.datasource.txhistory.db.dao.P2PEthPoolVaultDao
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultP2PEthPoolRepositoryAvailabilityTest {

    private val api: P2PEthPoolApi = mockk()
    private val vaultsStore = DefaultP2PEthPoolVaultsStore(MockStateDataStore(default = emptyList()))
    private val limitsStore = P2PVaultLimitsStore()
    private val tangemTechApi: TangemTechApi = mockk()
    private val featureToggles: StakingFeatureToggles = mockk()
    private val regionBlockedStore = P2PEthPoolRegionBlockedStore()
    private val vaultDao: P2PEthPoolVaultDao = mockk()
    private val txHistoryFeatureToggles: TxHistoryFeatureToggles = mockk()

    private val repository = DefaultP2PEthPoolRepository(
        p2pEthPoolApi = api,
        p2pEthPoolVaultsStore = vaultsStore,
        p2pVaultLimitsStore = limitsStore,
        p2pEthPoolVaultDao = vaultDao,
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        stakingFeatureToggles = featureToggles,
        txHistoryFeatureToggle = txHistoryFeatureToggles,
        p2pEthPoolRegionBlockedStore = regionBlockedStore,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(api, tangemTechApi, featureToggles, vaultDao, txHistoryFeatureToggles)
        limitsStore.clear()
        regionBlockedStore.clear()
        runBlocking { vaultsStore.store(emptyList()) }
    }

    private suspend fun seedStaleState() {
        regionBlockedStore.store(true)
        vaultsStore.store(listOf(buildVault("0xSTALE", totalAssets = "1")))
    }

    private suspend fun seedAvailability(
        vaults: List<P2PEthPoolVault>,
        limits: Map<String, VaultLimitInfo>?,
        regionBlocked: Boolean,
    ) {
        vaultsStore.store(vaults)
        limitsStore.store(limits)
        regionBlockedStore.store(regionBlocked)
    }

    private fun buildVault(address: String, totalAssets: String) = P2PEthPoolVault(
        vaultAddress = address,
        displayName = "Vault",
        apy = BigDecimal("4.5"),
        baseApy = BigDecimal("4.0"),
        capacity = BigDecimal("1000"),
        totalAssets = BigDecimal(totalAssets),
        feePercent = BigDecimal("10"),
        isPrivate = false,
        isGenesis = false,
        isSmoothingPool = true,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 0L,
    )

    private fun limits(address: String, limit: String) =
        mapOf(address.lowercase() to VaultLimitInfo(limit = BigDecimal(limit), coefficient = null))

    private fun errorResponse(
        code: ApiResponseError.HttpException.Code,
    ): ApiResponse<P2PEthPoolResponse<P2PEthPoolVaultsResponse>> = apiError(
        ApiResponseError.HttpException(code = code, message = code.name, errorBody = null),
    )

    private fun regionBlockedErrorResponse() =
        errorResponse(ApiResponseError.HttpException.Code.UNAVAILABLE_FOR_LEGAL_REASONS)

    private fun vaultDTO(address: String) = P2PEthPoolVaultDTO(
        vaultAddress = address,
        displayName = "Vault",
        apy = BigDecimal("4.5"),
        baseApy = BigDecimal("4.0"),
        capacity = BigDecimal("1000"),
        totalAssets = BigDecimal("100"),
        feePercent = BigDecimal("10"),
        isPrivate = false,
        isGenesis = false,
        isSmoothingPool = true,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 0L,
    )

    private fun p2pOption(address: String = "0xABC", totalAssets: String) =
        StakingOption.P2PEthPool(listOf(buildVault(address, totalAssets)))

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchVaults {

        @Test
        fun `GIVEN 451 WHEN fetchVaults THEN region flag set true`() = runTest {
            // Arrange
            vaultsStore.store(listOf(buildVault("0xSTALE", totalAssets = "1")))
            every { featureToggles.isIntegrationEnabled(any()) } returns true
            coEvery { api.getVaults(any()) } returns regionBlockedErrorResponse()

            // Act
            repository.fetchVaults()

            // Assert
            assertThat(regionBlockedStore.getSyncOrNull()).isTrue()
            assertThat(vaultsStore.getSync()).isEmpty()
        }

        @Test
        fun `GIVEN successful fetch WHEN fetchVaults THEN region flag reset to false`() = runTest {
            // Arrange
            seedStaleState()
            every { featureToggles.isIntegrationEnabled(any()) } returns true
            every { txHistoryFeatureToggles.isNewTxHistoryEnabled } returns false
            coEvery { api.getVaults(any()) } returns ApiResponse.Success(
                P2PEthPoolResponse(
                    error = null,
                    result = P2PEthPoolVaultsResponse(
                        network = P2PEthPoolNetworkDTO.MAINNET,
                        vaults = emptyList(),
                    ),
                ),
            )

            // Act
            repository.fetchVaults()

            // Assert
            assertThat(regionBlockedStore.getSyncOrNull()).isFalse()
            assertThat(vaultsStore.getSync()).isEmpty()
        }

        @Test
        fun `GIVEN integration toggle disabled WHEN fetchVaults THEN region flag false and empty vaults stored without api call`() =
            runTest {
                // Arrange
                seedStaleState()
                every { featureToggles.isIntegrationEnabled(StakingIntegrationID.P2PEthPool) } returns false

                // Act
                repository.fetchVaults()

                // Assert
                assertThat(regionBlockedStore.getSyncOrNull()).isFalse()
                assertThat(vaultsStore.getSync()).isEmpty()
                coVerify(exactly = 0) { api.getVaults(any()) }
            }

        @Test
        fun `GIVEN non 451 http error WHEN fetchVaults THEN region flag false and empty vaults stored`() = runTest {
            // Arrange
            seedStaleState()
            every { featureToggles.isIntegrationEnabled(any()) } returns true
            coEvery { api.getVaults(any()) } returns
                errorResponse(ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR)

            // Act
            repository.fetchVaults()

            // Assert
            assertThat(regionBlockedStore.getSyncOrNull()).isFalse()
            assertThat(vaultsStore.getSync()).isEmpty()
        }

        @Test
        fun `GIVEN network error WHEN fetchVaults THEN region flag false and empty vaults stored`() = runTest {
            // Arrange
            seedStaleState()
            every { featureToggles.isIntegrationEnabled(any()) } returns true
            coEvery { api.getVaults(any()) } returns apiError(ApiResponseError.NetworkException())

            // Act
            repository.fetchVaults()

            // Assert
            assertThat(regionBlockedStore.getSyncOrNull()).isFalse()
            assertThat(vaultsStore.getSync()).isEmpty()
        }

        @Test
        fun `GIVEN envelope error WHEN fetchVaults THEN region flag false and empty vaults stored`() = runTest {
            // Arrange
            seedStaleState()
            every { featureToggles.isIntegrationEnabled(any()) } returns true
            coEvery { api.getVaults(any()) } returns ApiResponse.Success(
                P2PEthPoolResponse(error = P2PEthPoolErrorDetailsDTO(127100, "net", null), result = null),
            )

            // Act
            repository.fetchVaults()

            // Assert
            assertThat(regionBlockedStore.getSyncOrNull()).isFalse()
            assertThat(vaultsStore.getSync()).isEmpty()
        }

        @Test
        fun `GIVEN vaults fetched WHEN fetchVaults THEN converted vaults stored`() = runTest {
            // Arrange
            every { featureToggles.isIntegrationEnabled(any()) } returns true
            every { txHistoryFeatureToggles.isNewTxHistoryEnabled } returns false
            coEvery { api.getVaults(any()) } returns ApiResponse.Success(
                P2PEthPoolResponse(
                    error = null,
                    result = P2PEthPoolVaultsResponse(
                        network = P2PEthPoolNetworkDTO.MAINNET,
                        vaults = listOf(vaultDTO(address = "0xABC")),
                    ),
                ),
            )

            // Act
            repository.fetchVaults()

            // Assert
            val expected = buildVault("0xABC", totalAssets = "100").copy(
                apy = BigDecimal("0.04500000"),
                baseApy = BigDecimal("0.04000000"),
            )
            assertThat(vaultsStore.getSync()).containsExactly(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvailabilityFlow {

        @Test
        fun `GIVEN all vaults full WHEN getStakingAvailability THEN emits Full`() = runTest {
            // Arrange
            seedAvailability(
                vaults = listOf(buildVault("0xABC", totalAssets = "999.95")),
                limits = limits("0xABC", limit = "1000"),
                regionBlocked = false,
            )

            // Act
            val result = repository.getStakingAvailability().first()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.Full(p2pOption(totalAssets = "999.95")))
        }

        @Test
        fun `GIVEN vault with remaining capacity WHEN getStakingAvailability THEN emits Available`() = runTest {
            // Arrange
            seedAvailability(
                vaults = listOf(buildVault("0xABC", totalAssets = "100")),
                limits = limits("0xABC", limit = "1000"),
                regionBlocked = false,
            )

            // Act
            val result = repository.getStakingAvailability().first()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.Available(p2pOption(totalAssets = "100")))
        }

        @Test
        fun `GIVEN region blocked flag WHEN getStakingAvailability THEN emits RegionUnavailable`() = runTest {
            // Arrange
            seedAvailability(
                vaults = emptyList(),
                limits = null,
                regionBlocked = true,
            )

            // Act
            val result = repository.getStakingAvailability().first()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.RegionUnavailable)
        }

        @Test
        fun `GIVEN no vaults WHEN getStakingAvailability THEN emits TemporaryUnavailable`() = runTest {
            // Arrange
            seedAvailability(
                vaults = emptyList(),
                limits = limits("0xABC", limit = "1000"),
                regionBlocked = false,
            )

            // Act
            val result = repository.getStakingAvailability().first()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.TemporaryUnavailable)
        }

        @Test
        fun `GIVEN limits not loaded WHEN getStakingAvailability THEN emits TemporaryUnavailable`() = runTest {
            // Arrange
            seedAvailability(
                vaults = listOf(buildVault("0xABC", totalAssets = "100")),
                limits = null,
                regionBlocked = false,
            )

            // Act
            val result = repository.getStakingAvailability().first()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.TemporaryUnavailable)
        }

        // Pins current behaviour: a vault without a limits entry counts as full
        @Test
        fun `GIVEN limits without vault key WHEN getStakingAvailability THEN emits Full`() = runTest {
            // Arrange
            seedAvailability(
                vaults = listOf(buildVault("0xABC", totalAssets = "100")),
                limits = emptyMap(),
                regionBlocked = false,
            )

            // Act
            val result = repository.getStakingAvailability().first()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.Full(p2pOption(totalAssets = "100")))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvailabilitySync {

        @Test
        fun `GIVEN region blocked flag WHEN getStakingAvailabilitySync THEN returns RegionUnavailable`() = runTest {
            // Arrange
            regionBlockedStore.store(true)

            // Act
            val result = repository.getStakingAvailabilitySync()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.RegionUnavailable)
        }

        @Test
        fun `GIVEN all vaults full WHEN getStakingAvailabilitySync THEN returns Full`() = runTest {
            // Arrange
            vaultsStore.store(listOf(buildVault("0xABC", totalAssets = "999.95")))
            limitsStore.store(limits("0xABC", limit = "1000"))

            // Act
            val result = repository.getStakingAvailabilitySync()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.Full(p2pOption(totalAssets = "999.95")))
        }

        @Test
        fun `GIVEN vault with remaining capacity WHEN getStakingAvailabilitySync THEN returns Available`() = runTest {
            // Arrange
            vaultsStore.store(listOf(buildVault("0xABC", totalAssets = "100")))
            limitsStore.store(limits("0xABC", limit = "1000"))

            // Act
            val result = repository.getStakingAvailabilitySync()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.Available(p2pOption(totalAssets = "100")))
        }

        @Test
        fun `GIVEN no vaults WHEN getStakingAvailabilitySync THEN returns TemporaryUnavailable`() = runTest {
            // Arrange
            vaultsStore.store(emptyList())
            limitsStore.store(limits("0xABC", limit = "1000"))

            // Act
            val result = repository.getStakingAvailabilitySync()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.TemporaryUnavailable)
        }

        @Test
        fun `GIVEN limits not loaded WHEN getStakingAvailabilitySync THEN returns TemporaryUnavailable`() = runTest {
            // Arrange
            vaultsStore.store(listOf(buildVault("0xABC", totalAssets = "100")))
            limitsStore.store(null)

            // Act
            val result = repository.getStakingAvailabilitySync()

            // Assert
            assertThat(result).isEqualTo(StakingAvailability.TemporaryUnavailable)
        }
    }
}