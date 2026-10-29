package com.tangem.features.introduction.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.OneTimeEventStore
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.analytics.IntroductionProcess.ScreenOpened.EntryReason
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IntroductionEntryReasonTrackerTest {

    private val oneTimeEventStore: OneTimeEventStore = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk()

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private lateinit var tracker: IntroductionEntryReasonTracker

    @BeforeEach
    fun resetMocks() {
        clearMocks(oneTimeEventStore, analyticsEventHandler)
        coEvery { oneTimeEventStore.markSent(any()) } returns true
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just runs
        tracker = IntroductionEntryReasonTracker(oneTimeEventStore, analyticsEventHandler)
    }

    @Test
    fun `GIVEN the screen was never shown WHEN the first opening is consumed THEN it is the first launch and gets recorded`() =
        runTest {
            // Arrange
            coEvery { oneTimeEventStore.markSent(any()) } returns false

            // Act
            tracker.reportOpening(AppRoute.Home.EntryReason.Launch)

            // Assert
            assertThat(sentEvents).containsExactly(IntroductionProcess.ScreenOpened(EntryReason.FirstLaunch))
            coVerify(exactly = 1) { oneTimeEventStore.markSent("introduction_welcome_opened") }
        }

    @Test
    fun `GIVEN the screen was shown before WHEN the first opening is consumed THEN it is no wallets`() =
        runTest {
            // Act
            tracker.reportOpening(AppRoute.Home.EntryReason.Launch)

            // Assert
            assertThat(sentEvents).containsExactly(IntroductionProcess.ScreenOpened(EntryReason.NoWallets))
        }

    @Test
    fun `GIVEN an opening was reported WHEN the screen is rebuilt in the same process THEN it stays silent`() = runTest {
        // Arrange
        tracker.reportOpening(AppRoute.Home.EntryReason.Launch)

        // Act
        tracker.reportOpening(AppRoute.Home.EntryReason.Launch)

        // Assert
        assertThat(sentEvents).containsExactly(IntroductionProcess.ScreenOpened(EntryReason.NoWallets))
    }

    @Test
    fun `GIVEN an opening was reported WHEN the last wallet is forgotten THEN it is reported again`() = runTest {
        // Arrange
        tracker.reportOpening(AppRoute.Home.EntryReason.Launch)

        // Act
        tracker.reportOpening(AppRoute.Home.EntryReason.LastWalletForgotten)

        // Assert
        assertThat(sentEvents).containsExactly(
            IntroductionProcess.ScreenOpened(EntryReason.NoWallets),
            IntroductionProcess.ScreenOpened(EntryReason.LastWalletForgotten),
        ).inOrder()
    }

    @Test
    fun `GIVEN the store fails WHEN the first opening is consumed THEN it falls back to no wallets`() = runTest {
        // Arrange
        coEvery { oneTimeEventStore.markSent(any()) } throws IllegalStateException("corrupt store")

        // Act
        tracker.reportOpening(AppRoute.Home.EntryReason.Launch)

        // Assert
        assertThat(sentEvents).containsExactly(IntroductionProcess.ScreenOpened(EntryReason.NoWallets))
    }
}