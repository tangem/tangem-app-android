package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolNetworkDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolResponse
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolVaultDTO
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolVaultsResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.P2PVaultLimitsStore
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.toggles.StakingFeatureToggles
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

    private companion object {
        const val PRODUCTION_VAULT_ADDRESS = "0x4c09BC47db288F998b33CD63BCc1b6ddCCe13F33"
        const val TEST_VAULT_ADDRESS = "0xB72668D6FF7A0e318F83097A754c6AEd0f8AF034"
    }

    private val api = mockk<P2PEthPoolApi>()
    private val store = mockk<P2PEthPoolVaultsStore>(relaxed = true)
    private val limitsStore = mockk<P2PVaultLimitsStore>(relaxed = true)
    private val tangemTechApi = mockk<TangemTechApi>(relaxed = true)
    private val featureToggles = mockk<StakingFeatureToggles> {
        every { isIntegrationEnabled(StakingIntegrationID.P2PEthPool) } returns true
    }
    private val repository = DefaultP2PEthPoolRepository(
        p2pEthPoolApi = api,
        p2pEthPoolVaultsStore = store,
        p2pVaultLimitsStore = limitsStore,
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        stakingFeatureToggles = featureToggles,
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
    fun `test vault address is filtered from getVaults result`() = runTest {
        coEvery { api.getVaults(any()) } returns successResponse(PRODUCTION_VAULT_ADDRESS, TEST_VAULT_ADDRESS)

        val vaults = repository.getVaults(P2PEthPoolNetwork.MAINNET).getOrNull()

        assertThat(vaults).hasSize(1)
        assertThat(vaults?.first()?.vaultAddress).isEqualTo(PRODUCTION_VAULT_ADDRESS)
    }

    @Test
    fun `production vault address passes filter`() = runTest {
        coEvery { api.getVaults(any()) } returns successResponse(PRODUCTION_VAULT_ADDRESS)

        val vaults = repository.getVaults(P2PEthPoolNetwork.MAINNET).getOrNull()

        assertThat(vaults).hasSize(1)
    }

    @Test
    fun `filter is case-insensitive`() = runTest {
        coEvery { api.getVaults(any()) } returns successResponse(
            TEST_VAULT_ADDRESS.uppercase(),
            TEST_VAULT_ADDRESS.lowercase(),
        )

        val vaults = repository.getVaults(P2PEthPoolNetwork.MAINNET).getOrNull()

        assertThat(vaults).isEmpty()
    }
}