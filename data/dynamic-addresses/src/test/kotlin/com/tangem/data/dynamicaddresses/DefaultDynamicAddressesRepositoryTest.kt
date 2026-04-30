package com.tangem.data.dynamicaddresses

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.GetDerivedXpubUseCase
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultDynamicAddressesRepositoryTest {

    private val walletAccountsFetcher: WalletAccountsFetcher = mockk(relaxed = true)
    private val walletAccountsSaver: WalletAccountsSaver = mockk(relaxed = true)
    private val accountsCRUDRepository: AccountsCRUDRepository = mockk(relaxed = true)
    private val walletManagersFacade: WalletManagersFacade = mockk(relaxed = true)
    private val featureToggles: DynamicAddressesFeatureToggles = mockk(relaxed = true)
    private val getDerivedXpubUseCase: GetDerivedXpubUseCase = mockk(relaxed = true)

    private val userWalletId: UserWalletId = mockk(relaxed = true)
    private val network: Network = mockk(relaxed = true) {
        every { id.rawId.value } returns SUPPORTED_NETWORK_ID
    }
    private val otherNetwork: Network = mockk(relaxed = true) {
        every { id.rawId.value } returns OTHER_SUPPORTED_NETWORK_ID
    }

    private val dispatchers: CoroutineDispatcherProvider = TestDispatchers(Dispatchers.Unconfined)

    private lateinit var repository: DefaultDynamicAddressesRepository

    @BeforeEach
    fun setUp() {
        clearMocks(walletManagersFacade, featureToggles, getDerivedXpubUseCase, answers = false)
        // Empty response → findToken returns null → getStatus emits DISABLED.
        val emptyResponse = mockk<GetWalletAccountsResponse>(relaxed = true) {
            every { accounts } returns emptyList()
        }
        every { walletAccountsFetcher.get(userWalletId) } returns flowOf(emptyResponse)
        coEvery { walletManagersFacade.enableXpubMode(any(), any(), any()) } returns SimpleResult.Success
        coEvery { walletManagersFacade.disableXpubMode(any(), any()) } returns SimpleResult.Success

        repository = DefaultDynamicAddressesRepository(
            walletAccountsFetcher = walletAccountsFetcher,
            walletAccountsSaver = walletAccountsSaver,
            accountsCRUDRepository = accountsCRUDRepository,
            walletManagersFacade = walletManagersFacade,
            dynamicAddressesFeatureToggles = featureToggles,
            getDerivedXpubUseCase = getDerivedXpubUseCase,
            dispatchers = dispatchers,
        )
    }

    @Test
    fun `GIVEN feature toggle off WHEN collect THEN probe is never called and flow emits false`() = runTest {
        // GIVEN
        every { featureToggles.isDynamicAddressesEnabled } returns false

        // WHEN
        val values = repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()

        // THEN
        assertThat(values).doesNotContain(true)
        coVerify(exactly = 0) {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN toggle on AND xpub is null WHEN collect THEN probe is not called AND cache is empty`() = runTest {
        // GIVEN
        every { featureToggles.isDynamicAddressesEnabled } returns true
        coEvery { getDerivedXpubUseCase(userWalletId, network) } returns null

        // WHEN
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()
        // Second collect should invoke xpub derivation again (nothing is cached for null xpub).
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()

        // THEN
        coVerify(exactly = 2) { getDerivedXpubUseCase(userWalletId, network) }
        coVerify(exactly = 0) {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN probe returns true WHEN collect THEN result is cached AND next collect skips probe`() = runTest {
        // GIVEN
        every { featureToggles.isDynamicAddressesEnabled } returns true
        coEvery { getDerivedXpubUseCase(userWalletId, network) } returns XPUB
        coEvery {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        } returns true

        // WHEN
        val firstValues = repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()
        val secondValues = repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()

        // THEN
        assertThat(firstValues).contains(true)
        assertThat(secondValues).contains(true)
        coVerify(exactly = 1) {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        }
    }

    @Test
    fun `GIVEN probe returns false WHEN collect twice THEN probe runs each time`() = runTest {
        // GIVEN
        every { featureToggles.isDynamicAddressesEnabled } returns true
        coEvery { getDerivedXpubUseCase(userWalletId, network) } returns XPUB
        coEvery {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        } returns false

        // WHEN
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()

        // THEN — negative results are not cached, so the probe must re-run.
        coVerify(exactly = 2) {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        }
    }

    @Test
    fun `GIVEN cached true WHEN enable succeeds THEN cache is invalidated and next probe runs again`() = runTest {
        // GIVEN — populate cache with a positive probe
        every { featureToggles.isDynamicAddressesEnabled } returns true
        coEvery { getDerivedXpubUseCase(userWalletId, network) } returns XPUB
        coEvery {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        } returns true
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).first()

        // WHEN — enable() succeeds and invalidates the cache
        repository.enable(userWalletId, network, XPUB)
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()

        // THEN — probe was called once before enable, again after invalidation
        coVerify(exactly = 2) {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        }
    }

    @Test
    fun `GIVEN cached true WHEN disable succeeds THEN cache is invalidated and next probe runs again`() = runTest {
        // GIVEN
        every { featureToggles.isDynamicAddressesEnabled } returns true
        coEvery { getDerivedXpubUseCase(userWalletId, network) } returns XPUB
        coEvery {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        } returns true
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).first()

        // WHEN
        repository.disable(userWalletId, network)
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()

        // THEN
        coVerify(exactly = 2) {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        }
    }

    @Test
    fun `GIVEN cache entry for one network WHEN invalidate other network THEN first entry is preserved`() = runTest {
        // GIVEN — cache populated for `network`
        every { featureToggles.isDynamicAddressesEnabled } returns true
        coEvery { getDerivedXpubUseCase(userWalletId, network) } returns XPUB
        coEvery {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        } returns true
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).first()

        // WHEN — disable invalidates a different network
        repository.disable(userWalletId, otherNetwork)
        repository.hasFundsOnAdditionalAddresses(userWalletId, network).toList()

        // THEN — `network` cache is untouched; probe was called only once (initial fill)
        coVerify(exactly = 1) {
            walletManagersFacade.probeHasFundsOnAdditionalAddresses(userWalletId, network, XPUB)
        }
    }

    private class TestDispatchers(dispatcher: CoroutineDispatcher) : CoroutineDispatcherProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val mainImmediate: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val single: CoroutineDispatcher = dispatcher
    }

    private companion object {
        const val XPUB = "xpub6-test-value"
        const val SUPPORTED_NETWORK_ID = "bitcoin"
        const val OTHER_SUPPORTED_NETWORK_ID = "litecoin"
    }
}