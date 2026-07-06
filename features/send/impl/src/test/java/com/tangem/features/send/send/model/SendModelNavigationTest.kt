package com.tangem.features.send.send.model

import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.features.send.api.SendComponent
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents.CloseButtonClicked as CloseButtonClickedEvent

/**
 * Guards the navigation refactor that moved the footer/app-bar actions out of the model into
 * `DefaultSendComponent`. The model's [SendModel.onBackClick] / [SendModel.onNextClick] no longer read
 * an internal `currentRoute` StateFlow — they receive the active [com.tangem.core.decompose.navigation.Route]
 * as a parameter and decide routing/analytics from it. These are pure, synchronous decisions, so each
 * method is asserted *before* advancing the scheduler; the model is then destroyed inside the test body
 * so its `init {}` collectors (all `modelScope.launch`/`launchIn`, including an infinite status collector)
 * are cancelled — never run — before `runTest`'s terminal advance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SendModelNavigationTest {

    private val router: Router = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase = mockk(relaxed = true)
    private val cryptoCurrency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)

    // region onNextClick

    @Test
    fun `GIVEN manual entry on amount WHEN onNextClick THEN pushes destination`() = runTest {
        val model = createModel(this)

        model.onNextClick(CommonSendRoute.Amount(isEditMode = false))

        verify(exactly = 1) { router.push(CommonSendRoute.Destination(isEditMode = false)) }
        verify(exactly = 0) { router.push(CommonSendRoute.Confirm) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN main-screen QR predefined values WHEN onNextClick on amount THEN skips destination and pushes confirm`() =
        runTest {
            // Arrange — address-only predefined values resolve to a MAIN_SCREEN QrCode (isFromMainScreenQr = true).
            val model = createModel(this, params = qrParams())

            // Act
            model.onNextClick(CommonSendRoute.Amount(isEditMode = false))

            // Assert
            verify(exactly = 1) { router.push(CommonSendRoute.Confirm) }
            verify(exactly = 0) { router.push(CommonSendRoute.Destination(isEditMode = false)) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN destination step WHEN onNextClick THEN pushes confirm`() = runTest {
        val model = createModel(this)

        model.onNextClick(CommonSendRoute.Destination(isEditMode = false))

        verify(exactly = 1) { router.push(CommonSendRoute.Confirm) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN edit-mode route WHEN onNextClick THEN pops instead of advancing`() = runTest {
        val model = createModel(this)

        model.onNextClick(CommonSendRoute.Amount(isEditMode = true))

        verify(exactly = 1) { router.pop() }
        verify(exactly = 0) { router.push(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN confirm-success route WHEN onNextClick THEN pops`() = runTest {
        val model = createModel(this)

        model.onNextClick(CommonSendRoute.ConfirmSuccess)

        verify(exactly = 1) { router.pop() }
        verify(exactly = 0) { router.push(any()) }
        model.onDestroy()
    }

    // endregion

    // region onBackClick

    @Test
    fun `GIVEN amount step not in edit mode WHEN onBackClick THEN sends amount close analytics and pops`() = runTest {
        val model = createModel(this)

        model.onBackClick(CommonSendRoute.Amount(isEditMode = false))

        verify(exactly = 1) {
            analyticsEventHandler.send(match { it is CloseButtonClickedEvent && it.source == SendScreenSource.Amount })
        }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN destination step not in edit mode WHEN onBackClick THEN sends address close analytics and pops`() =
        runTest {
            val model = createModel(this)

            model.onBackClick(CommonSendRoute.Destination(isEditMode = false))

            verify(exactly = 1) {
                analyticsEventHandler.send(
                    match { it is CloseButtonClickedEvent && it.source == SendScreenSource.Address },
                )
            }
            verify(exactly = 1) { router.pop() }
            model.onDestroy()
        }

    @Test
    fun `GIVEN edit-mode route WHEN onBackClick THEN pops without close analytics`() = runTest {
        val model = createModel(this)

        model.onBackClick(CommonSendRoute.Amount(isEditMode = true))

        verify(exactly = 0) { analyticsEventHandler.send(any<CloseButtonClickedEvent>()) }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    // endregion

    private fun manualParams() = SendComponent.Params(
        userWalletId = UserWalletId(stringValue = "0123456789"),
        currency = cryptoCurrency,
    )

    private fun qrParams() = SendComponent.Params(
        userWalletId = UserWalletId(stringValue = "0123456789"),
        currency = cryptoCurrency,
        destinationAddress = "0xRECIPIENT",
    )

    private fun createModel(testScope: TestScope, params: SendComponent.Params = manualParams()): SendModel {
        // Runs synchronously in init {}; a relaxed Either<Exception, …> would break getOrElse, so stub a Right.
        every { listenToQrScanningUseCase(any()) } returns emptyFlow<String>().right()
        return SendModel(
            paramsContainer = MutableParamsContainer(value = params),
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            getUserWalletUseCase = mockk(relaxed = true),
            getFeePaidCryptoCurrencyStatusSyncUseCase = mockk(relaxed = true),
            getSelectedAppCurrencyUseCase = mockk(relaxed = true),
            listenToQrScanningUseCase = listenToQrScanningUseCase,
            parseQrCodeUseCase = mockk(relaxed = true),
            sendConfirmAlertFactory = mockk(relaxed = true),
            saveBlockchainErrorUseCase = mockk(relaxed = true),
            getWalletMetaInfoUseCase = mockk(relaxed = true),
            sendFeedbackEmailUseCase = mockk(relaxed = true),
            getBalanceHidingSettingsUseCase = mockk(relaxed = true),
            createTransferTransactionUseCase = mockk(relaxed = true),
            getFeeUseCase = mockk(relaxed = true),
            getFeeForGaslessUseCase = mockk(relaxed = true),
            getFeeForTokenUseCase = mockk(relaxed = true),
            getAccountCurrencyStatusUseCase = mockk(relaxed = true),
            isAccountsModeEnabledUseCase = mockk(relaxed = true),
            sendAmountUpdateTrigger = mockk(relaxed = true),
            analyticsEventHandler = analyticsEventHandler,
        )
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