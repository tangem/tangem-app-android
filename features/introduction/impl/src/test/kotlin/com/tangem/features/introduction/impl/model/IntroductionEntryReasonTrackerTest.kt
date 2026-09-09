package com.tangem.features.introduction.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.entity.HomeEntryReason
import com.tangem.domain.analytics.repository.AnalyticsRepository
import com.tangem.domain.card.analytics.IntroductionProcess.ScreenOpened.EntryReason
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IntroductionEntryReasonTrackerTest {

    private val analyticsRepository: AnalyticsRepository = mockk(relaxed = true)

    private lateinit var tracker: IntroductionEntryReasonTracker

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsRepository)
        coEvery { analyticsRepository.checkIsEventSent(any()) } returns true
        tracker = IntroductionEntryReasonTracker(analyticsRepository)
    }

    @Test
    fun `GIVEN the screen was never shown WHEN the first opening is consumed THEN it is the first launch and gets recorded`() =
        runTest {
            // Arrange
            coEvery { analyticsRepository.checkIsEventSent(any()) } returns false

            // Act
            val reason = tracker.consume(HomeEntryReason.Launch)

            // Assert
            assertThat(reason).isEqualTo(EntryReason.FirstLaunch)
            coVerify(exactly = 1) { analyticsRepository.setIsEventSent("introduction_welcome_opened") }
        }

    @Test
    fun `GIVEN the screen was shown before WHEN the first opening is consumed THEN it is no wallets and nothing is written`() =
        runTest {
            // Act
            val reason = tracker.consume(HomeEntryReason.Launch)

            // Assert
            assertThat(reason).isEqualTo(EntryReason.NoWallets)
            coVerify(exactly = 0) { analyticsRepository.setIsEventSent(any()) }
        }

    @Test
    fun `GIVEN an opening was reported WHEN the screen is rebuilt in the same process THEN it stays silent`() = runTest {
        // Arrange
        tracker.consume(HomeEntryReason.Launch)

        // Act
        val reason = tracker.consume(HomeEntryReason.Launch)

        // Assert
        assertThat(reason).isNull()
    }

    @Test
    fun `GIVEN an opening was reported WHEN the last wallet is forgotten THEN it is reported again`() = runTest {
        // Arrange
        tracker.consume(HomeEntryReason.Launch)

        // Act
        val reason = tracker.consume(HomeEntryReason.LastWalletForgotten)

        // Assert
        assertThat(reason).isEqualTo(EntryReason.LastWalletForgotten)
    }

    @Test
    fun `GIVEN the store fails WHEN the first opening is consumed THEN it falls back to no wallets`() = runTest {
        // Arrange
        coEvery { analyticsRepository.checkIsEventSent(any()) } throws IllegalStateException("corrupt store")

        // Act
        val reason = tracker.consume(HomeEntryReason.Launch)

        // Assert
        assertThat(reason).isEqualTo(EntryReason.NoWallets)
    }
}