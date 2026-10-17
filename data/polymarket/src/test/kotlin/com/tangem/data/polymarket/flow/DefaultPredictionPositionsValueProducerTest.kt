package com.tangem.data.polymarket.flow

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.data.polymarket.store.PredictionPositionsValueStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionPositionsValueProducer
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultPredictionPositionsValueProducerTest {

    private val store = PredictionPositionsValueStore()

    @Test
    fun `GIVEN nothing stored WHEN produce THEN it emits unknown at once`() = runTest {
        // Act
        val actual = createProducer().produce().first()

        // Assert
        assertThat(actual).isEqualTo(PredictionPositionsValue.Unknown)
    }

    @Test
    fun `GIVEN a stored value WHEN produce THEN it emits the value once`() = runTest {
        // Arrange
        store.store(userWalletId = WALLET, value = VALUE)

        // Act
        createProducer().produce().test {
            // Assert
            assertThat(awaitItem()).isEqualTo(VALUE)
            expectNoEvents()
        }
    }

    @Test
    fun `GIVEN a subscriber WHEN a value is stored THEN it follows the store`() = runTest {
        createProducer().produce().test {
            // Arrange
            assertThat(awaitItem()).isEqualTo(PredictionPositionsValue.Unknown)

            // Act
            store.store(userWalletId = WALLET, value = VALUE)
            store.markUnrefreshed(userWalletId = WALLET)

            // Assert
            assertThat(awaitItem()).isEqualTo(VALUE)
            assertThat(awaitItem()).isEqualTo(VALUE.copy(source = StatusSource.ONLY_CACHE))
        }
    }

    private fun createProducer() = DefaultPredictionPositionsValueProducer(
        params = PredictionPositionsValueProducer.Params(userWalletId = WALLET),
        flowProducerTools = mockk(),
        store = store,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private companion object {
        val WALLET = UserWalletId("011")
        val VALUE = PredictionPositionsValue.Known(amount = BigDecimal("7.5"), source = StatusSource.ACTUAL)
    }
}