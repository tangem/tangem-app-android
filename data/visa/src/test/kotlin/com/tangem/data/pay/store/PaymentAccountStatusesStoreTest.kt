package com.tangem.data.pay.store

import com.google.common.truth.Truth.assertThat
import com.tangem.data.pay.converter.PaymentAccountStatusValueDMConverter
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PaymentAccountStatusesStoreTest {

    @Test
    fun `GIVEN stored status WHEN remove single THEN cleared from both runtime and persistence`() = runTest {
        // Arrange
        val persistenceDataStore = MockStateDataStore<WalletIdWithPaymentStatusDM>(default = emptyMap())
        val store = createStore(persistenceDataStore)
        advanceUntilIdle()
        store.storeNotCreated(WALLET_A)
        advanceUntilIdle()
        // sanity: present in both stores before removal
        assertThat(store.getSyncOrNull(WALLET_A)).isNotNull()
        assertThat(persistenceDataStore.data.first()).containsKey(WALLET_A.stringValue)

        // Act
        store.remove(WALLET_A)
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(persistenceDataStore.data.first()).doesNotContainKey(WALLET_A.stringValue)
    }

    @Test
    fun `GIVEN statuses for two wallets WHEN remove list THEN both cleared from both stores`() = runTest {
        // Arrange
        val persistenceDataStore = MockStateDataStore<WalletIdWithPaymentStatusDM>(default = emptyMap())
        val store = createStore(persistenceDataStore)
        advanceUntilIdle()
        store.storeNotCreated(WALLET_A)
        store.storeNotCreated(WALLET_B)
        advanceUntilIdle()

        // Act
        store.remove(listOf(WALLET_A, WALLET_B))
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B)).isNull()
        assertThat(persistenceDataStore.data.first()).doesNotContainKey(WALLET_A.stringValue)
        assertThat(persistenceDataStore.data.first()).doesNotContainKey(WALLET_B.stringValue)
    }

    private fun TestScope.createStore(
        persistenceDataStore: MockStateDataStore<WalletIdWithPaymentStatusDM>,
    ) = PaymentAccountStatusesStore(
        runtimeStore = RuntimeSharedStore<WalletIdWithPaymentStatus>(),
        persistenceDataStore = persistenceDataStore,
        converter = PaymentAccountStatusValueDMConverter(mockk<TangemPayCurrencyFactory>(relaxed = true)),
        scope = TestAppCoroutineScope(this),
    )

    private suspend fun PaymentAccountStatusesStore.storeNotCreated(userWalletId: UserWalletId) {
        store(
            userWalletId = userWalletId,
            status = AccountStatus.Payment(
                account = Account.Payment(userWalletId),
                value = PaymentAccountStatusValue.NotCreated,
            ),
        )
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}