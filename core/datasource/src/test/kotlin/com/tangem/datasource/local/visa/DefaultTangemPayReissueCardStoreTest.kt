package com.tangem.datasource.local.visa

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [DefaultTangemPayReissueCardStore], focused on the reissue order-id lifecycle.
 *
 * Uses a real [AppPreferencesStore] backed by an in-memory [MockStateDataStore] so the preferences
 * round-trip (store / read / remove) is exercised end-to-end. The remove path backs the [REDACTED_TASK_KEY] fix:
 * a terminal reissue order must be forgotten so the payment-account refresh stops re-polling
 * `GET /order/{id}` for it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTangemPayReissueCardStoreTest {

    private val dataStore = MockStateDataStore(default = emptyPreferences())
    private val prefs = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = dataStore,
    )
    private val feeStore: RuntimeDataStore<TangemPayReissueCardFee> = mockk(relaxed = true)

    private val store = DefaultTangemPayReissueCardStore(feeStore = feeStore, prefs = prefs)

    @BeforeEach
    fun resetStore() {
        runBlocking { dataStore.updateData { emptyPreferences() } }
    }

    @Test
    fun `GIVEN order id stored WHEN getOrderId THEN returns stored id`() = runTest {
        // Arrange
        store.storeReissueOrderId(CARD_ID, ORDER_ID)

        // Act
        val result = store.getOrderId(CARD_ID)

        // Assert
        assertThat(result).isEqualTo(ORDER_ID)
    }

    @Test
    fun `GIVEN order id stored WHEN removeReissueOrderId THEN order id is cleared`() = runTest {
        // Arrange
        store.storeReissueOrderId(CARD_ID, ORDER_ID)

        // Act
        store.removeReissueOrderId(CARD_ID)

        // Assert
        assertThat(store.getOrderId(CARD_ID)).isNull()
    }

    @Test
    fun `GIVEN clearing one card WHEN another card has an order THEN the other is untouched`() = runTest {
        // Arrange
        store.storeReissueOrderId(CARD_ID, ORDER_ID)
        store.storeReissueOrderId(OTHER_CARD_ID, OTHER_ORDER_ID)

        // Act
        store.removeReissueOrderId(CARD_ID)

        // Assert
        assertThat(store.getOrderId(CARD_ID)).isNull()
        assertThat(store.getOrderId(OTHER_CARD_ID)).isEqualTo(OTHER_ORDER_ID)
    }

    private companion object {
        const val CARD_ID = "card-1"
        const val OTHER_CARD_ID = "card-2"
        const val ORDER_ID = "reissue-order-1"
        const val OTHER_ORDER_ID = "reissue-order-2"
    }
}