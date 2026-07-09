package com.tangem.datasource.local.visa

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.visa.entity.TangemPayTxHistoryItemToDMConverter
import com.tangem.datasource.local.visa.entity.TangemPayTxHistoryItemToDomainConverter
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.test.core.datastore.MockStateDataStore
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.Currency

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultTangemPayTxHistoryItemsStoreTest {

    private lateinit var store: DefaultTangemPayTxHistoryItemsStore

    @BeforeEach
    fun setup() {
        store = DefaultTangemPayTxHistoryItemsStore(
            dataStore = MockStateDataStore(default = emptyMap()),
            toDMConverter = TangemPayTxHistoryItemToDMConverter(),
            toDomainConverter = TangemPayTxHistoryItemToDomainConverter(),
        )
    }

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        val result = store.getSyncOrNull(key = WALLET_A, cursor = CURSOR_1)

        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN stored page WHEN getSyncOrNull with same key and cursor THEN returns page`() = runTest {
        // Arrange
        val page = listOf(payment("1"), payment("2"))
        store.store(key = WALLET_A, cursor = CURSOR_1, value = page)

        // Act
        val result = store.getSyncOrNull(key = WALLET_A, cursor = CURSOR_1)

        // Assert
        assertThat(result).isEqualTo(page)
    }

    @Test
    fun `GIVEN page stored under one cursor WHEN getSyncOrNull with another cursor THEN returns null`() = runTest {
        // Arrange
        store.store(key = WALLET_A, cursor = CURSOR_1, value = listOf(payment("1")))

        // Act
        val result = store.getSyncOrNull(key = WALLET_A, cursor = CURSOR_2)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN two cursors stored for one wallet WHEN getSyncOrNull THEN both pages are kept`() = runTest {
        // Arrange
        val page1 = listOf(payment("1"))
        val page2 = listOf(payment("2"))
        store.store(key = WALLET_A, cursor = CURSOR_1, value = page1)
        store.store(key = WALLET_A, cursor = CURSOR_2, value = page2)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A, CURSOR_1)).isEqualTo(page1)
        assertThat(store.getSyncOrNull(WALLET_A, CURSOR_2)).isEqualTo(page2)
    }

    @Test
    fun `GIVEN entries for two wallets WHEN remove one wallet THEN only that wallet is cleared`() = runTest {
        // Arrange
        store.store(key = WALLET_A, cursor = CURSOR_1, value = listOf(payment("a")))
        store.store(key = WALLET_B, cursor = CURSOR_1, value = listOf(payment("b")))

        // Act
        store.remove(WALLET_A)

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A, CURSOR_1)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B, CURSOR_1)).isEqualTo(listOf(payment("b")))
    }

    @Test
    fun `GIVEN entries for two wallets WHEN remove both in one call THEN both are cleared`() = runTest {
        // Arrange
        store.store(key = WALLET_A, cursor = CURSOR_1, value = listOf(payment("a")))
        store.store(key = WALLET_B, cursor = CURSOR_1, value = listOf(payment("b")))

        // Act
        store.remove(listOf(WALLET_A, WALLET_B))

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A, CURSOR_1)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B, CURSOR_1)).isNull()
    }

    private fun payment(id: String) = TangemPayTxHistoryItem.Payment(
        id = id,
        jsonRepresentation = "{}",
        date = DateTime(DATE_MILLIS),
        amount = BigDecimal("1.00"),
        currency = Currency.getInstance("USD"),
        transactionHash = null,
    )

    private companion object {
        const val WALLET_A = "wallet-a"
        const val WALLET_B = "wallet-b"
        const val CURSOR_1 = "cursor-1"
        const val CURSOR_2 = "cursor-2"
        const val DATE_MILLIS = 1_700_000_000_000L
    }
}