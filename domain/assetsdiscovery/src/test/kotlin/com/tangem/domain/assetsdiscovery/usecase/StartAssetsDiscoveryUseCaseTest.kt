package com.tangem.domain.assetsdiscovery.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.assetsdiscovery.model.AssetsDiscoveryProgress
import com.tangem.domain.assetsdiscovery.repository.AssetsDiscoveryRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StartAssetsDiscoveryUseCaseTest {

    private val userWalletId = UserWalletId("011")

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val appliedCurrency = currencyFactory.ethereum
    private val lateDiscoveredCurrency = currencyFactory.createCoin(Blockchain.Polygon)

    private val manageCryptoCurrenciesUseCase = mockk<ManageCryptoCurrenciesUseCase>()
    private val analyticsEventHandler = mockk<AnalyticsEventHandler>(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(manageCryptoCurrenciesUseCase, analyticsEventHandler)
    }

    /**
     * Reproduces AND race: `WalletModel.init` triggers [StartAssetsDiscoveryUseCase.applyPendingAssetsDiscovery]
     * while [AssetsDiscoveryRepository.runDiscovery] is still appending tokens. A network discovered during the
     * (slow) apply window must NOT be wiped by the apply's store cleanup.
     */
    @Test
    fun `apply keeps tokens discovered concurrently while applying the snapshot`() = runTest {
        val repository = FakeAssetsDiscoveryRepository(
            initialCurrencies = listOf(appliedCurrency),
            pendingWalletIds = listOf(userWalletId),
        )
        // Simulate an in-flight discovery batch appending a new network during the apply call.
        coEvery { manageCryptoCurrenciesUseCase.invokeAndAwait(any(), any(), any(), any()) } coAnswers {
            repository.store.add(lateDiscoveredCurrency)
            Unit.right()
        }

        val useCase = createUseCase(repository)
        useCase.applyPendingAssetsDiscovery()
        advanceUntilIdle()

        assertThat(repository.store.map { it.id }).contains(lateDiscoveredCurrency.id)
        assertThat(repository.store.map { it.id }).doesNotContain(appliedCurrency.id)
    }

    @Test
    fun `apply removes every applied currency when nothing is discovered concurrently`() = runTest {
        val repository = FakeAssetsDiscoveryRepository(
            initialCurrencies = listOf(appliedCurrency, lateDiscoveredCurrency),
            pendingWalletIds = listOf(userWalletId),
        )
        coEvery { manageCryptoCurrenciesUseCase.invokeAndAwait(any(), any(), any(), any()) } returns Unit.right()

        val useCase = createUseCase(repository)
        useCase.applyPendingAssetsDiscovery()
        advanceUntilIdle()

        assertThat(repository.store).isEmpty()
        coVerify(exactly = 1) { manageCryptoCurrenciesUseCase.invokeAndAwait(any(), any(), any(), any()) }
        assertThat(repository.clearedPendingFlagFor).containsExactly(userWalletId)
    }

    @Test
    fun `apply retains tokens and keeps pending flag when applying fails`() = runTest {
        val repository = FakeAssetsDiscoveryRepository(
            initialCurrencies = listOf(appliedCurrency),
            pendingWalletIds = listOf(userWalletId),
        )
        coEvery {
            manageCryptoCurrenciesUseCase.invokeAndAwait(any(), any(), any(), any())
        } returns IllegalStateException("apply failed").left()

        val useCase = createUseCase(repository)
        useCase.applyPendingAssetsDiscovery()
        advanceUntilIdle()

        assertThat(repository.store.map { it.id }).containsExactly(appliedCurrency.id)
        assertThat(repository.clearedPendingFlagFor).isEmpty()
    }

    /**
     * Guards against premature pending-flag clearing: while `runDiscovery` is still in flight (the wallet
     * has an active sync job), `applyPendingAssetsDiscovery` must NOT clear the flag, otherwise tokens
     * discovered after the applied snapshot would be stranded if the app is killed before discovery finishes.
     */
    @Test
    fun `pending flag is not cleared while discovery is still running`() = runTest {
        val discoveryGate = CompletableDeferred<Unit>()
        val repository = FakeAssetsDiscoveryRepository(
            initialCurrencies = listOf(appliedCurrency),
            pendingWalletIds = listOf(userWalletId),
            onRunDiscovery = { discoveryGate.await() },
        )
        coEvery { manageCryptoCurrenciesUseCase.invokeAndAwait(any(), any(), any(), any()) } returns Unit.right()

        val useCase = createUseCase(repository)
        useCase(userWalletId) // registers an active sync job and parks inside runDiscovery
        runCurrent()
        useCase.applyPendingAssetsDiscovery()
        advanceUntilIdle()

        assertThat(repository.clearedPendingFlagFor).isEmpty()

        discoveryGate.complete(Unit) // let the discovery job finish so the test can complete cleanly
        advanceUntilIdle()
    }

    /**
     * invoke() must not mark the discovery complete when the apply fails: completeDiscovery clears the
     * pending flag, so calling it on failure would strand the still-unapplied tokens (no retry possible).
     */
    @Test
    fun `discovery is not marked complete when apply fails during invoke`() = runTest {
        val repository = FakeAssetsDiscoveryRepository(
            initialCurrencies = listOf(appliedCurrency),
            pendingWalletIds = emptyList(),
        )
        coEvery {
            manageCryptoCurrenciesUseCase.invokeAndAwait(any(), any(), any(), any())
        } returns IllegalStateException("apply failed").left()

        val useCase = createUseCase(repository)
        useCase(userWalletId)
        advanceUntilIdle()

        assertThat(repository.completedFor).isEmpty()
        assertThat(repository.store.map { it.id }).containsExactly(appliedCurrency.id)
    }

    @Test
    fun `discovery is marked complete when apply succeeds during invoke`() = runTest {
        val repository = FakeAssetsDiscoveryRepository(
            initialCurrencies = listOf(appliedCurrency),
            pendingWalletIds = emptyList(),
        )
        coEvery { manageCryptoCurrenciesUseCase.invokeAndAwait(any(), any(), any(), any()) } returns Unit.right()

        val useCase = createUseCase(repository)
        useCase(userWalletId)
        advanceUntilIdle()

        assertThat(repository.completedFor).containsExactly(userWalletId)
        assertThat(repository.store).isEmpty()
    }

    private fun TestScope.createUseCase(
        repository: AssetsDiscoveryRepository,
    ): StartAssetsDiscoveryUseCase = StartAssetsDiscoveryUseCase(
        assetsDiscoveryRepository = repository,
        manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
        appCoroutineScope = TestAppCoroutineScope(this),
        analyticsEventHandler = analyticsEventHandler,
    )

    private class FakeAssetsDiscoveryRepository(
        initialCurrencies: List<CryptoCurrency>,
        private val pendingWalletIds: List<UserWalletId>,
        private val onRunDiscovery: suspend () -> Unit = {},
    ) : AssetsDiscoveryRepository {

        val store: MutableList<CryptoCurrency> = initialCurrencies.toMutableList()
        val clearedPendingFlagFor: MutableList<UserWalletId> = mutableListOf()
        val completedFor: MutableList<UserWalletId> = mutableListOf()

        override suspend fun getDiscoveredCurrencies(userWalletId: UserWalletId): List<CryptoCurrency> = store.toList()

        override suspend fun removeAppliedCurrencies(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
            val appliedIds = currencies.mapTo(hashSetOf(), CryptoCurrency::id)
            store.removeAll { it.id in appliedIds }
        }

        override suspend fun clearDiscoveredTokens(userWalletId: UserWalletId) {
            store.clear()
        }

        override suspend fun getPendingDiscoveryWalletIds(): List<UserWalletId> = pendingWalletIds

        override suspend fun clearPendingFlag(userWalletId: UserWalletId) {
            clearedPendingFlagFor.add(userWalletId)
        }

        override suspend fun runDiscovery(userWalletId: UserWalletId) = onRunDiscovery()

        override suspend fun completeDiscovery(userWalletId: UserWalletId) {
            completedFor.add(userWalletId)
        }

        override fun observeDiscoveryProgress(userWalletId: UserWalletId): Flow<AssetsDiscoveryProgress> = emptyFlow()

        override fun acknowledgeCompletion(userWalletId: UserWalletId) = Unit
    }
}