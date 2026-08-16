package com.tangem.features.hotwallet.updateaccesscode

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.hotwallet.SetAccessCodeSkippedUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.hotwallet.UpdateAccessCodeComponent
import com.tangem.features.hotwallet.updateaccesscode.routing.UpdateAccessCodeRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class UpdateAccessCodeModelTest {

    private val router: Router = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val setAccessCodeSkippedUseCase: SetAccessCodeSkippedUseCase = mockk()
    private val paramsContainer: ParamsContainer = mockk()

    private val walletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(router, analyticsEventHandler, uiMessageSender, setAccessCodeSkippedUseCase, paramsContainer)
        coEvery { setAccessCodeSkippedUseCase(walletId, true) } returns Unit.right()
    }

    @Test
    fun `GIVEN canSkip false WHEN routes resolved THEN skip button is never visible`() = runTest {
        // Arrange
        val model = createModel(this, canSkip = false)

        // Act & Assert
        assertThat(model.isSkipButtonVisible(UpdateAccessCodeRoute.SetAccessCode(walletId))).isFalse()
        assertThat(model.isSkipButtonVisible(UpdateAccessCodeRoute.ConfirmAccessCode(walletId, "1111"))).isFalse()
        assertThat(model.isSkipButtonVisible(UpdateAccessCodeRoute.SetupFinished)).isFalse()
    }

    @Test
    fun `GIVEN canSkip true WHEN routes resolved THEN skip visible on access code steps only`() = runTest {
        // Arrange
        val model = createModel(this, canSkip = true)

        // Act & Assert
        assertThat(model.isSkipButtonVisible(UpdateAccessCodeRoute.SetAccessCode(walletId))).isTrue()
        assertThat(model.isSkipButtonVisible(UpdateAccessCodeRoute.ConfirmAccessCode(walletId, "1111"))).isTrue()
        assertThat(model.isSkipButtonVisible(UpdateAccessCodeRoute.SetupFinished)).isFalse()
    }

    @Test
    fun `GIVEN canSkip true WHEN skip clicked THEN warning dialog is shown`() = runTest {
        // Arrange
        val sentMessages = mutableListOf<UiMessage>()
        every { uiMessageSender.send(capture(sentMessages)) } just Runs
        val model = createModel(this, canSkip = true)

        // Act
        model.onSkipClick()

        // Assert
        assertThat(sentMessages.filterIsInstance<DialogMessage>()).hasSize(1)
        coVerify(exactly = 0) { setAccessCodeSkippedUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN skip dialog WHEN skip confirmed THEN skipped flag set AND SetupFinished pushed`() = runTest {
        // Arrange
        val sentMessages = mutableListOf<UiMessage>()
        every { uiMessageSender.send(capture(sentMessages)) } just Runs
        val model = createModel(this, canSkip = true)
        val stacks = mutableListOf<List<UpdateAccessCodeRoute>>()
        model.stackNavigation.subscribe { event -> stacks += event.transformer(listOf(model.startRoute)) }
        model.onSkipClick()

        // Act
        val dialog = sentMessages.filterIsInstance<DialogMessage>().single()
        requireNotNull(dialog.secondAction).onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { setAccessCodeSkippedUseCase(walletId, true) }
        assertThat(stacks.last().last()).isEqualTo(UpdateAccessCodeRoute.SetupFinished)
        verify(exactly = 0) { router.pop(onComplete = any()) }
    }

    @Test
    fun `GIVEN access code update started WHEN skip clicked THEN dialog is not shown`() = runTest {
        // Arrange
        val model = createModel(this, canSkip = true)
        model.onAccessCodeUpdateStarted(walletId)

        // Act
        model.onSkipClick()

        // Assert
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    private fun createModel(testScope: TestScope, canSkip: Boolean): UpdateAccessCodeModel {
        every { paramsContainer.require<UpdateAccessCodeComponent.Params>() } returns
            UpdateAccessCodeComponent.Params(
                userWalletId = walletId,
                source = "Settings",
                canSkip = canSkip,
            )
        return UpdateAccessCodeModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            paramsContainer = paramsContainer,
            analyticsEventHandler = analyticsEventHandler,
            uiMessageSender = uiMessageSender,
            setAccessCodeSkippedUseCase = setAccessCodeSkippedUseCase,
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