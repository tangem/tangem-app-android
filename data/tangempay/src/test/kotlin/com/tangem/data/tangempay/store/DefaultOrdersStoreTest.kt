package com.tangem.data.tangempay.store

import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.order.OrderRef
import com.tangem.spend.datasource.preferences.SpendPreferencesKeys
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultOrdersStoreTest {

    private val spendPreferencesKeys = SpendPreferencesKeys()
    private val prefs = createAppPreferencesStore(
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )
    private val store = DefaultOrdersStore(
        prefs = prefs,
        spendPreferencesKeys = spendPreferencesKeys,
        json = NetworkJsonFactory.create(),
    )

    @Test
    fun `GIVEN empty store WHEN getAll THEN empty list`() = runTest {
        // Act
        val actual = store.getAll(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN two saved orders WHEN getAll THEN both returned`() = runTest {
        // Arrange
        store.save(FIRST_ORDER)
        store.save(SECOND_ORDER)

        // Act
        val actual = store.getAll(WALLET_A)

        // Assert
        assertThat(actual).containsExactly(FIRST_ORDER, SECOND_ORDER).inOrder()
    }

    @Test
    fun `GIVEN order saved twice WHEN getAll THEN stored once`() = runTest {
        // Arrange
        store.save(FIRST_ORDER)
        store.save(FIRST_ORDER)

        // Act
        val actual = store.getAll(WALLET_A)

        // Assert
        assertThat(actual).containsExactly(FIRST_ORDER)
    }

    @Test
    fun `GIVEN saved orders WHEN remove one THEN only the other stays`() = runTest {
        // Arrange
        store.save(FIRST_ORDER)
        store.save(SECOND_ORDER)

        // Act
        store.remove(FIRST_ORDER)

        // Assert
        assertThat(store.getAll(WALLET_A)).containsExactly(SECOND_ORDER)
    }

    @Test
    fun `GIVEN orders of two wallets WHEN clear one wallet THEN the other wallet keeps its orders`() = runTest {
        // Arrange
        val otherWalletOrder = FIRST_ORDER.copy(userWalletId = WALLET_B)
        store.save(FIRST_ORDER)
        store.save(otherWalletOrder)

        // Act
        store.clear(WALLET_A)

        // Assert
        assertThat(store.getAll(WALLET_A)).isEmpty()
        assertThat(store.getAll(WALLET_B)).containsExactly(otherWalletOrder)
    }

    @Test
    fun `GIVEN a malformed blob WHEN save THEN the order is written and getAll returns it`() = runTest {
        // Arrange
        prefs.editData { preferences -> preferences[spendPreferencesKeys.orderRefs(WALLET_A)] = """{"x":1}""" }

        // Act
        store.save(FIRST_ORDER)

        // Assert
        assertThat(store.getAll(WALLET_A)).containsExactly(FIRST_ORDER)
    }

    private companion object {
        val WALLET_A = UserWalletId("0123456789abcdef")
        val WALLET_B = UserWalletId("fedcba9876543210")
        val FIRST_ORDER = OrderRef(userWalletId = WALLET_A, id = "order-1")
        val SECOND_ORDER = OrderRef(userWalletId = WALLET_A, id = "order-2")
    }
}