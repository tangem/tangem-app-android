package com.tangem.data.wallets

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.PromocodeActivationBody
import com.tangem.datasource.api.tangemTech.models.PromocodeActivationResponse
import com.tangem.datasource.api.tangemTech.models.WalletResponse
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.PromoCodeActivation
import com.tangem.domain.wallets.models.PromoPayout
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import arrow.core.left
import arrow.core.right
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Tests for [DefaultWalletsRepository]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultWalletsRepositoryTest {

    private val preferencesDataStore: DataStore<Preferences> = mockk(relaxed = true)
    private val tangemTechApi: TangemTechApi = mockk()
    private val walletServerBinder: WalletServerBinder = mockk()

    private val appPreferenceStore = createAppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        preferencesDataStore = preferencesDataStore,
    )

    private val storePreferencesKeys = StorePreferencesKeys()

    private val repository = DefaultWalletsRepository(
        appPreferencesStore = appPreferenceStore,
        storePreferencesKeys = storePreferencesKeys,
        tangemTechApi = tangemTechApi,
        userWalletsListRepository = mockk(),
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
            coEvery { preferences[storePreferencesKeys.notificationsEnabledStatesKey] } returns expectedPreferences

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
            coEvery { preferences[storePreferencesKeys.notificationsEnabledStatesKey] } returns "{}"

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
            coEvery { preferences[storePreferencesKeys.notificationsEnabledStatesKey] } returns "{}"
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
            coEvery { preferences[storePreferencesKeys.notificationsEnabledStatesKey] } returns "{}"
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
        fun `GIVEN stellar payout WHEN activatePromoCode THEN sends payout and returns activation`() = runTest {
            // Arrange
            val payout = PromoPayout(networkId = "stellar", currencySymbol = "XLM")
            coEvery { tangemTechApi.activatePromoCode(any()) } returns ApiResponse.Success(
                PromocodeActivationResponse(
                    status = "activated",
                    promoProgram = PromocodeActivationResponse.PromoProgram(id = "program-id"),
                ),
            )

            // Act
            val actual = repository.activatePromoCode(
                userWalletId = testWalletId,
                promoCode = "PROMO",
                address = "GADDRESS",
                payout = payout,
            )

            // Assert
            assertThat(actual).isEqualTo(PromoCodeActivation(programId = "program-id").right())
            coVerify(exactly = 1) {
                tangemTechApi.activatePromoCode(
                    PromocodeActivationBody(
                        promoCode = "PROMO",
                        address = "GADDRESS",
                        walletId = testWalletId.stringValue,
                        paymentNetwork = "stellar",
                        paymentCurrency = "XLM",
                    ),
                )
            }
        }

        @Test
        fun `GIVEN no payout and bare response WHEN activatePromoCode THEN omits payout and returns empty activation`() =
            runTest {
                // Arrange
                coEvery { tangemTechApi.activatePromoCode(any()) } returns ApiResponse.Success(
                    PromocodeActivationResponse(status = "activated"),
                )

                // Act
                val actual = repository.activatePromoCode(
                    userWalletId = testWalletId,
                    promoCode = "PROMO",
                    address = "bc1qaddress",
                    payout = null,
                )

                // Assert
                assertThat(actual).isEqualTo(PromoCodeActivation(programId = null).right())
                coVerify(exactly = 1) {
                    tangemTechApi.activatePromoCode(
                        PromocodeActivationBody(
                            promoCode = "PROMO",
                            address = "bc1qaddress",
                            walletId = testWalletId.stringValue,
                        ),
                    )
                }
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN api error WHEN activatePromoCode THEN maps to domain error`(model: ErrorModel) = runTest {
            // Arrange
            @Suppress("UNCHECKED_CAST")
            coEvery { tangemTechApi.activatePromoCode(any()) } returns
                ApiResponse.Error(model.apiError) as ApiResponse<PromocodeActivationResponse>

            // Act
            val actual = repository.activatePromoCode(
                userWalletId = testWalletId,
                promoCode = "PROMO",
                address = "addr",
                payout = null,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected.left())
        }

        private fun provideTestModels() = listOf(
            ErrorModel(httpError(HttpException.Code.NOT_FOUND), ActivatePromoCodeError.InvalidPromoCode),
            ErrorModel(httpError(HttpException.Code.CONFLICT), ActivatePromoCodeError.PromocodeAlreadyUsed),
            ErrorModel(httpError(HttpException.Code.BAD_REQUEST), ActivatePromoCodeError.InvalidRequest),
            ErrorModel(httpError(HttpException.Code.UNPROCESSABLE_ENTITY), ActivatePromoCodeError.CampaignExpired),
            ErrorModel(httpError(HttpException.Code.INTERNAL_SERVER_ERROR), ActivatePromoCodeError.ActivationFailed),
            ErrorModel(ApiResponseError.NetworkException(), ActivatePromoCodeError.NetworkError),
            ErrorModel(ApiResponseError.TimeoutException(), ActivatePromoCodeError.NetworkError),
            ErrorModel(ApiResponseError.UnknownException(IllegalStateException()), ActivatePromoCodeError.ActivationFailed),
        )

        private fun httpError(code: HttpException.Code) = HttpException(code = code, message = null, errorBody = null)
    }

    data class ErrorModel(val apiError: ApiResponseError, val expected: ActivatePromoCodeError)
}