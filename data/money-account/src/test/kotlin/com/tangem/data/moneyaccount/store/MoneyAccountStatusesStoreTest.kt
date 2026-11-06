package com.tangem.data.moneyaccount.store

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.moneyaccount.converter.MoneyAccountStatusValueDMConverter
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
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
internal class MoneyAccountStatusesStoreTest {

    @Test
    fun `GIVEN statuses for two wallets WHEN clear one THEN only it is cleared from both stores`() = runTest {
        // Arrange
        val persistenceDataStore = MockStateDataStore<WalletIdWithMoneyAccountStatusDM>(default = emptyMap())
        val store = createStore(persistenceDataStore)
        advanceUntilIdle()
        store.storeNotCreated(WALLET_A)
        store.storeNotCreated(WALLET_B)
        advanceUntilIdle()

        // Act
        store.clear(listOf(WALLET_A))
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B)).isNotNull()
        assertThat(persistenceDataStore.data.first().keys).containsExactly(WALLET_B.stringValue)
    }

    @Test
    fun `GIVEN statuses for two wallets WHEN clear both THEN both cleared from both stores`() = runTest {
        // Arrange
        val persistenceDataStore = MockStateDataStore<WalletIdWithMoneyAccountStatusDM>(default = emptyMap())
        val store = createStore(persistenceDataStore)
        advanceUntilIdle()
        store.storeNotCreated(WALLET_A)
        store.storeNotCreated(WALLET_B)
        advanceUntilIdle()

        // Act
        store.clear(listOf(WALLET_A, WALLET_B))
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B)).isNull()
        assertThat(persistenceDataStore.data.first()).isEmpty()
    }

    private fun TestScope.createStore(
        persistenceDataStore: MockStateDataStore<WalletIdWithMoneyAccountStatusDM>,
    ) = MoneyAccountStatusesStore(
        runtimeStore = RuntimeSharedStore<WalletIdWithMoneyAccountStatus>(),
        persistenceDataStore = persistenceDataStore,
        converter = MoneyAccountStatusValueDMConverter(mockk<MoneyAccountCurrencyFactory>(relaxed = true)),
        scope = TestAppCoroutineScope(this),
    )

    private suspend fun MoneyAccountStatusesStore.storeNotCreated(userWalletId: UserWalletId) {
        store(
            userWalletId = userWalletId,
            status = AccountStatus.Money(
                account = Account.Money(userWalletId),
                value = MoneyAccountStatusValue.NotCreated,
            ),
        )
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}