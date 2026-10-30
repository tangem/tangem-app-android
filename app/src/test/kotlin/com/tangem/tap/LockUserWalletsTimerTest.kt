package com.tangem.tap

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkManager
import androidx.work.WorkRequest
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.LockWalletsError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.wallets.usecase.ClearAllHotWalletContextualUnlockUseCase
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class LockUserWalletsTimerTest {

    private val context: Context = mockk()
    private val settingsRepository: SettingsRepository = mockk(relaxUnitFun = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val clearAllHotWalletContextualUnlockUseCase: ClearAllHotWalletContextualUnlockUseCase =
        mockk(relaxed = true)
    private val passwordRequester: HotWalletPasswordRequester = mockk(relaxUnitFun = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)
    private val owner: LifecycleOwner = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(
            settingsRepository,
            userWalletsListRepository,
            clearAllHotWalletContextualUnlockUseCase,
            passwordRequester,
            appRouter,
            workManager,
        )
        mockkStatic(WorkManager::class)
        every { WorkManager.getInstance(any()) } returns workManager
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(mockk<UserWallet>())
        coEvery { userWalletsListRepository.lockAllWallets() } returns Unit.right()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(WorkManager::class)
    }

    @Nested
    inner class OnStart {

        @Test
        fun `GIVEN welcome screen requested WHEN onStart THEN resets to welcome and clears the request`() = runTest {
            // Arrange
            coEvery { settingsRepository.shouldOpenWelcomeScreenOnResume() } returns true
            val timer = createTimer()

            // Act
            timer.onStart(owner)
            runCurrent()

            // Assert
            verify(exactly = 1) { workManager.cancelAllWorkByTag(LockTimerWorker.TAG) }
            coVerify(exactly = 1) { passwordRequester.dismiss() }
            verify(exactly = 1) { clearAllHotWalletContextualUnlockUseCase.invoke() }
            verify(exactly = 1) { appRouter.replaceAll(AppRoute.Welcome(), onComplete = any()) }
            coVerify(exactly = 1) { settingsRepository.setShouldOpenWelcomeScreenOnResume(value = false) }
        }

        @Test
        fun `GIVEN welcome screen not requested WHEN onStart THEN keeps the current screen`() = runTest {
            // Arrange
            coEvery { settingsRepository.shouldOpenWelcomeScreenOnResume() } returns false
            val timer = createTimer()

            // Act
            timer.onStart(owner)
            runCurrent()

            // Assert
            verify(exactly = 1) { workManager.cancelAllWorkByTag(LockTimerWorker.TAG) }
            verify(exactly = 0) { appRouter.replaceAll(*anyVararg(), onComplete = any()) }
            coVerify(exactly = 0) { settingsRepository.setShouldOpenWelcomeScreenOnResume(any()) }
        }
    }

    @Nested
    inner class Timer {

        @Test
        fun `GIVEN resumed WHEN duration passes THEN locks wallets and resets to welcome`() = runTest {
            // Arrange
            val timer = createTimer()
            timer.onResume(owner)

            // Act
            advanceTimeBy(DURATION + 1.seconds)

            // Assert
            coVerify(exactly = 1) { userWalletsListRepository.lockAllWallets() }
            verify(exactly = 1) { appRouter.replaceAll(AppRoute.Welcome(), onComplete = any()) }
        }

        @Test
        fun `GIVEN resumed WHEN duration has not passed THEN wallets stay unlocked`() = runTest {
            // Arrange
            val timer = createTimer()
            timer.onResume(owner)

            // Act
            advanceTimeBy(DURATION - 1.seconds)

            // Assert
            coVerify(exactly = 0) { userWalletsListRepository.lockAllWallets() }
        }

        @Test
        fun `GIVEN no wallets WHEN duration passes THEN nothing is locked`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            val timer = createTimer()
            timer.onResume(owner)

            // Act
            advanceTimeBy(DURATION + 1.seconds)

            // Assert
            coVerify(exactly = 0) { userWalletsListRepository.lockAllWallets() }
            verify(exactly = 0) { appRouter.replaceAll(*anyVararg(), onComplete = any()) }
        }

        @Test
        fun `GIVEN lock fails WHEN duration passes THEN timer starts again`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.lockAllWallets() } returns LockWalletsError.NothingToLock.left()
            val timer = createTimer()
            timer.onResume(owner)

            // Act
            advanceTimeBy(DURATION * 2 + 1.seconds)

            // Assert
            coVerify(exactly = 2) { userWalletsListRepository.lockAllWallets() }
            verify(exactly = 0) { appRouter.replaceAll(*anyVararg(), onComplete = any()) }
        }
    }

    @Nested
    inner class Restart {

        @Test
        fun `GIVEN running timer WHEN restart THEN duration is counted from the restart`() = runTest {
            // Arrange
            val timer = createTimer()
            timer.onResume(owner)
            advanceTimeBy(DURATION - 1.minutes)

            // Act
            timer.restart()
            advanceTimeBy(DURATION - 1.seconds)

            // Assert
            coVerify(exactly = 0) { userWalletsListRepository.lockAllWallets() }

            advanceTimeBy(2.seconds)
            coVerify(exactly = 1) { userWalletsListRepository.lockAllWallets() }
        }

        @Test
        fun `GIVEN timer not started WHEN restart THEN nothing is scheduled`() = runTest {
            // Arrange
            val timer = createTimer()

            // Act
            timer.restart()
            advanceTimeBy(DURATION + 1.seconds)

            // Assert
            coVerify(exactly = 0) { userWalletsListRepository.lockAllWallets() }
        }
    }

    @Nested
    inner class OnStop {

        @Test
        fun `GIVEN running timer WHEN onStop THEN foreground timer stops and background worker is enqueued`() =
            runTest {
                // Arrange
                val request = slot<WorkRequest>()
                every { workManager.enqueue(capture(request)) } returns mockk()
                val timer = createTimer()
                timer.onResume(owner)

                // Act
                timer.onStop(owner)
                advanceTimeBy(DURATION + 1.seconds)

                // Assert
                coVerify(exactly = 0) { userWalletsListRepository.lockAllWallets() }
                assertThat(request.captured.tags).contains(LockTimerWorker.TAG)
                assertThat(request.captured.workSpec.initialDelay).isEqualTo(DURATION.inWholeMilliseconds)
            }
    }

    @Nested
    inner class Attach {

        private val firstLifecycle: Lifecycle = mockk(relaxed = true)
        private val secondLifecycle: Lifecycle = mockk(relaxed = true)
        private val firstOwner: LifecycleOwner = mockk { every { lifecycle } returns firstLifecycle }
        private val secondOwner: LifecycleOwner = mockk { every { lifecycle } returns secondLifecycle }

        @Test
        fun `GIVEN attached to an owner WHEN attach to another THEN observes only the new one`() = runTest {
            // Arrange
            val timer = createTimer()
            timer.attach(firstOwner)

            // Act
            timer.attach(secondOwner)

            // Assert
            verify(exactly = 1) { firstLifecycle.addObserver(timer) }
            verify(exactly = 1) { firstLifecycle.removeObserver(timer) }
            verify(exactly = 1) { secondLifecycle.addObserver(timer) }
            verify(exactly = 0) { secondLifecycle.removeObserver(timer) }
        }

        @Test
        fun `GIVEN attached owner destroyed WHEN attach to another THEN destroyed lifecycle is not touched again`() =
            runTest {
                // Arrange
                val timer = createTimer()
                timer.attach(firstOwner)
                timer.onDestroy(firstOwner)

                // Act
                timer.attach(secondOwner)

                // Assert
                verify(exactly = 1) { firstLifecycle.removeObserver(timer) }
                verify(exactly = 1) { secondLifecycle.addObserver(timer) }
            }

        @Test
        fun `GIVEN previous owner destroyed after a new one attached WHEN attach again THEN detaches the new one`() =
            runTest {
                // Arrange
                val thirdOwner: LifecycleOwner = mockk { every { lifecycle } returns mockk(relaxed = true) }
                val timer = createTimer()
                timer.attach(firstOwner)
                timer.attach(secondOwner)
                timer.onDestroy(firstOwner)

                // Act
                timer.attach(thirdOwner)

                // Assert
                verify(exactly = 1) { secondLifecycle.removeObserver(timer) }
            }
    }

    private fun TestScope.createTimer(): LockUserWalletsTimer = LockUserWalletsTimer(
        context = context,
        settingsRepository = settingsRepository,
        userWalletsListRepository = userWalletsListRepository,
        clearAllHotWalletContextualUnlockUseCase = clearAllHotWalletContextualUnlockUseCase,
        passwordRequester = passwordRequester,
        appRouter = appRouter,
        appScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
        dispatchers = createTestingCoroutineDispatcherProvider(),
    )

    private companion object {
        val DURATION = 5.minutes
    }
}