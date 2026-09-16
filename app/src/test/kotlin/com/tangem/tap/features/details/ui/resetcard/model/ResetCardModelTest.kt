package com.tangem.tap.features.details.ui.resetcard.model

import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.tap.common.analytics.events.Settings
import com.tangem.tap.features.details.ui.cardsettings.domain.CardSettingsInteractor
import com.tangem.tap.features.details.ui.resetcard.api.ResetCardComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ResetCardModelTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val resetCardUseCase: ResetCardUseCase = mockk()
    private val deleteSavedAccessCodesUseCase: DeleteSavedAccessCodesUseCase = mockk()
    private val deleteWalletUseCase: DeleteWalletUseCase = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val cardSettingsInteractor: CardSettingsInteractor = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "011a")
    private val userWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(getUserWalletUseCase, getSelectedWalletSyncUseCase, analyticsEventHandler, appRouter)
        every { getUserWalletUseCase(userWalletId = userWalletId) } returns userWallet.right()
    }

    @Test
    fun `GIVEN wallet still exists WHEN onBackClick THEN previous screen is restored`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        model.onBackClick()

        // Assert
        verify(exactly = 1) { appRouter.pop(any()) }
        verify(exactly = 0) { appRouter.replaceAll(*anyVararg(), onComplete = any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN wallet deleted AND no wallets left WHEN onBackClick THEN stack is replaced with home`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        every { getUserWalletUseCase(userWalletId = userWalletId) } returns GetUserWalletError.UserWalletNotFound.left()
        every { getSelectedWalletSyncUseCase() } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        model.onBackClick()

        // Assert
        verify(exactly = 1) {
            appRouter.replaceAll(
                AppRoute.Home(entryReason = AppRoute.Home.EntryReason.LastWalletForgotten),
                onComplete = any(),
            )
        }
        verify(exactly = 0) { appRouter.pop(any()) }
    }

    @Test
    fun `GIVEN wallet deleted AND another wallet selected WHEN onBackClick THEN stack is popped to wallet`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            val remainingWallet: UserWallet.Cold = mockk(relaxed = true)
            every {
                getUserWalletUseCase(userWalletId = userWalletId)
            } returns GetUserWalletError.UserWalletNotFound.left()
            every { getSelectedWalletSyncUseCase() } returns remainingWallet.right()

            // Act
            model.onBackClick()

            // Assert
            verify(exactly = 1) { appRouter.popTo(AppRoute.Wallet::class, any()) }
            verify(exactly = 0) { appRouter.pop(any()) }
        }

    @Test
    fun `GIVEN wallet deleted WHEN onBackClick THEN reset cancellation is tracked`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        every { getUserWalletUseCase(userWalletId = userWalletId) } returns GetUserWalletError.UserWalletNotFound.left()
        every { getSelectedWalletSyncUseCase() } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        model.onBackClick()

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(
                match<Settings.CardSettings.FactoryResetCanceled> { it.params["Cards Count"] == "1" },
            )
        }
    }

    private fun createModel(testScope: TestScope): ResetCardModel {
        return ResetCardModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                value = ResetCardComponent.Params(
                    userWalletId = userWalletId,
                    cardId = "AF05000000000001",
                    isActiveBackupStatus = false,
                    backupCardsCount = 0,
                    hasTangemPay = false,
                ),
            ),
            getUserWalletUseCase = getUserWalletUseCase,
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            resetCardUseCase = resetCardUseCase,
            deleteSavedAccessCodesUseCase = deleteSavedAccessCodesUseCase,
            deleteWalletUseCase = deleteWalletUseCase,
            analyticsEventHandler = analyticsEventHandler,
            cardSettingsInteractor = cardSettingsInteractor,
            appRouter = appRouter,
        )
    }
}