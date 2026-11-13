package com.tangem.features.polymarket.impl.common.region

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketGeoblock
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PredictionRegionAccessTest {

    private val checkGeoblock: CheckPolymarketGeoblockUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(checkGeoblock)
    }

    @Test
    fun `GIVEN a blocked region WHEN refreshed THEN the status is restricted`() = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns PolymarketGeoblock.BLOCKED.right()
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

    @ParameterizedTest
    @EnumSource(PolymarketGeoblock::class, names = ["UNRECOGNIZED_RESPONSE", "NO_RESPONSE"])
    fun `GIVEN an inconclusive geoblock WHEN refreshed THEN the status is allowed`(
        geoblock: PolymarketGeoblock,
    ) = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns geoblock.right()
        val access = PredictionRegionAccess(checkGeoblock, createTestingCoroutineDispatcherProvider())

        // Act
        access.refresh()

        // Assert
        assertThat(access.status.value).isEqualTo(PredictionRegionStatus.ALLOWED)
    }

    @ParameterizedTest
    @EnumSource(PolymarketGeoblock::class, names = ["UNRECOGNIZED_RESPONSE", "NO_RESPONSE"])
    fun `GIVEN a restricted region WHEN a later geoblock is inconclusive THEN the region stays restricted`(
        geoblock: PolymarketGeoblock,
    ) = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns PolymarketGeoblock.BLOCKED.right() andThen geoblock.right()
        val access = PredictionRegionAccess(checkGeoblock, createTestingCoroutineDispatcherProvider())
        access.refresh()

        // Act
        access.refresh()

        // Assert
        assertThat(access.status.value).isEqualTo(PredictionRegionStatus.RESTRICTED)
    }

    @Test
    fun `GIVEN a restricted region WHEN a later check fails THEN the region stays restricted`() = runTest {
        // Arrange
        coEvery { checkGeoblock() } returns PolymarketGeoblock.BLOCKED.right() andThen PolymarketOnboardingError.Network.left()
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
        val olderAnswer = CompletableDeferred<Either<PolymarketOnboardingError, PolymarketGeoblock>>()
        val newerAnswer = CompletableDeferred<Either<PolymarketOnboardingError, PolymarketGeoblock>>()
        coEvery { checkGeoblock() } coAnswers { olderAnswer.await() } coAndThen { newerAnswer.await() }
        val access = PredictionRegionAccess(checkGeoblock, createTestingCoroutineDispatcherProvider())
        launch { access.refresh() }
        advanceUntilIdle()
        launch { access.refresh() }
        advanceUntilIdle()

        // Act
        newerAnswer.complete(PolymarketGeoblock.BLOCKED.right())
        advanceUntilIdle()
        olderAnswer.complete(PolymarketGeoblock.ALLOWED.right())
        advanceUntilIdle()

        // Assert
        assertThat(access.status.value).isEqualTo(PredictionRegionStatus.RESTRICTED)
    }
}