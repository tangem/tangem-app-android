package com.tangem.data.common.wallet

import com.google.common.truth.Truth
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.WalletIdBody
import com.tangem.datasource.api.tangemTech.models.WalletResponse
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultWalletServerBinderTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val appsFlyerRepository: AppsFlyerRepository = mockk()
    private val tangemTechApi: TangemTechApi = mockk()
    private val dispatchers: CoroutineDispatcherProvider = TestingCoroutineDispatcherProvider()

    private val binder = DefaultWalletServerBinder(
        userWalletsListRepository = userWalletsListRepository,
        appsFlyerRepository = appsFlyerRepository,
        tangemTechApi = tangemTechApi,
        dispatchers = dispatchers
    )

    private val userWalletId = UserWalletId("011")
    private val userWallet = mockk<UserWallet.Cold> {
        every { this@mockk.walletId } returns userWalletId
        every { this@mockk.name } returns "Wallet"
    }
    private val conversionData = AppsFlyerConversionData(refcode = "refcode", campaign = "campaign")

    @AfterEach
    fun tearDown() {
        clearMocks(userWalletsListRepository, appsFlyerRepository, tangemTechApi)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BindUserWalletId {

        @Test
        fun `bind successfully`() = runTest {
            val requestBody = WalletIdBody(
                walletId = userWalletId.stringValue,
                name = "Wallet",
                walletType = WalletType.COLD,
                refcode = "refcode",
                campaign = "campaign",
            )
            val apiResponse = ApiResponse.Success(Unit)

            val userWalletsFlow = MutableStateFlow(listOf(userWallet))

            every { userWalletsListRepository.userWallets } returns userWalletsFlow
            coEvery { appsFlyerRepository.getConversionData() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsListRepository.userWallets
                appsFlyerRepository.getConversionData()
                tangemTechApi.createWallet(requestBody)
            }
        }

        @Test
        fun `bind will skipped if userWalletsListRepository returns null`() = runTest {
            val userWalletsFlow = MutableStateFlow<List<UserWallet>?>(null)

            every { userWalletsListRepository.userWallets } returns userWalletsFlow

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(null)

            coVerifyOrder { userWalletsListRepository.userWallets }

            coVerify(inverse = true) {
                appsFlyerRepository.getConversionData()
                tangemTechApi.createWallet(any())
            }
        }

        @Test
        fun `bind successfully if appsFlyerConversionStore returns null`() = runTest {
            val requestBody = WalletIdBody(
                walletId = userWalletId.stringValue,
                name = "Wallet",
                walletType = WalletType.COLD,
            )
            val apiResponse = ApiResponse.Success(Unit)

            val userWalletsFlow = MutableStateFlow(listOf(userWallet))

            every { userWalletsListRepository.userWallets } returns userWalletsFlow
            coEvery { appsFlyerRepository.getConversionData() } returns null
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsListRepository.userWallets
                appsFlyerRepository.getConversionData()
                tangemTechApi.createWallet(requestBody)
            }
        }

        @Test
        fun `bind returns error if request is failed`() = runTest {
            val requestBody = WalletIdBody(
                walletId = userWalletId.stringValue,
                name = "Wallet",
                walletType = WalletType.COLD,
                refcode = "refcode",
                campaign = "campaign",
            )
            val apiResponse = ApiResponse.Error(ApiResponseError.TimeoutException()) as ApiResponse<Unit>

            val userWalletsFlow = MutableStateFlow(listOf(userWallet))

            every { userWalletsListRepository.userWallets } returns userWalletsFlow
            coEvery { appsFlyerRepository.getConversionData() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsListRepository.userWallets
                appsFlyerRepository.getConversionData()
                tangemTechApi.createWallet(requestBody)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BindUserWallet {

        @Test
        fun `bind successfully`() = runTest {
            val requestBody = WalletIdBody(
                walletId = userWalletId.stringValue,
                name = "Wallet",
                walletType = WalletType.COLD,
                refcode = "refcode",
                campaign = "campaign",
            )
            val apiResponse = ApiResponse.Success(Unit)

            coEvery { appsFlyerRepository.getConversionData() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerRepository.getConversionData()
                tangemTechApi.createWallet(requestBody)
            }
        }

        @Test
        fun `bind successfully if appsFlyerConversionStore returns null`() = runTest {
            val requestBody = WalletIdBody(
                walletId = userWalletId.stringValue,
                name = "Wallet",
                walletType = WalletType.COLD,
            )
            val apiResponse = ApiResponse.Success(Unit)

            coEvery { appsFlyerRepository.getConversionData() } returns null
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerRepository.getConversionData()
                tangemTechApi.createWallet(requestBody)
            }
        }

        @Test
        fun `bind returns error if request is failed`() = runTest {
            val requestBody = WalletIdBody(
                walletId = userWalletId.stringValue,
                name = "Wallet",
                walletType = WalletType.COLD,
                refcode = "refcode",
                campaign = "campaign",
            )
            val apiResponse = ApiResponse.Error(ApiResponseError.TimeoutException()) as ApiResponse<Unit>

            coEvery { appsFlyerRepository.getConversionData() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerRepository.getConversionData()
                tangemTechApi.createWallet(requestBody)
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AwaitBound {

        // A fresh binder per test: it remembers the wallets the server confirmed
        private fun createBinder() = DefaultWalletServerBinder(
            userWalletsListRepository = userWalletsListRepository,
            appsFlyerRepository = appsFlyerRepository,
            tangemTechApi = tangemTechApi,
            dispatchers = dispatchers,
        )

        @Test
        fun `GIVEN server has the wallet WHEN awaitBound THEN returns without waiting for bind`() = runTest {
            // Arrange
            val binder = createBinder()
            coEvery { tangemTechApi.getWalletById(userWalletId.stringValue) } returns
                ApiResponse.Success(mockk<WalletResponse>())

            // Act
            binder.awaitBound(userWalletId)

            // Assert
            coVerify(exactly = 0) { tangemTechApi.createWallet(any()) }
        }

        @Test
        fun `GIVEN server lacks the wallet WHEN bind succeeds THEN awaitBound returns`() = runTest {
            // Arrange
            val binder = createBinder()
            stubServerLacksWallet()
            coEvery { tangemTechApi.createWallet(any()) } returns ApiResponse.Success(Unit)
            val awaiting = launch { binder.awaitBound(userWalletId) }
            runCurrent()

            // Act
            binder.bind(userWallet = userWallet)
            runCurrent()

            // Assert
            Truth.assertThat(awaiting.isCompleted).isTrue()
        }

        @Test
        fun `GIVEN server lacks the wallet WHEN bind fails THEN awaitBound keeps waiting`() = runTest {
            // Arrange
            val binder = createBinder()
            stubServerLacksWallet()
            coEvery { tangemTechApi.createWallet(any()) } returns
                ApiResponse.Error(ApiResponseError.TimeoutException()) as ApiResponse<Unit>
            val awaiting = launch { binder.awaitBound(userWalletId) }
            runCurrent()

            // Act
            binder.bind(userWallet = userWallet)
            runCurrent()

            // Assert
            Truth.assertThat(awaiting.isActive).isTrue()
            awaiting.cancel()
        }

        private fun stubServerLacksWallet() {
            coEvery { appsFlyerRepository.getConversionData() } returns null
            coEvery { tangemTechApi.getWalletById(userWalletId.stringValue) } returns ApiResponse.Error(
                ApiResponseError.HttpException(
                    code = ApiResponseError.HttpException.Code.NOT_FOUND,
                    message = null,
                    errorBody = null,
                ),
            ) as ApiResponse<WalletResponse>
        }
    }
}