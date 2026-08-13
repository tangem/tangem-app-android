package com.tangem.data.polymarket.flow

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.data.polymarket.store.WalletIdWithPredictionStatus
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionAccountStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The producer's contract is that it emits without waiting for anything remote. Its value is combined with the
 * other accounts' statuses, and `combine` withholds all of them until every source has emitted at least once —
 * so a producer that waits for a quote would stall the wallet, not just this account.
 */
internal class DefaultPredictionAccountStatusProducerTest {

    private val quoteSupplier: SingleQuoteStatusSupplier = mockk()

    @Test
    fun `GIVEN nothing cached and no quote WHEN produce THEN it emits at once`() = runTest {
        // Arrange — a quote flow that never emits, and a store nothing was ever written to
        every { quoteSupplier.invoke(any<SingleQuoteStatusProducer.Params>()) } returns MutableSharedFlow()
        val producer = createProducer(testScope = this)

        // Act
        val actual = producer.produce().first()

        // Assert
        assertThat(actual).isEqualTo(PredictionAccountStatusValue.Loading)
    }

    @Test
    fun `GIVEN a cached balance and no rate WHEN produce THEN the value is still emitted`() = runTest {
        // Arrange
        every { quoteSupplier.invoke(any<SingleQuoteStatusProducer.Params>()) } returns quoteFlow(rate = null)
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE)

        // Act — the first emission is the loading one the pending quote produces; this is the one after it
        val actual = createProducer(testScope = this, store = store).produce()
            .first { it !is PredictionAccountStatusValue.Loading }

        // Assert
        assertThat(actual).isEqualTo(ACTIVE.copy(fiatRate = null))
    }

    @Test
    fun `GIVEN a cached balance and a rate WHEN produce THEN the rate is mixed in`() = runTest {
        // Arrange
        val rate = BigDecimal("0.92")
        every { quoteSupplier.invoke(any<SingleQuoteStatusProducer.Params>()) } returns quoteFlow(rate = rate)
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE)

        // Act
        val actual = createProducer(testScope = this, store = store).produce()
            .first { it !is PredictionAccountStatusValue.Loading }

        // Assert
        assertThat(actual).isEqualTo(ACTIVE.copy(fiatRate = rate))
    }

    @Test
    fun `GIVEN a cached balance and a pending quote WHEN produce THEN it reports loading, not a failed price`() =
        runTest {
            // Arrange
            every { quoteSupplier.invoke(any<SingleQuoteStatusProducer.Params>()) } returns MutableSharedFlow()
            val store = createStore(testScope = this)
            store.store(userWalletId = WALLET, value = ACTIVE)

            // Act
            val actual = createProducer(testScope = this, store = store).produce().first()

            // Assert — an Active without a rate resolves to a failed contribution, which would blank the wallet
            assertThat(actual).isEqualTo(PredictionAccountStatusValue.Loading)
        }

    @Test
    fun `GIVEN a cached onboarding status WHEN produce THEN the quote does not change it`() = runTest {
        // Arrange
        every { quoteSupplier.invoke(any<SingleQuoteStatusProducer.Params>()) } returns MutableSharedFlow()
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ONBOARDING)

        // Act
        val actual = createProducer(testScope = this, store = store).produce().first()

        // Assert
        assertThat(actual).isEqualTo(ONBOARDING)
    }

    private fun quoteFlow(rate: BigDecimal?): Flow<QuoteStatus> {
        val value = if (rate == null) {
            QuoteStatus.Empty
        } else {
            QuoteStatus.Data(
                source = StatusSource.ACTUAL,
                fiatRate = rate,
                fiatRateUSD = rate,
                priceChange = BigDecimal.ZERO,
            )
        }

        return flowOf(QuoteStatus(rawCurrencyId = CryptoCurrency.RawID("usd-coin"), value = value))
    }

    private fun createStore(testScope: TestScope) = PredictionAccountStatusStore(
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = MockStateDataStore<WalletIdWithPredictionStatus>(default = emptyMap()),
        scope = TestAppCoroutineScope(testScope),
    )

    private fun createProducer(
        testScope: TestScope,
        store: PredictionAccountStatusStore = createStore(testScope),
    ) = DefaultPredictionAccountStatusProducer(
        params = PredictionAccountStatusProducer.Params(userWalletId = WALLET),
        flowProducerTools = mockk(),
        statusStore = store,
        singleQuoteStatusSupplier = quoteSupplier,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private companion object {
        val WALLET = UserWalletId("011")

        val ACTIVE = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal("40"),
            fiatRate = null,
            isTradingAllowed = true,
        )

        val ONBOARDING = PredictionAccountStatusValue.Onboarding(
            source = StatusSource.ACTUAL,
            stage = PredictionAccountStatusValue.Onboarding.Stage.APPROVING,
        )
    }
}