package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CheckPolymarketGeoblockUseCaseTest {

    private val polymarketRepository: PolymarketRepository = mockk()

    private val useCase = CheckPolymarketGeoblockUseCase(polymarketRepository = polymarketRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketRepository)
    }

    @Test
    fun `GIVEN region is blocked WHEN invoke THEN returns true`() = runTest {
        // Arrange
        coEvery { polymarketRepository.checkGeoblock() } returns true.right()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(true.right())
    }

    @Test
    fun `GIVEN region is not blocked WHEN invoke THEN returns false`() = runTest {
        // Arrange
        coEvery { polymarketRepository.checkGeoblock() } returns false.right()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(false.right())
    }

    @Test
    fun `GIVEN no connection WHEN invoke THEN returns Network`() = runTest {
        // Arrange
        coEvery { polymarketRepository.checkGeoblock() } returns DataError.NetworkError.NoInternetConnection.left()

        // Act
        val actual = useCase()

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network.left())
    }
}