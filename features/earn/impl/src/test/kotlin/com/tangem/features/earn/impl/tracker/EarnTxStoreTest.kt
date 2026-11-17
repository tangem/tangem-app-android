package com.tangem.features.earn.impl.tracker

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.analytics.EarnTxAnalytics
import com.tangem.features.earn.product.api.session.EarnTxRef
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EarnTxStoreTest {

    private val keys = GrowPreferencesKeys()
    private val preferencesStore: AppPreferencesStore = createAppPreferencesStore(
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore<Preferences>(emptyPreferences()),
    )
    private val store = EarnTxStore(appPreferencesStore = preferencesStore, keys = keys)

    @BeforeEach
    fun setUp() {
        mockkObject(TangemLogger)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN nothing stored WHEN all THEN returns empty`() = runTest {
        // Act
        val actual = store.all()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN two refs added WHEN all THEN returns both in order`() = runTest {
        // Arrange
        val first = createRef(id = "0xaaa")
        val second = createRef(id = "0xbbb", productId = EarnProductId("staking"))
        store.add(first)
        store.add(second)

        // Act
        val actual = store.all()

        // Assert
        assertThat(actual).containsExactly(first, second).inOrder()
    }

    @Test
    fun `GIVEN a ref with the same id stored WHEN add THEN the record is replaced`() = runTest {
        // Arrange
        val stored = createRef(id = "0xaaa")
        val updated = stored.copy(createdAt = Instant.parse("2030-01-02T00:00:00Z"))
        store.add(stored)

        // Act
        store.add(updated)

        // Assert
        assertThat(store.all()).containsExactly(updated)
    }

    @Test
    fun `GIVEN two refs stored WHEN remove one THEN only the other stays`() = runTest {
        // Arrange
        val first = createRef(id = "0xaaa")
        val second = createRef(id = "0xbbb")
        store.add(first)
        store.add(second)

        // Act
        store.remove("0xaaa")

        // Assert
        assertThat(store.all()).containsExactly(second)
    }

    @Test
    fun `GIVEN nothing stored WHEN remove THEN all stays empty`() = runTest {
        // Act
        store.remove("0xaaa")

        // Assert
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun `GIVEN a broken stored string WHEN all THEN returns empty and logs an error`() = runTest {
        // Arrange
        preferencesStore.editData { it[keys.earnSentTransactionsKey] = "{not a list" }

        // Act
        val actual = store.all()

        // Assert
        assertThat(actual).isEmpty()
        verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a stored ref with a key this version does not know WHEN all THEN the ref still decodes`() = runTest {
        // Arrange
        val ref = createRef(id = "0xaaa")
        store.add(ref)
        preferencesStore.editData { preferences ->
            val stored = requireNotNull(preferences[keys.earnSentTransactionsKey])
            preferences[keys.earnSentTransactionsKey] = stored.replaceFirst("{", """{"addedLater":"value",""")
        }

        // Act
        val actual = store.all()

        // Assert
        assertThat(actual).containsExactly(ref)
        verify(exactly = 0) { TangemLogger.e(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a broken stored string WHEN add THEN the journal holds only the new ref`() = runTest {
        // Arrange
        preferencesStore.editData { it[keys.earnSentTransactionsKey] = "{not a list" }
        val ref = createRef(id = "0xaaa")

        // Act
        store.add(ref)

        // Assert
        assertThat(store.all()).containsExactly(ref)
    }

    private fun createRef(
        id: String,
        productId: EarnProductId = EarnProductId("lending"),
    ) = EarnTxRef(
        version = 1,
        productId = productId,
        id = id,
        userWalletId = UserWalletId("0123456789abcdef"),
        currencyId = CryptoCurrency.ID.fromValue("coin⟨ETH⟩ethereum"),
        createdAt = Instant.parse("2030-01-01T00:00:00Z"),
        analytics = EarnTxAnalytics(
            params = mapOf("Transaction Type" to "Deposit", "With Upgrade" to "false"),
            confirmedParams = mapOf("Amount Bucket" to "250-1K"),
        ),
    )
}