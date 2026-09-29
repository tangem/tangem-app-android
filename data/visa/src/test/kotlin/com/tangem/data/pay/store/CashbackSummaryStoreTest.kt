package com.tangem.data.pay.store

import androidx.datastore.core.DataStore
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.spend.datasource.pay.store.entity.CashbackSummaryDM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.IOException
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CashbackSummaryStoreTest {

    private lateinit var store: CashbackSummaryStore

    @BeforeEach
    fun setUp() {
        store = CashbackSummaryStore(dataStore = MockStateDataStore<Map<String, CashbackSummaryDM>>(default = emptyMap()))
    }

    @Test
    fun `GIVEN nothing cached WHEN get THEN emits null`() = runTest {
        // Act
        val actual = store.get(WALLET_A).first()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN enabled summary WHEN store THEN get emits it`() = runTest {
        // Act
        store.store(WALLET_A, ENABLED)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(ENABLED)
    }

    @Test
    fun `GIVEN deactivated summary WHEN store THEN get emits it`() = runTest {
        // Act
        store.store(WALLET_A, CashbackSummary.Deactivated)

        // Assert
        assertThat(store.get(WALLET_A).first()).isEqualTo(CashbackSummary.Deactivated)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN cached summary WHEN not cacheable summary stored THEN entry removed`(model: RemovalModel) = runTest {
        // Arrange
        store.store(WALLET_A, ENABLED)

        // Act
        store.store(WALLET_A, model.summary)

        // Assert
        assertThat(store.get(WALLET_A).first()).isNull()
    }

    @Test
    fun `GIVEN two cached wallets WHEN one removed THEN other kept`() = runTest {
        // Arrange
        store.store(WALLET_A, ENABLED)
        store.store(WALLET_B, CashbackSummary.Deactivated)

        // Act
        store.remove(listOf(WALLET_A))

        // Assert
        assertThat(store.get(WALLET_A).first()).isNull()
        assertThat(store.get(WALLET_B).first()).isEqualTo(CashbackSummary.Deactivated)
    }

    @Test
    fun `GIVEN observed wallet WHEN another wallet updated THEN no new emission`() = runTest {
        // Arrange
        store.store(WALLET_A, ENABLED)

        store.get(WALLET_A).test {
            assertThat(awaitItem()).isEqualTo(ENABLED)

            // Act
            store.store(WALLET_B, CashbackSummary.Deactivated)

            // Assert
            expectNoEvents()
        }
    }

    @Test
    fun `GIVEN write fails WHEN store THEN does not throw`() = runTest {
        // Arrange
        val failingStore = CashbackSummaryStore(dataStore = ThrowingDataStore())

        // Act
        val exception = runCatching { failingStore.store(WALLET_A, ENABLED) }.exceptionOrNull()

        // Assert
        assertThat(exception).isNull()
    }

    @Test
    fun `GIVEN write fails WHEN remove THEN does not throw`() = runTest {
        // Arrange
        val failingStore = CashbackSummaryStore(dataStore = ThrowingDataStore())

        // Act
        val exception = runCatching { failingStore.remove(listOf(WALLET_A)) }.exceptionOrNull()

        // Assert
        assertThat(exception).isNull()
    }

    @Test
    fun `GIVEN read fails WHEN get THEN emits null`() = runTest {
        // Arrange
        val failingStore = CashbackSummaryStore(dataStore = ThrowingDataStore())

        // Act
        val actual = failingStore.get(WALLET_A).first()

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels() = listOf(
        RemovalModel(description = "disabled", summary = CashbackSummary.Disabled),
        RemovalModel(description = "unknown", summary = CashbackSummary.Unknown),
    )

    internal class RemovalModel(val summary: CashbackSummary, private val description: String) {
        override fun toString(): String = description
    }

    private class ThrowingDataStore : DataStore<Map<String, CashbackSummaryDM>> {

        override val data: Flow<Map<String, CashbackSummaryDM>> = flow { throw IOException("disk error") }

        override suspend fun updateData(
            transform: suspend (t: Map<String, CashbackSummaryDM>) -> Map<String, CashbackSummaryDM>,
        ): Map<String, CashbackSummaryDM> {
            throw IOException("disk error")
        }
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
        val ENABLED = CashbackSummary.Enabled(
            displayMode = CashbackDisplayMode.FULL,
            cashback = TangemPayCashback(
                confirmedAmount = BigDecimal("2.70"),
                totalEarnedAmount = null,
                currency = "USD",
                payoutCurrency = "USDC",
                period = TangemPayCashback.Period(year = 2026, month = 8, payoutStart = null, payoutEnd = null),
                previousPayout = null,
            ),
        )
    }
}