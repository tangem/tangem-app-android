package com.tangem.features.earn.impl.overview

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnOverviewFetcher
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultEarnOverviewFetcherTest {

    private val currency = mockk<CryptoCurrency>()
    private val params = EarnOverviewFetcher.Params(userWalletId = USER_WALLET_ID, currency = currency)
    private val expectedRefreshParams = listOf(EarnProduct.Params(USER_WALLET_ID, currency))

    @Test
    fun `GIVEN one product refresh fails WHEN invoke THEN every product is refreshed and the failure is returned`() =
        runTest {
            // Arrange
            val cause = IllegalStateException("refresh failed")
            val first = TestEarnProduct(id = EarnProductId("first"), rank = 1)
            val failing = TestEarnProduct(
                id = EarnProductId("failing"),
                rank = 2,
                refreshResult = cause.left(),
            )
            val last = TestEarnProduct(id = EarnProductId("last"), rank = 3)
            val products = listOf(first, failing, last)
            val fetcher = createFetcher(*products.toTypedArray())

            // Act
            val actual = fetcher(params)

            // Assert
            assertThat(actual).isEqualTo(cause.left())
            assertThat(products.map { it.refreshCalls }).containsExactly(1, 1, 1)
            assertThat(products.map { it.refreshParams }).isEqualTo(List(products.size) { expectedRefreshParams })
        }

    @Test
    fun `GIVEN several products fail WHEN invoke THEN the failure of the lowest rank product is returned`() = runTest {
        // Arrange
        val firstCause = IllegalStateException("first failed")
        val secondCause = IllegalArgumentException("second failed")
        val first = TestEarnProduct(id = EarnProductId("first"), rank = 1)
        val secondFailing = TestEarnProduct(
            id = EarnProductId("second_failing"),
            rank = 5,
            refreshResult = secondCause.left(),
        )
        val firstFailing = TestEarnProduct(
            id = EarnProductId("first_failing"),
            rank = 2,
            refreshResult = firstCause.left(),
        )
        val products = listOf(first, firstFailing, secondFailing)
        val fetcher = createFetcher(secondFailing, first, firstFailing)

        // Act
        val actual = fetcher(params)

        // Assert
        assertThat(actual).isEqualTo(firstCause.left())
        assertThat(products.map { it.refreshCalls }).containsExactly(1, 1, 1)
        assertThat(products.map { it.refreshParams }).isEqualTo(List(products.size) { expectedRefreshParams })
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN all products refresh successfully WHEN invoke THEN unit is returned`(model: SuccessModel) = runTest {
        // Arrange
        val products = List(model.productCount) { index ->
            TestEarnProduct(id = EarnProductId("product_$index"), rank = index)
        }
        val fetcher = createFetcher(*products.toTypedArray())

        // Act
        val actual: Either<Throwable, Unit> = fetcher(params)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        assertThat(products.map { it.refreshCalls }).isEqualTo(List(model.productCount) { 1 })
        assertThat(products.map { it.refreshParams })
            .isEqualTo(List(model.productCount) { expectedRefreshParams })
    }

    @Test
    fun `GIVEN one product refresh is suspended WHEN invoke THEN the other product is refreshed without waiting`() =
        runTest {
            // Arrange
            val gate = CompletableDeferred<Unit>()
            val suspendedDelegate = TestEarnProduct(id = EarnProductId("suspended"), rank = 1)
            val suspended = SuspendedRefreshEarnProduct(delegate = suspendedDelegate, gate = gate)
            val other = TestEarnProduct(id = EarnProductId("other"), rank = 2)
            val fetcher = createFetcher(suspended, other)

            // Act
            val result = async { fetcher(params) }
            runCurrent()
            val refreshCallsWhileSuspended = listOf(suspendedDelegate, other).map { it.refreshCalls }
            val completedWhileSuspended = result.isCompleted
            gate.complete(Unit)
            val actual = result.await()

            // Assert
            assertThat(refreshCallsWhileSuspended).containsExactly(1, 1)
            assertThat(completedWhileSuspended).isFalse()
            assertThat(actual).isEqualTo(Unit.right())
        }

    private fun createFetcher(vararg products: EarnProduct) = DefaultEarnOverviewFetcher(
        registry = EarnProductsRegistry(products = products.toSet(), rateSources = emptySet()),
    )

    private class SuspendedRefreshEarnProduct(
        private val delegate: TestEarnProduct,
        private val gate: CompletableDeferred<Unit>,
    ) : EarnProduct by delegate {

        override suspend fun refresh(params: EarnProduct.Params): Either<Throwable, Unit> {
            delegate.refresh(params)
            gate.await()
            return Unit.right()
        }
    }

    data class SuccessModel(val productCount: Int)

    private fun provideTestModels() = listOf(
        SuccessModel(productCount = 0),
        SuccessModel(productCount = 1),
        SuccessModel(productCount = 3),
    )
}