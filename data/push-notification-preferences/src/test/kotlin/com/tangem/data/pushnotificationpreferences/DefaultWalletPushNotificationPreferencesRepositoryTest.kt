package com.tangem.data.pushnotificationpreferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationPreference
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultWalletPushNotificationPreferencesRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val preferencesDataStore: DataStore<Preferences> = mockk()
    private val appPreferencesStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = preferencesDataStore,
    )

    private val userWalletId = UserWalletId(stringValue = "0011223344556677")
    private val otherWalletId = UserWalletId(stringValue = "ffeeddccbbaa9988")

    private val repository = DefaultWalletPushNotificationPreferencesRepository(
        appPreferencesStore = appPreferencesStore,
        tangemTechApi = tangemTechApi,
        cache = RuntimeSharedStore(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `GIVEN no prior state WHEN preload THEN cache contains defaults`() = runTest {
        coEvery { preferencesDataStore.data } returns flowOf(emptyPreferences())

        repository.preload(userWalletId)

        repository.observePreferences(userWalletId).test {
            assertThat(awaitItem()).isEqualTo(defaults(transactionAlertsEnabled = true))
        }
    }

    @Test
    fun `GIVEN preload already done WHEN preload called again THEN no-op`() = runTest {
        coEvery { preferencesDataStore.data } returns flowOf(emptyPreferences())

        repository.preload(userWalletId)
        repository.updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, isEnabled = false)
        repository.preload(userWalletId)

        repository.observePreferences(userWalletId).test {
            val item = awaitItem()
            assertThat(item.offersUpdates.isEnabled).isFalse()
        }
    }

    @Test
    fun `GIVEN cache miss WHEN updatePreference THEN loads defaults and applies update`() = runTest {
        coEvery { preferencesDataStore.data } returns flowOf(emptyPreferences())

        val result = repository.updatePreference(
            userWalletId = userWalletId,
            category = PushNotificationCategory.PriceAlerts,
            isEnabled = true,
        )

        assertThat(result).isInstanceOf(Either.Right::class.java)
        repository.observePreferences(userWalletId).test {
            val item = awaitItem()
            assertThat(item.priceAlerts.isEnabled).isTrue()
            assertThat(item.offersUpdates.isEnabled).isTrue()
            assertThat(item.transactionAlerts.isEnabled).isTrue()
        }
    }

    @Test
    fun `GIVEN preloaded state WHEN updatePreference for each category THEN updates only that category`() = runTest {
        coEvery { preferencesDataStore.data } returns flowOf(emptyPreferences())

        repository.preload(userWalletId)
        repository.updatePreference(userWalletId, PushNotificationCategory.TransactionAlerts, isEnabled = false)
        repository.updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, isEnabled = false)
        repository.updatePreference(userWalletId, PushNotificationCategory.PriceAlerts, isEnabled = true)

        repository.observePreferences(userWalletId).test {
            val item = awaitItem()
            assertThat(item.transactionAlerts.isEnabled).isFalse()
            assertThat(item.offersUpdates.isEnabled).isFalse()
            assertThat(item.priceAlerts.isEnabled).isTrue()
        }
    }

    @Test
    fun `GIVEN no subscription yet WHEN observePreferences subscribed THEN triggers preload and emits defaults`() =
        runTest {
            coEvery { preferencesDataStore.data } returns flowOf(emptyPreferences())

            repository.observePreferences(userWalletId).test {
                val item = awaitItem()
                assertThat(item).isEqualTo(defaults(transactionAlertsEnabled = true))
            }
        }

    @Test
    fun `GIVEN updates for different wallets WHEN observed independently THEN each wallet has its own state`() =
        runTest {
            coEvery { preferencesDataStore.data } returns flowOf(emptyPreferences())

            repository.updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, isEnabled = false)
            repository.updatePreference(otherWalletId, PushNotificationCategory.PriceAlerts, isEnabled = true)

            repository.observePreferences(userWalletId).test {
                val item = awaitItem()
                assertThat(item.offersUpdates.isEnabled).isFalse()
                assertThat(item.priceAlerts.isEnabled).isFalse()
            }
            repository.observePreferences(otherWalletId).test {
                val item = awaitItem()
                assertThat(item.offersUpdates.isEnabled).isTrue()
                assertThat(item.priceAlerts.isEnabled).isTrue()
            }
        }

    private fun defaults(transactionAlertsEnabled: Boolean) = WalletPushNotificationPreferences(
        transactionAlerts = PushNotificationPreference(isEnabled = transactionAlertsEnabled, isVisible = true),
        offersUpdates = PushNotificationPreference(isEnabled = true, isVisible = true),
        priceAlerts = PushNotificationPreference(isEnabled = false, isVisible = true),
    )
}