package com.tangem.data.moneyaccount.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.moneyaccount.converter.MoneyAccountStatusValueDMConverter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.spend.datasource.visa.entity.MoneyAccountStatusValueDM
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.DelayedReadDataStore
import com.tangem.test.core.datastore.MockStateDataStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
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
        store.storeEmpty(WALLET_A)
        store.storeEmpty(WALLET_B)
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
        store.storeEmpty(WALLET_A)
        store.storeEmpty(WALLET_B)
        advanceUntilIdle()

        // Act
        store.clear(listOf(WALLET_A, WALLET_B))
        advanceUntilIdle()

        // Assert
        assertThat(store.getSyncOrNull(WALLET_A)).isNull()
        assertThat(store.getSyncOrNull(WALLET_B)).isNull()
        assertThat(persistenceDataStore.data.first()).isEmpty()
    }

    @Test
    fun `GIVEN persisted status still restoring WHEN getSyncOrNull THEN returns cached status`() = runTest {
        // Arrange
        val persistenceDataStore = MockStateDataStore<WalletIdWithMoneyAccountStatusDM>(
            default = mapOf(WALLET_A.stringValue to MoneyAccountStatusValueDM.Failed(customerId = CUSTOMER_ID)),
        )
        val store = createStore(DelayedReadDataStore(persistenceDataStore, readDelayMillis = RESTORE_DELAY_MS))

        // Act
        val actual = store.getSyncOrNull(WALLET_A)

        // Assert
        val expected = AccountStatus.Money(
            account = Account.Money(WALLET_A),
            value = MoneyAccountStatusValue.Onboarding.Failed(source = StatusSource.CACHE, customerId = CUSTOMER_ID),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN persisted status still restoring WHEN store THEN restored cache does not overwrite it`() = runTest {
        // Arrange
        val persistenceDataStore = MockStateDataStore<WalletIdWithMoneyAccountStatusDM>(
            default = mapOf(WALLET_A.stringValue to MoneyAccountStatusValueDM.Failed(customerId = CUSTOMER_ID)),
        )
        val store = createStore(DelayedReadDataStore(persistenceDataStore, readDelayMillis = RESTORE_DELAY_MS))

        // Act
        store.storeEmpty(WALLET_A)
        advanceUntilIdle()

        // Assert
        val expected = AccountStatus.Money(account = Account.Money(WALLET_A), value = MoneyAccountStatusValue.Empty)
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(expected)
        assertThat(persistenceDataStore.data.first())
            .containsExactly(WALLET_A.stringValue, MoneyAccountStatusValueDM.Empty)
    }

    @Test
    fun `GIVEN user wallets not loaded WHEN getSyncOrNull THEN returns cached status once wallets are loaded`() =
        runTest {
            // Arrange
            val userWallets = MutableStateFlow<List<UserWallet>?>(null)
            val currencyFactory = mockk<MoneyAccountCurrencyFactory> {
                every { create(any()) } answers {
                    checkNotNull(userWallets.value) { "User wallets are not loaded" }
                    CRYPTO_CURRENCY
                }
            }
            val persistenceDataStore = MockStateDataStore<WalletIdWithMoneyAccountStatusDM>(
                default = mapOf(WALLET_A.stringValue to activeAccountDM()),
            )
            val store = createStore(persistenceDataStore, userWallets, currencyFactory)

            // Act
            val actual = async { store.getSyncOrNull(WALLET_A) }
            advanceUntilIdle()
            val isCompletedBeforeWalletsLoaded = actual.isCompleted
            userWallets.value = emptyList()
            advanceUntilIdle()

            // Assert
            assertThat(isCompletedBeforeWalletsLoaded).isFalse()
            assertThat(actual.await()).isEqualTo(cachedActiveStatus(WALLET_A))
            assertThat(persistenceDataStore.data.first())
                .containsExactly(WALLET_A.stringValue, activeAccountDM())
        }

    @Test
    fun `GIVEN one cached status fails to restore WHEN getSyncOrNull THEN other statuses restored and persistence kept`() =
        runTest {
            // Arrange
            val currencyFactory = mockk<MoneyAccountCurrencyFactory> {
                every { create(WALLET_A) } throws IllegalArgumentException("Unable to find user wallet")
                every { create(WALLET_B) } returns CRYPTO_CURRENCY
            }
            val persistedStatuses = mapOf(
                WALLET_A.stringValue to activeAccountDM(),
                WALLET_B.stringValue to activeAccountDM(),
            )
            val persistenceDataStore = MockStateDataStore<WalletIdWithMoneyAccountStatusDM>(default = persistedStatuses)
            val store = createStore(persistenceDataStore, currencyFactory = currencyFactory)

            // Act
            val actualA = store.getSyncOrNull(WALLET_A)
            val actualB = store.getSyncOrNull(WALLET_B)

            // Assert
            assertThat(actualA).isNull()
            assertThat(actualB).isEqualTo(cachedActiveStatus(WALLET_B))
            assertThat(persistenceDataStore.data.first()).isEqualTo(persistedStatuses)
        }

    private fun TestScope.createStore(
        persistenceDataStore: DataStore<WalletIdWithMoneyAccountStatusDM>,
        userWallets: MutableStateFlow<List<UserWallet>?> = MutableStateFlow(emptyList()),
        currencyFactory: MoneyAccountCurrencyFactory = mockk(relaxed = true),
    ) = MoneyAccountStatusesStore(
        runtimeStore = RuntimeSharedStore<WalletIdWithMoneyAccountStatus>(),
        persistenceDataStore = persistenceDataStore,
        converter = MoneyAccountStatusValueDMConverter(currencyFactory),
        userWalletsListRepository = mockk<UserWalletsListRepository> {
            every { this@mockk.userWallets } returns userWallets
        },
        scope = TestAppCoroutineScope(this),
    )

    private fun activeAccountDM() = MoneyAccountStatusValueDM.ActiveAccount(
        customerId = CUSTOMER_ID,
        productInstanceId = PRODUCT_INSTANCE_ID,
        balance = null,
    )

    private fun cachedActiveStatus(userWalletId: UserWalletId) = AccountStatus.Money(
        account = Account.Money(userWalletId),
        value = MoneyAccountStatusValue.Account.Active(
            source = StatusSource.CACHE,
            cryptoCurrency = CRYPTO_CURRENCY,
            balance = null,
            customerId = CUSTOMER_ID,
            productInstanceId = PRODUCT_INSTANCE_ID,
            error = null,
        ),
    )

    private suspend fun MoneyAccountStatusesStore.storeEmpty(userWalletId: UserWalletId) {
        store(
            userWalletId = userWalletId,
            status = AccountStatus.Money(
                account = Account.Money(userWalletId),
                value = MoneyAccountStatusValue.Empty,
            ),
        )
    }

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
        const val CUSTOMER_ID = "customer_id"
        const val PRODUCT_INSTANCE_ID = "product_instance_id"
        val CRYPTO_CURRENCY = mockk<CryptoCurrency.Token>()
        const val RESTORE_DELAY_MS = 1_000L
    }
}