package com.tangem.features.allservices.impl.entries.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.earn.usecase.GetBestEarnOfferUseCase
import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnType
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EarnOfferEntryModelTest {

    private val getBestEarnOfferUseCase: GetBestEarnOfferUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(getBestEarnOfferUseCase)
    }

    @Test
    fun `GIVEN a best offer WHEN model created THEN exposes its apy as percent`() = runTest {
        // Arrange
        coEvery { getBestEarnOfferUseCase(EarnType.STAKING) } returns Either.Right(earnToken(apy = "0.24"))

        // Act
        val model = createModel(type = EarnType.STAKING, testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.bestApy.value).isEqualTo("24.00%")
        coVerify(exactly = 1) { getBestEarnOfferUseCase(EarnType.STAKING) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no offer WHEN model created THEN apy stays null`() = runTest {
        // Arrange
        coEvery { getBestEarnOfferUseCase(EarnType.YIELD) } returns Either.Right(null)

        // Act
        val model = createModel(type = EarnType.YIELD, testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.bestApy.value).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN request fails WHEN model created THEN apy stays null`() = runTest {
        // Arrange
        coEvery { getBestEarnOfferUseCase(EarnType.YIELD) } returns Either.Left(IllegalStateException())

        // Act
        val model = createModel(type = EarnType.YIELD, testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.bestApy.value).isNull()
        model.onDestroy()
    }

    private fun createModel(type: EarnType, testScope: TestScope) = EarnOfferEntryModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(value = type),
        getBestEarnOfferUseCase = getBestEarnOfferUseCase,
    )

    private fun earnToken(apy: String) = EarnToken(
        apy = apy,
        networkId = "ethereum",
        rewardType = EarnRewardType.APY,
        type = EarnType.STAKING,
        tokenId = "eth",
        tokenSymbol = "ETH",
        tokenName = "ETH",
        tokenAddress = null,
        decimalCount = 18,
    )
}