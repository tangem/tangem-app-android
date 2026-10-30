package com.tangem.data.wallets

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.BindWalletsByReferralCodeBody
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class DefaultWalletsPromoRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val appsFlyerRepository: AppsFlyerRepository = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val userWallet: UserWallet = mockk {
        every { walletId } returns UserWalletId(WALLET_ID)
    }

    private lateinit var appPreferencesStore: AppPreferencesStore
    private lateinit var repository: DefaultWalletsPromoRepository

    @BeforeEach
    fun setUp() {
        clearMocks(tangemTechApi, userWalletsListRepository, appsFlyerRepository)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))

        appPreferencesStore = createAppPreferencesStore(
            dispatchers = dispatchers,
            preferencesDataStore = MockStateDataStore(default = emptyPreferences()),
        )
        repository = DefaultWalletsPromoRepository(
            appPreferencesStore = appPreferencesStore,
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            dispatchers = dispatchers,
            appsFlyerRepository = appsFlyerRepository,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RejectedCode {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN backend rejects code WHEN bindRefcodeWithWallets THEN saved binding data is cleared`(
            code: HttpException.Code,
        ) = runTest {
            // Arrange
            coEvery { tangemTechApi.bindWalletsByReferralCode(any()) } returns httpError(code)

            // Act
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = REFCODE, campaign = CAMPAIGN))

            // Assert
            assertThat(readSavedBindingData()).isNull()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN rejected code WHEN bindRefcodeWithWallets with new code THEN new code is sent`(
            code: HttpException.Code,
        ) = runTest {
            // Arrange
            coEvery { tangemTechApi.bindWalletsByReferralCode(any()) } returnsMany listOf(
                httpError(code),
                ApiResponse.Success(Unit),
            )
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = REFCODE, campaign = CAMPAIGN))

            // Act
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = NEW_REFCODE, campaign = null))

            // Assert
            coVerify(exactly = 1) {
                tangemTechApi.bindWalletsByReferralCode(body(refcode = NEW_REFCODE, campaign = null))
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN pending code rejected on retry WHEN bindRefcodeWithWallets with new code THEN new code is bound`(
            code: HttpException.Code,
        ) = runTest {
            // Arrange
            coEvery { tangemTechApi.bindWalletsByReferralCode(any()) } returnsMany listOf(
                httpError(HttpException.Code.SERVICE_UNAVAILABLE),
                httpError(code),
                ApiResponse.Success(Unit),
            )
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = REFCODE, campaign = CAMPAIGN))

            // Act
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = NEW_REFCODE, campaign = null))

            // Assert
            coVerify(exactly = 2) {
                tangemTechApi.bindWalletsByReferralCode(body(refcode = REFCODE, campaign = CAMPAIGN))
            }
            coVerify(exactly = 1) {
                tangemTechApi.bindWalletsByReferralCode(body(refcode = NEW_REFCODE, campaign = null))
            }
            assertThat(readSavedBindingData()).isEqualTo("""{"refcode":"$NEW_REFCODE","done":true}""")
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN pending code rejected on retry WHEN bindRefcodeWithWallets with same code THEN it is not resent`(
            code: HttpException.Code,
        ) = runTest {
            // Arrange
            coEvery { tangemTechApi.bindWalletsByReferralCode(any()) } returnsMany listOf(
                httpError(HttpException.Code.SERVICE_UNAVAILABLE),
                httpError(code),
            )
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = REFCODE, campaign = CAMPAIGN))

            // Act
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = REFCODE, campaign = CAMPAIGN))

            // Assert
            coVerify(exactly = 2) { tangemTechApi.bindWalletsByReferralCode(any()) }
            assertThat(readSavedBindingData()).isNull()
        }

        private fun provideTestModels() = listOf(
            HttpException.Code.BAD_REQUEST,
            HttpException.Code.NOT_FOUND,
            HttpException.Code.CONFLICT,
            HttpException.Code.UNPROCESSABLE_ENTITY,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RetryableError {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN retryable error WHEN retryBindRefcodeWithWallets THEN same code is sent again`(
            error: ApiResponseError,
        ) = runTest {
            // Arrange
            coEvery { tangemTechApi.bindWalletsByReferralCode(any()) } returns ApiResponse.Error(error).asUnit()
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = REFCODE, campaign = CAMPAIGN))

            // Act
            repository.retryBindRefcodeWithWallets()

            // Assert
            coVerify(exactly = 2) {
                tangemTechApi.bindWalletsByReferralCode(body(refcode = REFCODE, campaign = CAMPAIGN))
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN retryable error WHEN bindRefcodeWithWallets with new code THEN saved code is sent again`(
            error: ApiResponseError,
        ) = runTest {
            // Arrange
            coEvery { tangemTechApi.bindWalletsByReferralCode(any()) } returns ApiResponse.Error(error).asUnit()
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = REFCODE, campaign = CAMPAIGN))

            // Act
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = NEW_REFCODE, campaign = null))

            // Assert
            coVerify(exactly = 2) {
                tangemTechApi.bindWalletsByReferralCode(body(refcode = REFCODE, campaign = CAMPAIGN))
            }
            coVerify(exactly = 0) {
                tangemTechApi.bindWalletsByReferralCode(body(refcode = NEW_REFCODE, campaign = null))
            }
        }

        private fun provideTestModels(): List<ApiResponseError> = listOf(
            httpException(HttpException.Code.INTERNAL_SERVER_ERROR),
            httpException(HttpException.Code.SERVICE_UNAVAILABLE),
            httpException(HttpException.Code.UNAUTHORIZED),
            httpException(HttpException.Code.TOO_MANY_REQUESTS),
            httpException(HttpException.Code.REQUEST_TIMEOUT),
            ApiResponseError.NetworkException(),
            ApiResponseError.TimeoutException(),
        )
    }

    @Test
    fun `GIVEN successful binding WHEN bindRefcodeWithWallets with another code THEN api is not called again`() =
        runTest {
            // Arrange
            coEvery { tangemTechApi.bindWalletsByReferralCode(any()) } returns ApiResponse.Success(Unit)
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = REFCODE, campaign = CAMPAIGN))

            // Act
            repository.bindRefcodeWithWallets(AppsFlyerConversionData(refcode = NEW_REFCODE, campaign = null))

            // Assert
            coVerify(exactly = 1) { tangemTechApi.bindWalletsByReferralCode(any()) }
            coVerify(exactly = 1) {
                tangemTechApi.bindWalletsByReferralCode(body(refcode = REFCODE, campaign = CAMPAIGN))
            }
        }

    private suspend fun readSavedBindingData(): String? {
        return appPreferencesStore.data.first()[stringPreferencesKey(name = BINDING_DATA_KEY)]
    }

    private fun body(refcode: String, campaign: String?) = BindWalletsByReferralCodeBody(
        walletIds = listOf(WALLET_ID),
        refcode = refcode,
        campaign = campaign,
    )

    private fun httpException(code: HttpException.Code) = HttpException(code = code, message = null, errorBody = null)

    private fun httpError(code: HttpException.Code) = ApiResponse.Error(httpException(code)).asUnit()

    @Suppress("UNCHECKED_CAST")
    private fun ApiResponse.Error.asUnit() = this as ApiResponse<Unit>

    private companion object {
        const val WALLET_ID = "ABCDEF"
        const val REFCODE = "REFCODE"
        const val NEW_REFCODE = "NEWCODE"
        const val CAMPAIGN = "spring"
        const val BINDING_DATA_KEY = "referralWalletsBindingData"
    }
}