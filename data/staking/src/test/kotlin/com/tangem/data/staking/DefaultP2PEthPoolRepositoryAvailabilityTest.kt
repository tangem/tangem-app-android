package com.tangem.data.staking

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.P2PVaultLimitsStore
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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

    private val repository = DefaultP2PEthPoolRepository(
        p2pEthPoolApi = api,
        p2pEthPoolVaultsStore = vaultsStore,
        p2pVaultLimitsStore = limitsStore,
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        stakingFeatureToggles = featureToggles,
    )

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

    @Test
    fun `all vaults full - emits Full with option`() = runTest {
        every { vaultsStore.get() } returns flowOf(listOf(buildVault("0xABC", totalAssets = "999.95")))
        every { limitsStore.get() } returns MutableStateFlow(limits("0xABC", limit = "1000")) // remaining 0.05 <= 0.1

        val result = repository.getStakingAvailability().first()

        assertThat(result).isInstanceOf(StakingAvailability.Full::class.java)
    }

    @Test
    fun `capacity available - emits Available`() = runTest {
        every { vaultsStore.get() } returns flowOf(listOf(buildVault("0xABC", totalAssets = "100")))
        every { limitsStore.get() } returns MutableStateFlow(limits("0xABC", limit = "1000")) // remaining 900 > 0.1

        val result = repository.getStakingAvailability().first()

        assertThat(result).isInstanceOf(StakingAvailability.Available::class.java)
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