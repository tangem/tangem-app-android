package com.tangem.features.tangempay.hotwallet

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.appsflyer.AppsFlyerDeeplinkSource
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class TangemPayHotWalletOnboardingModelTest {

    private val createHotWalletUseCase: CreateHotWalletUseCase = mockk()
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported = mockk() {
        every { getLeastVersionName() } returns "Android 10"
    }
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val testUserWallet: UserWallet.Hot = mockk(relaxed = true) {
        every { walletId } returns testUserWalletId
    }

    @Nested
    inner class OnTermsClick {

        @Test
        fun `WHEN onTermsClick THEN urlOpener called with terms link`() = runTest {
            val model = createModel()

            model.uiState.value.onTermsClick.invoke()

            verify { urlOpener.openUrl(TangemPayConstants.TERMS_AND_LIMITS_LINK) }
        }
    }

    @Nested
    inner class OnGetCardClick {

        @Test
        fun `GIVEN hot wallet creation not supported WHEN onGetCardClick THEN wallet creation not attempted`() =
            runTest {
                every { isHotWalletCreationSupported() } returns false
                coEvery { clearAppsFlyerDeeplinkUseCase(any()) } just Runs

                val model = createModel()
                model.uiState.value.onGetCardClick.invoke()

                verify { uiMessageSender.send(any()) }
                verify { router.replaceCurrent(AppRoute.Home()) }
                coVerify { clearAppsFlyerDeeplinkUseCase(AppsFlyerDeeplinkSource.TangemPayHotWalletOnboarding) }
                coVerify(exactly = 0) { createHotWalletUseCase(any(), any()) }
            }

        @Test
        fun `GIVEN hot wallet supported AND wallet creation succeeds WHEN onGetCardClick THEN deeplink cleared AND navigate to CreateWalletBackup`() =
            runTest {
                every { isHotWalletCreationSupported() } returns true
                coEvery { createHotWalletUseCase(HotAuth.NoAuth, MnemonicType.Words12) } returns testUserWallet.right()
                coEvery { clearAppsFlyerDeeplinkUseCase(AppsFlyerDeeplinkSource.TangemPayHotWalletOnboarding) } just Runs

                val model = createModel()
                model.uiState.value.onGetCardClick.invoke()

                coVerify { clearAppsFlyerDeeplinkUseCase(AppsFlyerDeeplinkSource.TangemPayHotWalletOnboarding) }
                verify {
                    router.replaceCurrent(
                        match { it is AppRoute.CreateWalletBackup && it.userWalletId == testUserWalletId },
                    )
                }
            }

        @Test
        fun `GIVEN hot wallet supported AND wallet creation fails WHEN onGetCardClick THEN error dialog sent`() =
            runTest {
                every { isHotWalletCreationSupported() } returns true
                coEvery {
                    createHotWalletUseCase(HotAuth.NoAuth, MnemonicType.Words12)
                } returns RuntimeException("error").left()

                val model = createModel()
                model.uiState.value.onGetCardClick.invoke()

                assertThat(model.uiState.value.isLoading).isFalse()
                verify { uiMessageSender.send(match<DialogMessage> { true }) }
                coVerify(exactly = 0) { clearAppsFlyerDeeplinkUseCase(any()) }
                verify(exactly = 0) { router.replaceCurrent(any()) }
            }
    }

    private fun createModel(): TangemPayHotWalletOnboardingModel {
        return TangemPayHotWalletOnboardingModel(
            dispatchers = TestingCoroutineDispatcherProvider(),
            createHotWalletUseCase = createHotWalletUseCase,
            isHotWalletCreationSupported = isHotWalletCreationSupported,
            clearAppsFlyerDeeplinkUseCase = clearAppsFlyerDeeplinkUseCase,
            router = router,
            uiMessageSender = uiMessageSender,
            urlOpener = urlOpener,
        )
    }
}