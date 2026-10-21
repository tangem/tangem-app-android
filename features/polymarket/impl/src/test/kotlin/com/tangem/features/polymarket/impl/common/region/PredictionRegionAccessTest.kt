package com.tangem.features.polymarket.impl.common.region

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class PredictionRegionAccessTest {

    private val checkGeoblock: CheckPolymarketGeoblockUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(checkGeoblock)
    }

    @Test
    fun `GIVEN a blocked region WHEN refreshed THEN the status is restricted`() = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns true.right()
        val access = PredictionRegionAccess(checkGeoblock, createTestingCoroutineDispatcherProvider())

        // Act
        access.refresh()

        // Assert
        assertThat(access.status.value).isEqualTo(PredictionRegionStatus.RESTRICTED)
        assertThat(access.isRestricted).isTrue()
    }

    @Test
    fun `GIVEN a failed check WHEN refreshed THEN the status is allowed`() = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns PolymarketOnboardingError.Network.left()
        val access = PredictionRegionAccess(checkGeoblock, createTestingCoroutineDispatcherProvider())

        // Act
        access.refresh()

        // Assert
        assertThat(access.status.value).isEqualTo(PredictionRegionStatus.ALLOWED)
    }

    @Test
    fun `GIVEN a restricted region WHEN a later check fails THEN the region stays restricted`() = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns true.right() andThen PolymarketOnboardingError.Network.left()
        val access = PredictionRegionAccess(checkGeoblock, createTestingCoroutineDispatcherProvider())
        access.refresh()

        // Act
        access.refresh()

        // Assert
        assertThat(access.status.value).isEqualTo(PredictionRegionStatus.RESTRICTED)
    }

    @Test
    fun `GIVEN an older check answers last WHEN two refreshes overlap THEN the newer answer stays`() = runTest {
        // Arrange
        val olderAnswer = CompletableDeferred<Either<PolymarketOnboardingError, Boolean>>()
        val newerAnswer = CompletableDeferred<Either<PolymarketOnboardingError, Boolean>>()
        coEvery { checkGeoblock() } coAnswers { olderAnswer.await() } coAndThen { newerAnswer.await() }
        val access = PredictionRegionAccess(checkGeoblock, createTestingCoroutineDispatcherProvider())
        launch { access.refresh() }
        advanceUntilIdle()
        launch { access.refresh() }
        advanceUntilIdle()

        // Act
        newerAnswer.complete(true.right())
        advanceUntilIdle()
        olderAnswer.complete(false.right())
        advanceUntilIdle()

        // Assert
        assertThat(access.status.value).isEqualTo(PredictionRegionStatus.RESTRICTED)
    }
}