package com.tangem.features.send.sendnft.model

import arrow.core.left
import arrow.core.right
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.nft.entity.NFTSendSuccessTrigger
import com.tangem.features.send.api.NFTSendComponent
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * Guards the NFT-send navigation refactor: [NFTSendModel.onBackClick] / [NFTSendModel.onNextClick] now
 * receive the active [com.tangem.core.decompose.navigation.Route] as a parameter (instead of reading an
 * internal `currentRouteFlow`). Routing is synchronous, so those assertions run without advancing; the
 * one async effect — firing `NFTSendSuccessTrigger.triggerSuccessNFTSend()` when leaving the success
 * screen — is verified after `advanceUntilIdle()`.
 *
 * `init {}` collectors are kept harmless under `advanceUntilIdle()` by stubbing the wallet lookup to the
 * not-found branch (so no further currency/account fetching is reached) and the app-currency lookup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class NFTSendModelNavigationTest {

    private val router: Router = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    private val nftSendSuccessTrigger: NFTSendSuccessTrigger = mockk(relaxed = true)

    private var model: NFTSendModel? = null

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN destination step WHEN onNextClick THEN pushes confirm`() = runTest {
        val model = createModel(this)

        model.onNextClick(CommonSendRoute.Destination(isEditMode = false))

        verify(exactly = 1) { router.push(CommonSendRoute.Confirm) }
    }

    @Test
    fun `GIVEN edit-mode destination WHEN onNextClick THEN pops instead of advancing`() = runTest {
        val model = createModel(this)

        model.onNextClick(CommonSendRoute.Destination(isEditMode = true))

        verify(exactly = 1) { router.pop() }
        verify(exactly = 0) { router.push(any()) }
    }

    @Test
    fun `GIVEN destination step WHEN onBackClick THEN pops without firing success trigger`() = runTest {
        val model = createModel(this)

        model.onBackClick(CommonSendRoute.Destination(isEditMode = false))
        advanceUntilIdle()

        verify(exactly = 1) { router.pop() }
        coVerify(exactly = 0) { nftSendSuccessTrigger.triggerSuccessNFTSend() }
    }

    @Test
    fun `GIVEN confirm-success route WHEN onBackClick THEN fires success trigger and pops`() = runTest {
        val model = createModel(this)

        model.onBackClick(CommonSendRoute.ConfirmSuccess)
        advanceUntilIdle()

        coVerify(exactly = 1) { nftSendSuccessTrigger.triggerSuccessNFTSend() }
        verify(exactly = 1) { router.pop() }
    }

    private fun createModel(testScope: TestScope): NFTSendModel {
        every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()

        val params = NFTSendComponent.Params(
            userWalletId = UserWalletId(stringValue = "0123456789"),
            nftAsset = mockk<NFTAsset>(relaxed = true),
            nftCollectionName = "Test Collection",
        )
        return NFTSendModel(
            paramsContainer = MutableParamsContainer(value = params),
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            multiWalletCryptoCurrenciesSupplier = mockk(relaxed = true),
            getFeePaidCryptoCurrencyStatusSyncUseCase = mockk(relaxed = true),
            createNFTTransferTransactionUseCase = mockk(relaxed = true),
            getFeeUseCase = mockk(relaxed = true),
            saveBlockchainErrorUseCase = mockk(relaxed = true),
            getWalletMetaInfoUseCase = mockk(relaxed = true),
            sendFeedbackEmailUseCase = mockk(relaxed = true),
            alertFactory = mockk(relaxed = true),
            nftSendSuccessTrigger = nftSendSuccessTrigger,
            isAccountsModeEnabledUseCase = mockk(relaxed = true),
            getAccountCurrencyStatusUseCase = mockk(relaxed = true),
            analyticsEventHandler = mockk(relaxed = true),
        ).also { model = it }
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }
}