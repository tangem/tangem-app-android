package com.tangem.domain.yield.supply.usecase

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.incentives.YieldIncentivesRepository
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetYieldAprHistoryUseCaseTest {

    private val repository: YieldIncentivesRepository = mockk()

    private val useCase = GetYieldAprHistoryUseCase(yieldIncentivesRepository = repository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: DaysModel) = runTest {
        // Arrange
        val history = YieldAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, points = emptyList())
        coEvery {
            repository.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = model.passedOn)
        } returns history.right()

        // Act
        val actual = useCase(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = model.requested)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(history)
        coVerify(exactly = 1) {
            repository.getAprHistory(chainId = CHAIN_ID, tokenAddress = TOKEN_ADDRESS, days = model.passedOn)
        }
    }

    /** @property passedOn what the repository must receive once the contract bounds are applied */
    internal data class DaysModel(val requested: Int, val passedOn: Int)

    private fun provideTestModels() = listOf(
        DaysModel(requested = 7, passedOn = 7),
        DaysModel(requested = 90, passedOn = 90),
        DaysModel(requested = 180, passedOn = 90),
        DaysModel(requested = 0, passedOn = 1),
        DaysModel(requested = -5, passedOn = 1),
    )

    private companion object {
        const val CHAIN_ID = 1
        const val TOKEN_ADDRESS = "0xabc"
    }
}