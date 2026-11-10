package com.tangem.data.tangempay.store

import androidx.datastore.core.DataStore
import com.google.common.truth.Truth.assertThat
import com.tangem.data.tangempay.converter.PaymentAccountStatusValueDMConverter
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayCurrencyFactory
import com.tangem.spend.datasource.tangempay.entity.PaymentAccountStatusValueDM
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

    @Test
    fun `GIVEN persisted status still restoring WHEN getSyncOrNull THEN returns cached status`() = runTest {
        // Arrange
        val persistenceDataStore = MockStateDataStore<WalletIdWithPaymentStatusDM>(
            default = mapOf(WALLET_A.stringValue to PaymentAccountStatusValueDM.IssuingCard),
        )
        val store = createStore(DelayedReadDataStore(persistenceDataStore, readDelayMillis = RESTORE_DELAY_MS))

        // Act
        val actual = store.getSyncOrNull(WALLET_A)

        // Assert
        val expected = AccountStatus.Payment(
            account = Account.Payment(WALLET_A),
            value = PaymentAccountStatusValue.IssuingCard(source = StatusSource.CACHE),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN persisted status still restoring WHEN store THEN restored cache does not overwrite it`() = runTest {
        // Arrange
        val persistenceDataStore = MockStateDataStore<WalletIdWithPaymentStatusDM>(
            default = mapOf(WALLET_A.stringValue to PaymentAccountStatusValueDM.IssuingCard),
        )
        val store = createStore(DelayedReadDataStore(persistenceDataStore, readDelayMillis = RESTORE_DELAY_MS))

        // Act
        store.storeNotCreated(WALLET_A)
        advanceUntilIdle()

        // Assert
        val expected = AccountStatus.Payment(
            account = Account.Payment(WALLET_A),
            value = PaymentAccountStatusValue.NotCreated,
        )
        assertThat(store.getSyncOrNull(WALLET_A)).isEqualTo(expected)
        assertThat(persistenceDataStore.data.first())
            .containsExactly(WALLET_A.stringValue, PaymentAccountStatusValueDM.NotCreated)
    }

    @Test
    fun `GIVEN user wallets not loaded WHEN getSyncOrNull THEN returns cached status once wallets are loaded`() =
        runTest {
            // Arrange
            val userWallets = MutableStateFlow<List<UserWallet>?>(null)
            val currencyFactory = mockk<TangemPayCurrencyFactory> {
                every { create(any()) } answers {
                    checkNotNull(userWallets.value) { "User wallets are not loaded" }
                    CRYPTO_CURRENCY
                }
            }
            val persistenceDataStore = MockStateDataStore<WalletIdWithPaymentStatusDM>(
                default = mapOf(WALLET_A.stringValue to PaymentAccountStatusValueDM.IssuingCard),
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
            assertThat(actual.await()).isEqualTo(cachedIssuingCardStatus(WALLET_A))
            assertThat(persistenceDataStore.data.first())
                .containsExactly(WALLET_A.stringValue, PaymentAccountStatusValueDM.IssuingCard)
        }

    @Test
    fun `GIVEN one cached status fails to restore WHEN getSyncOrNull THEN other statuses restored and persistence kept`() =
        runTest {
            // Arrange
            val currencyFactory = mockk<TangemPayCurrencyFactory> {
                every { create(WALLET_A) } throws IllegalArgumentException("Unable to find user wallet")
                every { create(WALLET_B) } returns CRYPTO_CURRENCY
            }
            val persistedStatuses = mapOf(
                WALLET_A.stringValue to PaymentAccountStatusValueDM.IssuingCard,
                WALLET_B.stringValue to PaymentAccountStatusValueDM.IssuingCard,
            )
            val persistenceDataStore = MockStateDataStore<WalletIdWithPaymentStatusDM>(default = persistedStatuses)
            val store = createStore(persistenceDataStore, currencyFactory = currencyFactory)

            // Act
            val actualA = store.getSyncOrNull(WALLET_A)
            val actualB = store.getSyncOrNull(WALLET_B)

            // Assert
            assertThat(actualA).isNull()
            assertThat(actualB).isEqualTo(cachedIssuingCardStatus(WALLET_B))
            assertThat(persistenceDataStore.data.first()).isEqualTo(persistedStatuses)
        }

    private fun TestScope.createStore(
        persistenceDataStore: DataStore<WalletIdWithPaymentStatusDM>,
        userWallets: MutableStateFlow<List<UserWallet>?> = MutableStateFlow(emptyList()),
        currencyFactory: TangemPayCurrencyFactory = mockk(relaxed = true),
    ) = PaymentAccountStatusesStore(
        runtimeStore = RuntimeSharedStore<WalletIdWithPaymentStatus>(),
        persistenceDataStore = persistenceDataStore,
        converter = PaymentAccountStatusValueDMConverter(currencyFactory),
        userWalletsListRepository = mockk<UserWalletsListRepository> {
            every { this@mockk.userWallets } returns userWallets
        },
        scope = TestAppCoroutineScope(this),
    )

    private fun cachedIssuingCardStatus(userWalletId: UserWalletId) = AccountStatus.Payment(
        account = Account.Payment(userWalletId),
        value = PaymentAccountStatusValue.IssuingCard(source = StatusSource.CACHE),
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
        const val RESTORE_DELAY_MS = 1_000L
        val CRYPTO_CURRENCY = mockk<CryptoCurrency.Token>()
    }
}