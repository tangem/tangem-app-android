package com.tangem.data.wallets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError.HttpException
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.PromocodeActivationBody
import com.tangem.datasource.api.tangemTech.models.PromocodeActivationResponse
import com.tangem.datasource.api.tangemTech.models.WalletResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Tests for [DefaultWalletsRepository]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultWalletsRepositoryTest {

    private val preferencesDataStore: DataStore<Preferences> = mockk(relaxed = true)
    private val tangemTechApi: TangemTechApi = mockk()
    private val walletServerBinder: WalletServerBinder = mockk()

    private val appPreferenceStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = preferencesDataStore,
    )

    private val repository = DefaultWalletsRepository(
        appPreferencesStore = appPreferenceStore,
        tangemTechApi = tangemTechApi,
        userWalletsListRepository = mockk(),
        seedPhraseNotificationVisibilityStore = mockk(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        walletServerBinder = walletServerBinder,
        moshi = mockk(),
    )

    private val testWalletId = UserWalletId("1234567890abcdef")

    @AfterEach
    fun tearDown() {
        clearMocks(tangemTechApi, preferencesDataStore)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsNotificationsEnabled {

        @Test
        fun `should return local value when local storage has value`() = runTest {
            // Arrange
            val expectedPreferences = """{"${testWalletId.stringValue}":true}"""
            val preferences = mockk<Preferences>()
            coEvery { preferencesDataStore.data } returns flowOf(preferences)
            coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns expectedPreferences

            // Act
            val result = repository.isNotificationsEnabled(testWalletId)

            // Assert
            assertThat(result).isTrue()
        }

        @Test
        fun `should return false when local storage is empty`() = runTest {
            // Arrange
            val preferences = mockk<Preferences>()
            coEvery { preferencesDataStore.data } returns flowOf(preferences)
            coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns "{}"

            // Act
            val result = repository.isNotificationsEnabled(testWalletId)

            // Assert
            assertThat(result).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SetNotificationsEnabled {

        @Test
        fun `should update local storage when enabled status`() = runTest {
            // Arrange
            val preferences = mockk<Preferences>()
            coEvery { preferencesDataStore.data } returns flowOf(preferences)
            coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns "{}"
            coEvery { preferencesDataStore.updateData(any()) } returns mockk()

            // Act
            repository.setNotificationsEnabled(testWalletId, isEnabled = true)

            // Assert
            coVerify(exactly = 1) { preferencesDataStore.updateData(any()) }
        }

        @Test
        fun `should update local storage when disabled status`() = runTest {
            // Arrange
            val preferences = mockk<Preferences>()
            coEvery { preferencesDataStore.data } returns flowOf(preferences)
            coEvery { preferences[PreferencesKeys.NOTIFICATIONS_ENABLED_STATES_KEY] } returns "{}"
            coEvery { preferencesDataStore.updateData(any()) } returns mockk()

            // Act
            repository.setNotificationsEnabled(testWalletId, isEnabled = false)

            // Assert
            coVerify(exactly = 1) { preferencesDataStore.updateData(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetWalletsInfo {

        @Test
        fun `should return converted wallets and update cache when updateCache is true`() = runTest {
            // Arrange
            val applicationId = "test_app_id"
            val wallet1Id = "1234567890abcdef"
            val wallet2Id = "fedcba0987654321"
            val walletResponses = listOf(
                WalletResponse(id = wallet1Id, notifyStatus = true),
                WalletResponse(id = wallet2Id, notifyStatus = false),
            )
            coEvery { tangemTechApi.getWallets(applicationId) } returns ApiResponse.Success(walletResponses)
            coEvery { preferencesDataStore.updateData(any()) } returns mockk()

            // Act
            val result = repository.getWalletsInfo(applicationId, updateCache = true)

            // Assert
            assertThat(result).hasSize(2)
            assertThat(result[0].walletId.stringValue).isEqualTo(wallet1Id)
            assertThat(result[0].isNotificationsEnabled).isTrue()
            assertThat(result[1].walletId.stringValue).isEqualTo(wallet2Id)
            assertThat(result[1].isNotificationsEnabled).isFalse()

            coVerify(exactly = 1) { tangemTechApi.getWallets(applicationId) }
            coVerify(exactly = 2) { preferencesDataStore.updateData(any()) }
        }

        @Test
        fun `should return converted wallets without updating cache when updateCache is false`() = runTest {
            // Arrange
            val applicationId = "test_app_id"
            val wallet1Id = "1234567890abcdef"
            val walletResponses = listOf(
                WalletResponse(id = wallet1Id, notifyStatus = true),
            )
            coEvery { tangemTechApi.getWallets(applicationId) } returns ApiResponse.Success(walletResponses)

            // Act
            val result = repository.getWalletsInfo(applicationId, updateCache = false)

            // Assert
            assertThat(result).hasSize(1)
            assertThat(result[0].walletId.stringValue).isEqualTo(wallet1Id)
            assertThat(result[0].isNotificationsEnabled).isTrue()

            coVerify(exactly = 1) { tangemTechApi.getWallets(applicationId) }
            coVerify(exactly = 0) { preferencesDataStore.updateData(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AssociateWallets {

        @Test
        fun `should convert and send to associateApplicationIdWithWallets`() = runTest {
            // Arrange
            val applicationId = "test_app_id"
            val wallet1Id = "1234567890abcdef"
            val wallet2Id = "fedcba0987654321"

            val userWallets = listOf(
                mockk<UserWallet.Cold> {
                    every { walletId } returns UserWalletId(wallet1Id)
                },
                mockk<UserWallet.Cold> {
                    every { walletId } returns UserWalletId(wallet2Id)
                },
            )

            coEvery {
                tangemTechApi.associateApplicationIdWithWallets(eq(applicationId), any())
            } returns ApiResponse.Success(Unit)

            // Act
            repository.associateWallets(applicationId, userWallets)

            // Assert
            coVerify(exactly = 1) {
                tangemTechApi.associateApplicationIdWithWallets(
                    applicationId = eq(applicationId),
                    body = match { body ->
                        body.walletIds.size == 2 &&
                            body.walletIds.contains(wallet1Id) &&
                            body.walletIds.contains(wallet2Id)
                    },
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActivatePromoCode {

        @Test
        fun `should return Right with status when API returns success`() = runTest {
            // Arrange
            val walletId = UserWalletId("1234567890abcdef")
            val promoCode = "PROMO123"
            val address = "bc1qexampleaddress"
            coEvery { tangemTechApi.activatePromoCode(any()) } returns ApiResponse.Success(
                PromocodeActivationResponse(status = "activated"),
            )

            // Act
            val result = repository.activatePromoCode(
                userWalletId = walletId,
                promoCode = promoCode,
                bitcoinAddress = address,
            )

            // Assert
            var right: String? = null
            var left: ActivatePromoCodeError? = null
            result.fold({ left = it }, { right = it })
            assertThat(left).isNull()
            assertThat(right).isEqualTo("activated")

            coVerify(exactly = 1) {
                tangemTechApi.activatePromoCode(
                    match<PromocodeActivationBody> { it.promoCode == promoCode && it.address == address },
                )
            }
        }

        @Test
        fun `should return Left InvalidPromoCode when API returns NOT_FOUND`() = runTest {
            // Arrange
            val walletId = UserWalletId("1234567890abcdef")
            @Suppress("UNCHECKED_CAST")
            coEvery { tangemTechApi.activatePromoCode(any()) } returns ApiResponse.Error(
                HttpException(code = HttpException.Code.NOT_FOUND, message = null, errorBody = null),
            ) as ApiResponse<PromocodeActivationResponse>

            // Act
            val result = repository.activatePromoCode(
                userWalletId = walletId,
                promoCode = "PROMO",
                bitcoinAddress = "addr",
            )

            // Assert
            var error: ActivatePromoCodeError? = null
            result.fold({ error = it }, { })
            assertThat(error).isEqualTo(ActivatePromoCodeError.InvalidPromoCode)
        }

        @Test
        fun `should return Left PromocodeAlreadyUsed when API returns CONFLICT`() = runTest {
            // Arrange
            val walletId = UserWalletId("1234567890abcdef")
            @Suppress("UNCHECKED_CAST")
            coEvery { tangemTechApi.activatePromoCode(any()) } returns ApiResponse.Error(
                HttpException(code = HttpException.Code.CONFLICT, message = null, errorBody = null),
            ) as ApiResponse<PromocodeActivationResponse>

            // Act
            val result = repository.activatePromoCode(
                userWalletId = walletId,
                promoCode = "PROMO",
                bitcoinAddress = "addr",
            )

            // Assert
            var error: ActivatePromoCodeError? = null
            result.fold({ error = it }, { })
            assertThat(error).isEqualTo(ActivatePromoCodeError.PromocodeAlreadyUsed)
        }
    }
}