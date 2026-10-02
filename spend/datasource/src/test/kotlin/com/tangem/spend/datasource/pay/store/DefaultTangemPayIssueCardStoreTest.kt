package com.tangem.spend.datasource.pay.store

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultTangemPayIssueCardStoreTest {

    private val prefs = createAppPreferencesStore(
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
    )

    private val store = DefaultTangemPayIssueCardStore(prefs = prefs, json = NetworkJsonFactory.create())

    @Test
    fun `GIVEN empty store WHEN getIssueOrderIds THEN empty list`() = runTest {
        // Act
        val actual = store.getIssueOrderIds(WALLET_A)

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN two order ids added WHEN getIssueOrderIds THEN both kept in insertion order`() = runTest {
        // Arrange
        store.addIssueOrderId(WALLET_A, "order-1")
        store.addIssueOrderId(WALLET_A, "order-2")

        // Act
        val actual = store.getIssueOrderIds(WALLET_A)

        // Assert
        assertThat(actual).containsExactly("order-1", "order-2").inOrder()
    }

    @Test
    fun `GIVEN order id added twice WHEN getIssueOrderIds THEN it is stored once`() = runTest {
        // Arrange
        store.addIssueOrderId(WALLET_A, "order-1")
        store.addIssueOrderId(WALLET_A, "order-1")

        // Act
        val actual = store.getIssueOrderIds(WALLET_A)

        // Assert
        assertThat(actual).containsExactly("order-1")
    }

    @Test
    fun `GIVEN order id added WHEN removeIssueOrderId THEN it is gone and the other wallet is untouched`() = runTest {
        // Arrange
        store.addIssueOrderId(WALLET_A, "order-1")
        store.addIssueOrderId(WALLET_B, "order-1")

        // Act
        store.removeIssueOrderId(WALLET_A, "order-1")

        // Assert
        assertThat(store.getIssueOrderIds(WALLET_A)).isEmpty()
        assertThat(store.getIssueOrderIds(WALLET_B)).containsExactly("order-1")
    }

    @Test
    fun `GIVEN legacy json array WHEN getIssueOrderIds THEN it is read as is`() = runTest {
        // Arrange
        prefs.editData { preferences -> preferences[key(WALLET_A)] = """["legacy-1","legacy-2"]""" }

        // Act
        val actual = store.getIssueOrderIds(WALLET_A)

        // Assert
        assertThat(actual).containsExactly("legacy-1", "legacy-2").inOrder()
    }

    @Test
    fun `GIVEN malformed blob WHEN addIssueOrderId THEN the entry is written over it`() = runTest {
        // Arrange
        prefs.editData { preferences -> preferences[key(WALLET_A)] = "{" }

        // Act
        store.addIssueOrderId(WALLET_A, "order-1")

        // Assert
        assertThat(store.getIssueOrderIds(WALLET_A)).containsExactly("order-1")
        assertThat(prefs.data.first()[key(WALLET_A)]).isEqualTo("""["order-1"]""")
    }

    private fun key(userWalletId: UserWalletId) =
        stringPreferencesKey("tangem_pay_issue_card_orders_${userWalletId.stringValue}")

    private companion object {
        val WALLET_A = UserWalletId("0123456789abcdef")
        val WALLET_B = UserWalletId("fedcba9876543210")
    }
}