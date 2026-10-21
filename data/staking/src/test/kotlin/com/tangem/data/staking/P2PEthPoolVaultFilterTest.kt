package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.ethpool.P2PEthPoolApi
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolNetworkDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolVaultDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolVaultsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.data.staking.store.P2PEthPoolRegionBlockedStore
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.data.staking.store.P2PVaultLimitsStore
import com.tangem.store.datasource.txhistory.db.dao.P2PEthPoolVaultDao
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class P2PEthPoolVaultFilterTest {

    private val api = mockk<P2PEthPoolApi>()
    private val store = mockk<P2PEthPoolVaultsStore>(relaxed = true)
    private val limitsStore = mockk<P2PVaultLimitsStore>(relaxed = true)
    private val tangemTechApi = mockk<TangemTechApi>(relaxed = true)
    private val featureToggles = mockk<StakingFeatureToggles> {
        every { isIntegrationEnabled(StakingIntegrationID.P2PEthPool) } returns true
    }
    private val regionBlockedStore = mockk<P2PEthPoolRegionBlockedStore>(relaxed = true)
    private val vaultDao = mockk<P2PEthPoolVaultDao>(relaxed = true)
    private val txHistoryFeatureToggles = mockk<TxHistoryFeatureToggles>(relaxed = true)
    private val repository = DefaultP2PEthPoolRepository(
        p2pEthPoolApi = api,
        p2pEthPoolVaultsStore = store,
        p2pVaultLimitsStore = limitsStore,
        p2pEthPoolVaultDao = vaultDao,
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        stakingFeatureToggles = featureToggles,
        txHistoryFeatureToggle = txHistoryFeatureToggles,
        p2pEthPoolRegionBlockedStore = regionBlockedStore,
    )

    private fun buildVaultDTO(address: String) = P2PEthPoolVaultDTO(
        vaultAddress = address,
        displayName = "Vault $address",
        apy = BigDecimal("4.5"),
        baseApy = BigDecimal("4.0"),
        capacity = BigDecimal("10000"),
        totalAssets = BigDecimal("5000"),
        feePercent = BigDecimal("10"),
        isPrivate = false,
        isGenesis = false,
        isSmoothingPool = true,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 0L,
    )

    private fun expectedVault(address: String) = P2PEthPoolVault(
        vaultAddress = address,
        displayName = "Vault $address",
        apy = BigDecimal("0.04500000"),
        baseApy = BigDecimal("0.04000000"),
        capacity = BigDecimal("10000"),
        totalAssets = BigDecimal("5000"),
        feePercent = BigDecimal("10"),
        isPrivate = false,
        isGenesis = false,
        isSmoothingPool = true,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 0L,
    )

    private fun successResponse(vararg addresses: String) = ApiResponse.Success(
        P2PEthPoolResponse(
            error = null,
            result = P2PEthPoolVaultsResponse(
                network = P2PEthPoolNetworkDTO.MAINNET,
                vaults = addresses.map { buildVaultDTO(it) },
            ),
        ),
    )

    @Test
    fun `GIVEN production and test vaults WHEN getVaults THEN only production vault returned`() = runTest {
        // Arrange
        coEvery { api.getVaults(any()) } returns successResponse(PRODUCTION_VAULT_ADDRESS, TEST_VAULT_ADDRESS)

        // Act
        val vaults = repository.getVaults(P2PEthPoolNetwork.MAINNET).getOrNull()

        // Assert
        assertThat(vaults).containsExactly(expectedVault(PRODUCTION_VAULT_ADDRESS))
    }

    @Test
    fun `GIVEN production vault only WHEN getVaults THEN vault kept`() = runTest {
        // Arrange
        coEvery { api.getVaults(any()) } returns successResponse(PRODUCTION_VAULT_ADDRESS)

        // Act
        val vaults = repository.getVaults(P2PEthPoolNetwork.MAINNET).getOrNull()

        // Assert
        assertThat(vaults).containsExactly(expectedVault(PRODUCTION_VAULT_ADDRESS))
    }

    @Test
    fun `GIVEN test vault address in upper and lower case WHEN getVaults THEN both filtered out`() = runTest {
        // Arrange
        coEvery { api.getVaults(any()) } returns successResponse(
            TEST_VAULT_ADDRESS.uppercase(),
            TEST_VAULT_ADDRESS.lowercase(),
        )

        // Act
        val vaults = repository.getVaults(P2PEthPoolNetwork.MAINNET).getOrNull()

        // Assert
        assertThat(vaults).isEmpty()
    }

    private companion object {
        const val PRODUCTION_VAULT_ADDRESS = "0x4c09BC47db288F998b33CD63BCc1b6ddCCe13F33"
        const val TEST_VAULT_ADDRESS = "0xB72668D6FF7A0e318F83097A754c6AEd0f8AF034"
    }
}