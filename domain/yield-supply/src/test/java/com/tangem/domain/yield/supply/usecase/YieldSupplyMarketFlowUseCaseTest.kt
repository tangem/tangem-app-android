package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.yield.MockYieldSupply.TOKEN_ADDRESS
import com.tangem.test.mock.yield.MockYieldSupply.market
import com.tangem.test.mock.yield.MockYieldSupply.token
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class YieldSupplyMarketFlowUseCaseTest {

    private val repository: YieldSupplyRepository = mockk()

    private val useCase = YieldSupplyMarketFlowUseCase(yieldSupplyRepository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN markets hold the token WHEN collected THEN its entry is emitted`() = runTest {
        // Arrange
        val market = market()
        every { repository.getMarketsFlow() } returns
            flowOf(listOf(market(backendId = OTHER_BACKEND_ID), market))

        // Act
        val actual = getEmittedValues(useCase(token()))

        // Assert
        assertThat(actual).containsExactly(market)
    }

    /** The backend lowercases what it is sent, while the token list carries the checksummed address. */
    @Test
    fun `GIVEN the market address differs in case WHEN collected THEN the entry still matches`() = runTest {
        // Arrange
        val market = market().copy(tokenAddress = TOKEN_ADDRESS.lowercase())
        every { repository.getMarketsFlow() } returns flowOf(listOf(market))

        // Act
        val actual = getEmittedValues(useCase(token()))

        // Assert
        assertThat(actual).containsExactly(market)
    }

    @Test
    fun `GIVEN markets cache is empty WHEN collected THEN null is emitted`() = runTest {
        // Arrange
        every { repository.getMarketsFlow() } returns flowOf(emptyList())

        // Act
        val actual = getEmittedValues(useCase(token()))

        // Assert
        assertThat(actual).containsExactly(null)
    }

    @Test
    fun `GIVEN cache warms up after being empty WHEN collected THEN the entry follows`() = runTest {
        // Arrange
        val market = market()
        every { repository.getMarketsFlow() } returns flowOf(emptyList(), listOf(market))

        // Act
        val actual = getEmittedValues(useCase(token()))

        // Assert
        assertThat(actual).containsExactly(null, market).inOrder()
    }

    @Test
    fun `GIVEN markets re-emit the same entry WHEN collected THEN the duplicate is dropped`() = runTest {
        // Arrange
        val market = market()
        every { repository.getMarketsFlow() } returns flowOf(listOf(market), listOf(market))

        // Act
        val actual = getEmittedValues(useCase(token()))

        // Assert
        assertThat(actual).containsExactly(market)
    }

    private companion object {
        const val OTHER_BACKEND_ID = "polygon"
    }
}