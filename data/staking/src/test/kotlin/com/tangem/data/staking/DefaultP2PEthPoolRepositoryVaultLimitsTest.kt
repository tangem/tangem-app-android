package com.tangem.data.staking

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.staking.StakeKitRepositoryFixtures.apiError
import com.tangem.data.staking.store.P2PEthPoolRegionBlockedStore
import com.tangem.data.staking.store.P2PVaultLimitsStore
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsSettingsResponse
import com.tangem.datasource.api.tangemTech.models.StakingSettingsDTO
import com.tangem.datasource.api.tangemTech.models.VaultSettingsDTO
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.grow.datasource.ethpool.P2PEthPoolApi
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolNetworkDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolVaultDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolVaultsResponse
import com.tangem.store.datasource.txhistory.db.dao.P2PEthPoolVaultDao
import com.tangem.store.datasource.txhistory.db.entity.staking.P2PEthPoolVaultEntity
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
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultP2PEthPoolRepositoryVaultLimitsTest {

    private val api: P2PEthPoolApi = mockk()
    private val vaultsStore: P2PEthPoolVaultsStore = mockk(relaxed = true)
    private val limitsStore = P2PVaultLimitsStore()
    private val tangemTechApi: TangemTechApi = mockk()
    private val featureToggles: StakingFeatureToggles = mockk(relaxed = true)
    private val regionBlockedStore: P2PEthPoolRegionBlockedStore = mockk(relaxed = true)
    private val vaultDao: P2PEthPoolVaultDao = mockk()

    private val repository = DefaultP2PEthPoolRepository(
        p2pEthPoolApi = api,
        p2pEthPoolVaultsStore = vaultsStore,
        p2pVaultLimitsStore = limitsStore,
        p2pEthPoolVaultDao = vaultDao,
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        stakingFeatureToggles = featureToggles,
        p2pEthPoolRegionBlockedStore = regionBlockedStore,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(api, tangemTechApi, vaultDao)
        limitsStore.clear()
        coEvery { vaultDao.upsert(any()) } just Runs
    }

    private fun vaultDTO(
        address: String = "0xVault1",
        apy: BigDecimal = BigDecimal("4.5"),
        baseApy: BigDecimal = BigDecimal("4.0"),
    ) = P2PEthPoolVaultDTO(
        vaultAddress = address,
        displayName = "Vault 1",
        apy = apy,
        baseApy = baseApy,
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

    private fun vaultsOk(vararg dtos: P2PEthPoolVaultDTO): ApiResponse<P2PEthPoolResponse<P2PEthPoolVaultsResponse>> =
        ApiResponse.Success(
            P2PEthPoolResponse(
                error = null,
                result = P2PEthPoolVaultsResponse(network = P2PEthPoolNetworkDTO.MAINNET, vaults = dtos.toList()),
            ),
        )

    private fun coinsSettings(vararg vaults: VaultSettingsDTO) =
        ApiResponse.Success(CoinsSettingsResponse(StakingSettingsDTO(vaults.toList())))

    private val expectedVault = P2PEthPoolVault(
        vaultAddress = "0xVault1",
        displayName = "Vault 1",
        apy = BigDecimal("0.04500000"),
        baseApy = BigDecimal("0.04000000"),
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

    private val expectedEntity = P2PEthPoolVaultEntity(
        vaultAddress = "0xVault1",
        displayName = "Vault 1",
        apy = "4.5",
        baseApy = "4.0",
        capacity = "1000",
        totalAssets = "100",
        feePercent = "10",
        isPrivate = false,
        isGenesis = false,
        isSmoothingPool = true,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 0L,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchVaultLimits {

        @Test
        fun `GIVEN vaults with limits WHEN fetchVaultLimits THEN stored by lowercased address`() = runTest {
            // Arrange
            coEvery { tangemTechApi.getCoinsSettings() } returns coinsSettings(
                VaultSettingsDTO("0xABC", BigDecimal("32"), BigDecimal("1.1")),
                VaultSettingsDTO("0xdef", BigDecimal("5"), null),
            )

            // Act
            repository.fetchVaultLimits()

            // Assert
            val expected = mapOf(
                "0xabc" to VaultLimitInfo(BigDecimal("32"), BigDecimal("1.1")),
                "0xdef" to VaultLimitInfo(BigDecimal("5"), null),
            )
            assertThat(limitsStore.getSyncOrNull()).isEqualTo(expected)
        }

        @Test
        fun `GIVEN vault with null limit WHEN fetchVaultLimits THEN vault skipped`() = runTest {
            // Arrange
            coEvery { tangemTechApi.getCoinsSettings() } returns coinsSettings(
                VaultSettingsDTO("0xABC", null, BigDecimal("1")),
                VaultSettingsDTO("0xDEF", BigDecimal("7"), null),
            )

            // Act
            repository.fetchVaultLimits()

            // Assert
            assertThat(limitsStore.getSyncOrNull()).isEqualTo(mapOf("0xdef" to VaultLimitInfo(BigDecimal("7"), null)))
        }

        // Pins current behaviour: missing staking section stores empty limits, so every vault counts as full
        @Test
        fun `GIVEN staking section null WHEN fetchVaultLimits THEN empty map stored`() = runTest {
            // Arrange
            coEvery { tangemTechApi.getCoinsSettings() } returns ApiResponse.Success(CoinsSettingsResponse(null))

            // Act
            repository.fetchVaultLimits()

            // Assert
            assertThat(limitsStore.getSyncOrNull()).isEqualTo(emptyMap<String, VaultLimitInfo>())
        }

        @Test
        fun `GIVEN api error WHEN fetchVaultLimits THEN failure swallowed and limits stay unset`() = runTest {
            // Arrange
            coEvery { tangemTechApi.getCoinsSettings() } returns apiError(ApiResponseError.NetworkException())

            // Act
            val error = runCatching { repository.fetchVaultLimits() }.exceptionOrNull()

            // Assert
            assertThat(error).isNull()
            assertThat(limitsStore.getSyncOrNull()).isNull()
        }

        @Test
        fun `GIVEN limits stored before WHEN fetchVaultLimits fails THEN previous limits kept`() = runTest {
            // Arrange
            val previous = mapOf("0xabc" to VaultLimitInfo(BigDecimal.ONE, null))
            limitsStore.store(previous)
            coEvery { tangemTechApi.getCoinsSettings() } returns apiError(ApiResponseError.NetworkException())

            // Act
            repository.fetchVaultLimits()

            // Assert
            assertThat(limitsStore.getSyncOrNull()).isEqualTo(previous)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetVaults {

        @Test
        fun `GIVEN vaults WHEN getVaults THEN raw vaults upserted and domain vaults rescaled`() = runTest {
            // Arrange
            coEvery { api.getVaults("mainnet") } returns vaultsOk(vaultDTO())

            // Act
            val actual = repository.getVaults(P2PEthPoolNetwork.MAINNET)

            // Assert
            assertThat(actual).isEqualTo(listOf(expectedVault).right())
            coVerify { vaultDao.upsert(listOf(expectedEntity)) }
        }

        // Pins current behaviour: upsert runs before the test-vault filter
        @Test
        fun `GIVEN test vault WHEN getVaults THEN test vault persisted but filtered from result`() =
            runTest {
                // Arrange
                val testAddress = "0xb72668d6ff7a0e318f83097a754c6aed0f8af034"
                coEvery { api.getVaults("mainnet") } returns vaultsOk(vaultDTO(), vaultDTO(address = testAddress))

                // Act
                val actual = repository.getVaults(P2PEthPoolNetwork.MAINNET)

                // Assert
                assertThat(actual).isEqualTo(listOf(expectedVault).right())
                coVerify { vaultDao.upsert(listOf(expectedEntity, expectedEntity.copy(vaultAddress = testAddress))) }
            }

        @Test
        fun `GIVEN api error WHEN getVaults THEN unknown error and dao untouched`() = runTest {
            // Arrange
            val cause = ApiResponseError.NetworkException()
            coEvery { api.getVaults("mainnet") } returns apiError<P2PEthPoolResponse<P2PEthPoolVaultsResponse>>(cause)

            // Act
            val actual = repository.getVaults(P2PEthPoolNetwork.MAINNET)

            // Assert
            assertThat(actual).isEqualTo(StakingError.UnknownError(cause).left())
            coVerify(exactly = 0) { vaultDao.upsert(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetPersistedVaultsFlow {

        @Test
        fun `GIVEN persisted entity WHEN getPersistedVaultsFlow THEN apy rescaled from entity`() = runTest {
            // Arrange
            every { vaultDao.getAllAsFlow() } returns flowOf(listOf(expectedEntity))

            // Act
            val actual = repository.getPersistedVaultsFlow().first()

            // Assert
            assertThat(actual).isEqualTo(listOf(expectedVault))
        }
    }
}