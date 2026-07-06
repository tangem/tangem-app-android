package com.tangem.features.swap.v2.impl.sendviaswap.model

import arrow.core.left
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.api.entry.SendEntryRoute
import com.tangem.features.swap.v2.api.SendWithSwapComponent
import com.tangem.features.swap.v2.impl.sendviaswap.SendWithSwapRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * Guards the send-with-swap navigation refactor: [SendWithSwapModel.onBackClick] /
 * [SendWithSwapModel.onNextClick] now receive the active [com.tangem.core.decompose.navigation.Route]
 * as a parameter instead of reading an internal `currentRoute` StateFlow. Routing is a pure,
 * synchronous decision over [SendWithSwapRoute], so each method is asserted before any advance; the
 * model is then destroyed inside the test body so its `init {}` collectors (`initAppCurrency`,
 * `subscribeOnBalanceHidden`) are cancelled — never run — before `runTest`'s terminal advance. The
 * synchronous `initUserWallet()` is steered to the not-found branch so it never touches the router
 * during construction.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SendWithSwapModelNavigationTest {

    private val router: Router = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val cryptoCurrency = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)

    @Test
    fun `GIVEN amount step WHEN onNextClick THEN pushes destination`() = runTest {
        val model = createModel(this)

        model.onNextClick(SendWithSwapRoute.Amount(isEditMode = false))

        verify(exactly = 1) { router.push(SendWithSwapRoute.Destination(isEditMode = false)) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN destination step WHEN onNextClick THEN pushes confirm`() = runTest {
        val model = createModel(this)

        model.onNextClick(SendWithSwapRoute.Destination(isEditMode = false))

        verify(exactly = 1) { router.push(SendWithSwapRoute.Confirm) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN confirm step WHEN onNextClick THEN pushes success`() = runTest {
        val model = createModel(this)

        model.onNextClick(SendWithSwapRoute.Confirm)

        verify(exactly = 1) { router.push(SendWithSwapRoute.Success) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN success step WHEN onNextClick THEN pops`() = runTest {
        val model = createModel(this)

        model.onNextClick(SendWithSwapRoute.Success)

        verify(exactly = 1) { router.pop() }
        verify(exactly = 0) { router.push(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN edit-mode amount WHEN onNextClick THEN pops instead of advancing`() = runTest {
        val model = createModel(this)

        model.onNextClick(SendWithSwapRoute.Amount(isEditMode = true))

        verify(exactly = 1) { router.pop() }
        verify(exactly = 0) { router.push(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN any route WHEN onBackClick THEN pops`() = runTest {
        val model = createModel(this)

        model.onBackClick(SendWithSwapRoute.Destination(isEditMode = false))

        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope): SendWithSwapModel {
        // Synchronous initUserWallet() runs at construction → keep it on the not-found branch (no router calls).
        every { getUserWalletUseCase(any()) } returns GetUserWalletError.UserWalletNotFound.left()

        val params = SendWithSwapComponent.Params(
            userWalletId = UserWalletId(stringValue = "0123456789"),
            currency = cryptoCurrency,
            callback = null,
            currentRoute = MutableStateFlow(SendEntryRoute.SendWithSwap),
        )
        return SendWithSwapModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            getFeePaidCryptoCurrencyStatusSyncUseCase = mockk(relaxed = true),
            getUserWalletUseCase = getUserWalletUseCase,
            getSelectedAppCurrencyUseCase = mockk(relaxed = true),
            getBalanceHidingSettingsUseCase = mockk(relaxed = true),
            getAccountCurrencyStatusUseCase = mockk(relaxed = true),
            isAccountsModeEnabledUseCase = mockk(relaxed = true),
            swapAlertFactory = mockk(relaxed = true),
            paramsContainer = MutableParamsContainer(value = params),
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