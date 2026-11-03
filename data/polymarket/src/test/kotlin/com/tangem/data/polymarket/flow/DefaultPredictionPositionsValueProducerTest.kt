package com.tangem.data.polymarket.flow

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.converter.PredictionAccountStatusValueDTOConverter
import com.tangem.data.polymarket.store.PredictionAccountStatusStore
import com.tangem.data.polymarket.store.WalletIdWithPredictionStatusDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.account.PredictionPositionsValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.flow.PredictionPositionsValueProducer
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultPredictionPositionsValueProducerTest {

    @Test
    fun `GIVEN nothing stored WHEN produce THEN it emits unknown at once`() = runTest {
        // Arrange
        val store = createStore(testScope = this)

        // Act
        val actual = createProducer(store).produce().first()

        // Assert
        assertThat(actual).isEqualTo(PredictionPositionsValue.Unknown)
    }

    @Test
    fun `GIVEN an account that is not active WHEN produce THEN it emits unknown`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = PredictionAccountStatusValue.NotOnboarded)

        // Act
        val actual = createProducer(store).produce().first()

        // Assert
        assertThat(actual).isEqualTo(PredictionPositionsValue.Unknown)
    }

    @Test
    fun `GIVEN an active account WHEN produce THEN it emits its positions once`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE.copy(positions = VALUE))

        // Act
        createProducer(store).produce().test {
            // Assert
            assertThat(awaitItem()).isEqualTo(VALUE)
            expectNoEvents()
        }
    }

    @Test
    fun `GIVEN an un-refreshed account WHEN produce THEN its positions are flagged as cached`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE.copy(positions = VALUE))
        store.markUnrefreshed(userWalletId = WALLET)

        // Act
        val actual = createProducer(store).produce().first()

        // Assert
        assertThat(actual).isEqualTo(VALUE.copy(source = StatusSource.ONLY_CACHE))
    }

    @Test
    fun `GIVEN a subscriber WHEN the balance changes but the positions do not THEN nothing is emitted`() = runTest {
        // Arrange
        val store = createStore(testScope = this)
        store.store(userWalletId = WALLET, value = ACTIVE.copy(positions = VALUE))

        createProducer(store).produce().test {
            assertThat(awaitItem()).isEqualTo(VALUE)

            // Act
            store.store(userWalletId = WALLET, value = ACTIVE.copy(balance = BigDecimal("41"), positions = VALUE))

            // Assert
            expectNoEvents()
        }
    }

    private fun createStore(testScope: TestScope) = PredictionAccountStatusStore(
        dtoConverter = PredictionAccountStatusValueDTOConverter(
            collateralCurrencyFactory = mockk { every { createDepositTarget() } returns COLLATERAL },
        ),
        runtimeStore = RuntimeSharedStore(),
        persistenceDataStore = MockStateDataStore<WalletIdWithPredictionStatusDTO>(default = emptyMap()),
        scope = TestAppCoroutineScope(testScope),
    )

    private fun createProducer(store: PredictionAccountStatusStore) = DefaultPredictionPositionsValueProducer(
        params = PredictionPositionsValueProducer.Params(userWalletId = WALLET),
        flowProducerTools = mockk(),
        statusStore = store,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private companion object {
        val WALLET = UserWalletId("011")
        val COLLATERAL: CryptoCurrency.Token = mockk(relaxed = true)
        val VALUE = PredictionPositionsValue.Known(amount = BigDecimal("7.5"), source = StatusSource.ACTUAL)
        val ACTIVE = PredictionAccountStatusValue.Active(
            source = StatusSource.ACTUAL,
            balance = BigDecimal("40"),
            fiatRate = null,
            depositCurrency = COLLATERAL,
            depositAddress = null,
        )
    }
}