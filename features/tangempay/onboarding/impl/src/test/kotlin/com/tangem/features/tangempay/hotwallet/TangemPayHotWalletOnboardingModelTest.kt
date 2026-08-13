package com.tangem.features.tangempay.hotwallet

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayHotWalletOnboardingModelTest {

    private val createHotWalletUseCase: CreateHotWalletUseCase = mockk()
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk() {
        every { getLeastVersionName() } returns "Android 10"
    }
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val testUserWallet: UserWallet.Hot = mockk(relaxed = true) {
        every { walletId } returns testUserWalletId
    }

    @Nested
    inner class OnTermsClick {

        @Test
        fun `WHEN onTermsClick THEN navigate to Disclaimer`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.uiState.value.onTermsClick.invoke()
            advanceUntilIdle()

            // Assert
            verify { router.push(AppRoute.Disclaimer(isTosAccepted = true)) }
            model.onDestroy()
        }
    }

    @Nested
    inner class OnGetCardClick {

        @Test
        fun `GIVEN hot wallet creation not supported WHEN onGetCardClick THEN wallet creation not attempted`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns false
                val model = createModel(testScope = this)

                // Act
                model.uiState.value.onGetCardClick.invoke()
                advanceUntilIdle()

                // Assert
                verify { uiMessageSender.send(any()) }
                verify { router.replaceCurrent(AppRoute.Home()) }
                coVerify(exactly = 0) { createHotWalletUseCase(any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN hot wallet supported AND wallet creation succeeds WHEN onGetCardClick THEN navigate to TangemPayOnboarding`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns true
                coEvery { createHotWalletUseCase(HotAuth.NoAuth, MnemonicType.Words12) } returns testUserWallet.right()
                val model = createModel(testScope = this)

                // Act
                model.uiState.value.onGetCardClick.invoke()
                advanceUntilIdle()

                // Assert
                verify {
                    router.replaceAll(
                        AppRoute.TangemPayOnboarding(
                            mode = AppRoute.TangemPayOnboarding.Mode.FirstSetup(testUserWalletId),
                        ),
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN hot wallet supported AND wallet creation fails WHEN onGetCardClick THEN error dialog sent`() =
            runTest {
                // Arrange
                every { isHotWalletCreationSupported() } returns true
                coEvery {
                    createHotWalletUseCase(HotAuth.NoAuth, MnemonicType.Words12)
                } returns RuntimeException("error").left()
                val model = createModel(testScope = this)

                // Act
                model.uiState.value.onGetCardClick.invoke()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.isLoading).isFalse()
                verify { uiMessageSender.send(match<DialogMessage> { true }) }
                verify(exactly = 0) { router.replaceAll(*anyVararg()) }
                model.onDestroy()
            }
    }

    private fun createModel(testScope: TestScope): TangemPayHotWalletOnboardingModel {
        return TangemPayHotWalletOnboardingModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            createHotWalletUseCase = createHotWalletUseCase,
            isHotWalletCreationSupported = isHotWalletCreationSupported,
            router = router,
            uiMessageSender = uiMessageSender,
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