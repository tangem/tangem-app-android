package com.tangem.features.jointaccount.creation.promo.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.account.accountLimitReachedDialog
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Pick
import com.tangem.features.commonfeatures.api.portfolioselector.AccountWalletPicker.Resolution
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent
import com.tangem.features.jointaccount.analytics.JointAccountAnalyticsEvent.LimitSource
import com.tangem.features.jointaccount.creation.model.JointAccountCreationChildParams
import com.tangem.features.jointaccount.creation.model.JointAccountCreationDraftHolder
import com.tangem.features.jointaccount.creation.navigation.JointAccountCreationRoute
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JointAccountPromoModelTest {

    private val router: Router = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val walletPicker: AccountWalletPicker = mockk(relaxed = true)
    private val walletPickerFactory: AccountWalletPicker.Factory = mockk()

    private val picks = MutableSharedFlow<Pick>()

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, analyticsEventHandler, messageSender, walletPicker, walletPickerFactory)
        every { walletPickerFactory.create(any()) } returns walletPicker
        every { walletPicker.picks } returns picks
    }

    @Test
    fun `GIVEN opened from main screen WHEN model created THEN intro event carries that source`() = runTest {
        // Arrange
        val source = AppRoute.JointAccountCreation.Source.MainScreen

        // Act
        createModel(source = source, draftHolder = JointAccountCreationDraftHolder())

        // Assert
        verify(exactly = 1) {
            analyticsEventHandler.send(event = JointAccountAnalyticsEvent.IntroScreenOpened(source = source))
        }
    }

    @Test
    fun `GIVEN wallet known WHEN continue THEN opens config without resolving wallets`() = runTest {
        // Arrange
        val draftHolder = JointAccountCreationDraftHolder().apply { setWalletId(WALLET_A) }
        val model = createModel(draftHolder = draftHolder)
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(JointAccountCreationRoute.Config) }
        coVerify(exactly = 0) { walletPicker.resolve(any()) }
    }

    @Test
    fun `GIVEN no wallet and the only one has room WHEN continue THEN settles it and opens config`() = runTest {
        // Arrange
        coEvery { walletPicker.resolve(any()) } returns Resolution.Single(WALLET_A)
        val draftHolder = JointAccountCreationDraftHolder()
        val model = createModel(draftHolder = draftHolder)
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        assertWalletSettled(draftHolder = draftHolder, walletId = WALLET_A)
    }

    @Test
    fun `GIVEN no wallet and several WHEN continue THEN chooser opens with the full ones marked`() = runTest {
        // Arrange
        coEvery { walletPicker.resolve(any()) } returns Resolution.Choose(fullWalletIds = setOf(WALLET_A))
        val model = createModel(draftHolder = JointAccountCreationDraftHolder())
        advanceUntilIdle()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { walletPicker.openChooser(setOf(WALLET_A)) }
        verify(exactly = 0) { router.push(any()) }
    }

    @Test
    fun `GIVEN chooser open WHEN a wallet with room picked THEN it is settled and config opens`() = runTest {
        // Arrange
        val draftHolder = JointAccountCreationDraftHolder()
        val model = createModel(draftHolder = draftHolder)
        advanceUntilIdle()

        // Act
        picks.emit(Pick.Wallet(WALLET_B))
        advanceUntilIdle()

        // Assert
        assertWalletSettled(draftHolder = draftHolder, walletId = WALLET_B)
        model.onDestroy()
    }

    @Test
    fun `GIVEN chooser open WHEN a full wallet picked THEN limit notice sent and config not opened`() = runTest {
        // Arrange
        val model = createModel(draftHolder = JointAccountCreationDraftHolder())
        advanceUntilIdle()

        // Act
        picks.emit(Pick.Full)
        advanceUntilIdle()

        // Assert
        assertLimitNoticed()
        model.onDestroy()
    }

    @Test
    fun `GIVEN no wallet and the only one is full WHEN continue THEN limit notice sent and config not opened`() =
        runTest {
            // Arrange
            coEvery { walletPicker.resolve(any()) } returns Resolution.LimitReached
            val model = createModel(draftHolder = JointAccountCreationDraftHolder())
            advanceUntilIdle()

            // Act
            model.uiState.value.onContinueClick()
            advanceUntilIdle()

            // Assert
            assertLimitNoticed()
        }

    @Test
    fun `GIVEN account lists unavailable WHEN continue THEN error dialog shown without limit notice`() = runTest {
        // Arrange
        coEvery { walletPicker.resolve(any()) } returns Resolution.Unavailable
        val model = createModel(draftHolder = JointAccountCreationDraftHolder())
        advanceUntilIdle()
        val message = slot<UiMessage>()

        // Act
        model.uiState.value.onContinueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(capture(message)) }
        assertThat(isLimitDialog(message.captured)).isFalse()
        verify(exactly = 0) { analyticsEventHandler.send(any<JointAccountAnalyticsEvent.NoticeAccountsLimitReached>()) }
        verify(exactly = 0) { router.push(any()) }
    }

    private fun assertLimitNoticed() {
        verify(exactly = 1) {
            analyticsEventHandler.send(JointAccountAnalyticsEvent.NoticeAccountsLimitReached(LimitSource.Create))
        }
        verify(exactly = 1) { messageSender.send(match(::isLimitDialog)) }
        verify(exactly = 0) { router.push(any()) }
    }

    private fun isLimitDialog(message: UiMessage): Boolean = (message as? DialogMessage)?.message ==
        accountLimitReachedDialog(limit = AccountList.MAX_JOINT_ACCOUNTS_COUNT).message

    private fun assertWalletSettled(draftHolder: JointAccountCreationDraftHolder, walletId: UserWalletId) {
        assertThat(draftHolder.draft.value.walletId).isEqualTo(walletId)
        verify(exactly = 1) { router.push(JointAccountCreationRoute.Config) }
    }

    private fun TestScope.createModel(
        draftHolder: JointAccountCreationDraftHolder,
        source: AppRoute.JointAccountCreation.Source = AppRoute.JointAccountCreation.Source.AllServices,
    ) = JointAccountPromoModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            JointAccountCreationChildParams(source = source, draftHolder = draftHolder),
        ),
        router = router,
        analyticsEventHandler = analyticsEventHandler,
        messageSender = messageSender,
        accountWalletPickerFactory = walletPickerFactory,
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}