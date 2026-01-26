package com.tangem.data.common.wallet

import com.google.common.truth.Truth
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.WalletIdBody
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.datasource.local.appsflyer.AppsFlyerConversionStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
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

    private val userWalletsStore: UserWalletsStore = mockk()
    private val appsFlyerConversionStore: AppsFlyerConversionStore = mockk()
    private val tangemTechApi: TangemTechApi = mockk()
    private val dispatchers: CoroutineDispatcherProvider = TestingCoroutineDispatcherProvider()

    private val binder = DefaultWalletServerBinder(
        userWalletsStore = userWalletsStore,
        appsFlyerConversionStore = appsFlyerConversionStore,
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
        clearMocks(userWalletsStore, appsFlyerConversionStore, tangemTechApi)
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

            coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns userWallet
            coEvery { appsFlyerConversionStore.get() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsStore.getSyncOrNull(userWalletId)
                appsFlyerConversionStore.get()
                tangemTechApi.createWallet(requestBody)
            }
        }

        @Test
        fun `bind will skipped if userWalletsStore returns null`() = runTest {
            coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns null

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(null)

            coVerifyOrder {
                userWalletsStore.getSyncOrNull(userWalletId)
            }

            coVerify(inverse = true) {
                appsFlyerConversionStore.get()
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

            coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns userWallet
            coEvery { appsFlyerConversionStore.get() } returns null
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsStore.getSyncOrNull(userWalletId)
                appsFlyerConversionStore.get()
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

            coEvery { userWalletsStore.getSyncOrNull(userWalletId) } returns userWallet
            coEvery { appsFlyerConversionStore.get() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWalletId = userWalletId)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                userWalletsStore.getSyncOrNull(userWalletId)
                appsFlyerConversionStore.get()
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

            coEvery { appsFlyerConversionStore.get() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerConversionStore.get()
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

            coEvery { appsFlyerConversionStore.get() } returns null
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerConversionStore.get()
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

            coEvery { appsFlyerConversionStore.get() } returns conversionData
            coEvery { tangemTechApi.createWallet(requestBody) } returns apiResponse

            val actual = binder.bind(userWallet = userWallet)

            Truth.assertThat(actual).isEqualTo(apiResponse)

            coVerifyOrder {
                appsFlyerConversionStore.get()
                tangemTechApi.createWallet(requestBody)
            }
        }
    }
}