package com.tangem.feature.usedesk.deeplink

import android.net.Uri
import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SupportDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk()

    private val userWallet: UserWallet = mockk<UserWallet.Cold> {
        every { walletId } returns USER_WALLET_ID
    }
    private val walletMetaInfo = WalletMetaInfo(userWalletId = USER_WALLET_ID)
    private val deeplinkUsedeskRoute = AppRoute.Usedesk(
        walletMetaInfo = walletMetaInfo,
        source = AnalyticsParam.ScreensSources.Deeplink,
    )
    private val settingsUsedeskRoute = AppRoute.Usedesk(
        walletMetaInfo = walletMetaInfo,
        source = AnalyticsParam.ScreensSources.Settings,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(appRouter, getSelectedWalletSyncUseCase, getWalletMetaInfoUseCase)
        mockkObject(TangemLogger)
        every { TangemLogger.e(any(), any(), any()) } just Runs
        every { getSelectedWalletSyncUseCase() } returns Either.Right(userWallet)
        coEvery { getWalletMetaInfoUseCase(USER_WALLET_ID) } returns Either.Right(walletMetaInfo)
        every { appRouter.stack } returns listOf(AppRoute.Wallet)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    @Test
    fun `GIVEN selected wallet and meta info WHEN handled THEN pushes Usedesk with Deeplink source`() =
        runTest {
            // Act
            handle()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { appRouter.push(deeplinkUsedeskRoute, any()) }
        }

    @Test
    fun `GIVEN no selected wallet WHEN handled THEN does not request meta info and does not push`() =
        runTest {
            // Arrange
            every { getSelectedWalletSyncUseCase() } returns Either.Left(GetUserWalletError.UserWalletNotFound)

            // Act
            handle()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { getWalletMetaInfoUseCase(any<UserWalletId>()) }
            verify(exactly = 0) { appRouter.push(any(), any()) }
            verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
        }

    @Test
    fun `GIVEN meta info fails WHEN handled THEN does not push`() = runTest {
        // Arrange
        coEvery { getWalletMetaInfoUseCase(USER_WALLET_ID) } returns Either.Left(IllegalStateException("No meta"))

        // Act
        handle()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { appRouter.push(any(), any()) }
        verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
    }

    @Test
    fun `GIVEN chat on top of stack WHEN handled THEN neither pushes nor pops`() = runTest {
        // Arrange
        every { appRouter.stack } returns listOf(AppRoute.Wallet, settingsUsedeskRoute)

        // Act
        handle()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { appRouter.push(any(), any()) }
        verify(exactly = 0) { appRouter.popTo(any<AppRoute>(), any()) }
    }

    @Test
    fun `GIVEN chat covered by another screen WHEN handled THEN pops back to that chat`() = runTest {
        // Arrange
        every { appRouter.stack } returns listOf(
            AppRoute.Wallet,
            settingsUsedeskRoute,
            AppRoute.ReferralProgram(userWalletId = USER_WALLET_ID),
        )

        // Act
        handle()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { appRouter.popTo(settingsUsedeskRoute, any()) }
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    private fun TestScope.handle() = SupportDeepLinkHandler(
        appRouter = appRouter,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
    ).handle(
        DeepLinkRequest(uri = mockk<Uri>(), queryParams = emptyMap(), scope = this, isFromOnNewIntent = false),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}