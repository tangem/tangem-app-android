package com.tangem.data.pushnotificationpreferences

import app.cash.turbine.test
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.PushNotificationPreferencesBody
import com.tangem.datasource.api.tangemTech.models.PushNotificationPreferencesResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationPreference
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DefaultWalletPushNotificationPreferencesRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()

    private val userWalletId = UserWalletId(stringValue = "0011223344556677")
    private val otherWalletId = UserWalletId(stringValue = "ffeeddccbbaa9988")

    private val repository = DefaultWalletPushNotificationPreferencesRepository(
        tangemTechApi = tangemTechApi,
        cache = RuntimeSharedStore(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `GIVEN server returns prefs WHEN preload THEN cache holds converted server state`() = runTest {
        // Arrange
        stubGet(userWalletId, transaction = true, offers = true, price = false)

        // Act
        repository.preload(userWalletId)

        // Assert
        repository.observePreferences(userWalletId).test {
            assertThat(awaitItem()).isEqualTo(prefs(transaction = true, offers = true, price = false))
        }
        coVerify(exactly = 1) { tangemTechApi.getPushNotificationPreferences(userWalletId.stringValue) }
    }

    @Test
    fun `GIVEN already preloaded WHEN preload called again THEN no second GET`() = runTest {
        // Arrange
        stubGet(userWalletId, transaction = true, offers = true, price = false)

        // Act
        repository.preload(userWalletId)
        repository.preload(userWalletId)

        // Assert
        coVerify(exactly = 1) { tangemTechApi.getPushNotificationPreferences(userWalletId.stringValue) }
    }

    @Test
    fun `GIVEN cache miss WHEN updatePreference THEN fetches baseline AND sends full-replace PUT AND caches echo`() =
        runTest {
            // Arrange
            stubGet(userWalletId, transaction = true, offers = true, price = false)
            stubPut(userWalletId, transaction = true, offers = true, price = true)

            // Act
            val result = repository.updatePreference(
                userWalletId = userWalletId,
                category = PushNotificationCategory.PriceAlerts,
                isEnabled = true,
            )

            // Assert
            assertThat(result).isInstanceOf(Either.Right::class.java)
            // The full-replace body changes only the tapped category on top of the server baseline.
            coVerify(exactly = 1) {
                tangemTechApi.updatePushNotificationPreferences(
                    userWalletId.stringValue,
                    PushNotificationPreferencesBody(
                        areTransactionEventsEnabled = true,
                        areOfferUpdatesEnabled = true,
                        arePriceAlertsEnabled = true,
                    ),
                )
            }
            repository.observePreferences(userWalletId).test {
                assertThat(awaitItem()).isEqualTo(prefs(transaction = true, offers = true, price = true))
            }
        }

    @Test
    fun `GIVEN preloaded state WHEN updatePreference THEN only the tapped category changes in the PUT body`() = runTest {
        // Arrange
        stubGet(userWalletId, transaction = true, offers = true, price = false)
        stubPut(userWalletId, transaction = true, offers = false, price = false)

        // Act
        repository.preload(userWalletId)
        repository.updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, isEnabled = false)

        // Assert
        coVerify(exactly = 1) {
            tangemTechApi.updatePushNotificationPreferences(
                userWalletId.stringValue,
                PushNotificationPreferencesBody(
                    areTransactionEventsEnabled = true,
                    areOfferUpdatesEnabled = false,
                    arePriceAlertsEnabled = false,
                ),
            )
        }
    }

    @Test
    fun `GIVEN write fails WHEN updatePreference THEN returns Left`() = runTest {
        // Arrange
        stubGet(userWalletId, transaction = true, offers = true, price = false)
        repository.preload(userWalletId)
        coEvery { tangemTechApi.updatePushNotificationPreferences(any(), any()) } throws IllegalStateException("boom")

        // Act
        val result = repository.updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, isEnabled = false)

        // Assert
        assertThat(result).isInstanceOf(Either.Left::class.java)
    }

    @Test
    fun `GIVEN different wallets WHEN observed THEN each keeps its own server state`() = runTest {
        // Arrange
        stubGet(userWalletId, transaction = false, offers = false, price = false)
        stubGet(otherWalletId, transaction = true, offers = true, price = true)

        // Assert
        repository.observePreferences(userWalletId).test {
            assertThat(awaitItem()).isEqualTo(prefs(transaction = false, offers = false, price = false))
        }
        repository.observePreferences(otherWalletId).test {
            assertThat(awaitItem()).isEqualTo(prefs(transaction = true, offers = true, price = true))
        }
    }

    @Test
    fun `GIVEN concurrent collectors WHEN preload races THEN a single GET is issued`() = runTest {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        coEvery { tangemTechApi.getPushNotificationPreferences(userWalletId.stringValue) } coAnswers {
            gate.await()
            ApiResponse.Success(PushNotificationPreferencesResponse(true, true, false))
        }

        // Act
        launch { repository.preload(userWalletId) }
        runCurrent()
        launch { repository.preload(userWalletId) }
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { tangemTechApi.getPushNotificationPreferences(userWalletId.stringValue) }
    }

    @Test
    fun `GIVEN concurrent writes WHEN updatePreference races THEN serialized so no update is lost`() = runTest {
        // Arrange
        stubGet(userWalletId, transaction = true, offers = true, price = false)
        val gate = CompletableDeferred<Unit>()
        coEvery { tangemTechApi.updatePushNotificationPreferences(eq(userWalletId.stringValue), any()) } coAnswers {
            val body = arg<PushNotificationPreferencesBody>(1)
            gate.await()
            ApiResponse.Success(
                PushNotificationPreferencesResponse(
                    body.areTransactionEventsEnabled,
                    body.areOfferUpdatesEnabled,
                    body.arePriceAlertsEnabled,
                ),
            )
        }
        repository.preload(userWalletId)

        // Act
        launch { repository.updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, isEnabled = false) }
        runCurrent()
        launch { repository.updatePreference(userWalletId, PushNotificationCategory.PriceAlerts, isEnabled = true) }
        runCurrent()
        gate.complete(Unit)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { tangemTechApi.updatePushNotificationPreferences(eq(userWalletId.stringValue), any()) }
        repository.observePreferences(userWalletId).test {
            assertThat(awaitItem()).isEqualTo(prefs(transaction = true, offers = false, price = true))
        }
    }

    private fun stubGet(id: UserWalletId, transaction: Boolean, offers: Boolean, price: Boolean) {
        coEvery { tangemTechApi.getPushNotificationPreferences(id.stringValue) } returns
            ApiResponse.Success(PushNotificationPreferencesResponse(transaction, offers, price))
    }

    private fun stubPut(id: UserWalletId, transaction: Boolean, offers: Boolean, price: Boolean) {
        coEvery { tangemTechApi.updatePushNotificationPreferences(eq(id.stringValue), any()) } returns
            ApiResponse.Success(PushNotificationPreferencesResponse(transaction, offers, price))
    }

    private fun prefs(transaction: Boolean, offers: Boolean, price: Boolean) = WalletPushNotificationPreferences(
        transactionAlerts = PushNotificationPreference(isEnabled = transaction),
        offersUpdates = PushNotificationPreference(isEnabled = offers),
        priceAlerts = PushNotificationPreference(isEnabled = price),
    )
}