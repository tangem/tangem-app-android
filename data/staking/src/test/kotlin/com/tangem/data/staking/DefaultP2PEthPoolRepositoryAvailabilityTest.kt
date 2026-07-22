package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolNetworkDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolVaultsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.token.P2PEthPoolRegionBlockedStore
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.P2PVaultLimitsStore
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultP2PEthPoolRepositoryAvailabilityTest {

    private val api = mockk<P2PEthPoolApi>(relaxed = true)
    private val vaultsStore = mockk<P2PEthPoolVaultsStore>(relaxed = true)
    private val limitsStore = mockk<P2PVaultLimitsStore>(relaxed = true)
    private val tangemTechApi = mockk<TangemTechApi>(relaxed = true)
    private val featureToggles = mockk<StakingFeatureToggles>(relaxed = true)
    private val regionBlockedStore = mockk<P2PEthPoolRegionBlockedStore>(relaxed = true)

    private val repository = DefaultP2PEthPoolRepository(
        p2pEthPoolApi = api,
        p2pEthPoolVaultsStore = vaultsStore,
        p2pVaultLimitsStore = limitsStore,
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        stakingFeatureToggles = featureToggles,
        p2pEthPoolRegionBlockedStore = regionBlockedStore,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(api, vaultsStore, limitsStore, tangemTechApi, featureToggles, regionBlockedStore)
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

    @Suppress("UNCHECKED_CAST")
    private fun regionBlockedErrorResponse(): ApiResponse<P2PEthPoolResponse<P2PEthPoolVaultsResponse>> {
        return ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.UNAVAILABLE_FOR_LEGAL_REASONS,
                message = "451",
                errorBody = null,
            ),
        ) as ApiResponse<P2PEthPoolResponse<P2PEthPoolVaultsResponse>>
    }

    @Test
    fun `all vaults full - emits Full with option`() = runTest {
        every { vaultsStore.get() } returns flowOf(listOf(buildVault("0xABC", totalAssets = "999.95")))
        every { limitsStore.get() } returns MutableStateFlow(limits("0xABC", limit = "1000")) // remaining 0.05 <= 0.1
        every { regionBlockedStore.get() } returns MutableStateFlow(false)

        val result = repository.getStakingAvailability().first()

        assertThat(result).isInstanceOf(StakingAvailability.Full::class.java)
    }

    @Test
    fun `capacity available - emits Available`() = runTest {
        every { vaultsStore.get() } returns flowOf(listOf(buildVault("0xABC", totalAssets = "100")))
        every { limitsStore.get() } returns MutableStateFlow(limits("0xABC", limit = "1000")) // remaining 900 > 0.1
        every { regionBlockedStore.get() } returns MutableStateFlow(false)

        val result = repository.getStakingAvailability().first()

        assertThat(result).isInstanceOf(StakingAvailability.Available::class.java)
    }

    @Test
    fun `GIVEN region blocked flag WHEN getStakingAvailability THEN emits RegionUnavailable`() = runTest {
        // Arrange
        every { vaultsStore.get() } returns flowOf(emptyList())
        every { limitsStore.get() } returns MutableStateFlow(null)
        every { regionBlockedStore.get() } returns MutableStateFlow(true)

        // Act
        val result = repository.getStakingAvailability().first()

        // Assert
        assertThat(result).isInstanceOf(StakingAvailability.RegionUnavailable::class.java)
    }

    @Test
    fun `GIVEN region blocked flag WHEN getStakingAvailabilitySync THEN returns RegionUnavailable`() = runTest {
        // Arrange
        coEvery { regionBlockedStore.getSyncOrNull() } returns true

        // Act
        val result = repository.getStakingAvailabilitySync()

        // Assert
        assertThat(result).isInstanceOf(StakingAvailability.RegionUnavailable::class.java)
    }

    @Test
    fun `GIVEN 451 AND toggle on WHEN fetchVaults THEN region flag set true`() = runTest {
        // Arrange
        every { featureToggles.isIntegrationEnabled(any()) } returns true
        every { featureToggles.isRegionUnavailableHandlingEnabled() } returns true
        coEvery { api.getVaults(any()) } returns regionBlockedErrorResponse()

        // Act
        repository.fetchVaults()

        // Assert
        coVerify { regionBlockedStore.store(true) }
        coVerify { vaultsStore.store(emptyList()) }
    }

    @Test
    fun `GIVEN 451 AND toggle off WHEN fetchVaults THEN region flag stays false`() = runTest {
        // Arrange
        every { featureToggles.isIntegrationEnabled(any()) } returns true
        every { featureToggles.isRegionUnavailableHandlingEnabled() } returns false
        coEvery { api.getVaults(any()) } returns regionBlockedErrorResponse()

        // Act
        repository.fetchVaults()

        // Assert
        coVerify { regionBlockedStore.store(false) }
    }

    @Test
    fun `GIVEN successful fetch WHEN fetchVaults THEN region flag reset to false`() = runTest {
        // Arrange
        every { featureToggles.isIntegrationEnabled(any()) } returns true
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
        coVerify { regionBlockedStore.store(false) }
    }

    @Test
    fun `sync - all vaults full - returns Full with option`() = runTest {
        coEvery { vaultsStore.getSync() } returns listOf(buildVault("0xABC", totalAssets = "999.95"))
        coEvery { limitsStore.getSyncOrNull() } returns limits("0xABC", limit = "1000") // remaining 0.05 <= 0.1

        val result = repository.getStakingAvailabilitySync()

        assertThat(result).isInstanceOf(StakingAvailability.Full::class.java)
    }

    @Test
    fun `sync - capacity available - returns Available`() = runTest {
        coEvery { vaultsStore.getSync() } returns listOf(buildVault("0xABC", totalAssets = "100"))
        coEvery { limitsStore.getSyncOrNull() } returns limits("0xABC", limit = "1000") // remaining 900 > 0.1

        val result = repository.getStakingAvailabilitySync()

        assertThat(result).isInstanceOf(StakingAvailability.Available::class.java)
    }

    @Test
    fun `sync - empty vaults - returns TemporaryUnavailable`() = runTest {
        coEvery { vaultsStore.getSync() } returns emptyList()

        val result = repository.getStakingAvailabilitySync()

        assertThat(result).isInstanceOf(StakingAvailability.TemporaryUnavailable::class.java)
    }

    @Test
    fun `sync - limits not loaded - returns TemporaryUnavailable`() = runTest {
        coEvery { vaultsStore.getSync() } returns listOf(buildVault("0xABC", totalAssets = "100"))
        coEvery { limitsStore.getSyncOrNull() } returns null

        val result = repository.getStakingAvailabilitySync()

        assertThat(result).isInstanceOf(StakingAvailability.TemporaryUnavailable::class.java)
    }
}