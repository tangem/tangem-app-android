package com.tangem.data.common.wallet

import com.google.common.truth.Truth
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.WalletIdBody
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val appsFlyerStore: AppsFlyerStore = mockk()
    private val tangemTechApi: TangemTechApi = mockk()
    private val dispatchers: CoroutineDispatcherProvider = TestingCoroutineDispatcherProvider()

    private val binder = DefaultWalletServerBinder(
        userWalletsListRepository = userWalletsListRepository,
        appsFlyerStore = appsFlyerStore,
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
        clearMocks(userWalletsListRepository, appsFlyerStore, tangemTechApi)
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
            coEvery { appsFlyerStore.get() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsListRepository.userWallets
                appsFlyerStore.get()
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
                appsFlyerStore.get()
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
            coEvery { appsFlyerStore.get() } returns null
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsListRepository.userWallets
                appsFlyerStore.get()
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
            coEvery { appsFlyerStore.get() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsListRepository.userWallets
                appsFlyerStore.get()
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

            coEvery { appsFlyerStore.get() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerStore.get()
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

            coEvery { appsFlyerStore.get() } returns null
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerStore.get()
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

            coEvery { appsFlyerStore.get() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerStore.get()
                tangemTechApi.createWallet(requestBody)
            }
        }
    }
}