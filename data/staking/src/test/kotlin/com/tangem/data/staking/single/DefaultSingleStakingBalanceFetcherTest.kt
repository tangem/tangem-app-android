package com.tangem.data.staking.single

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.staking.single.SingleStakingBalanceFetcher
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSingleStakingBalanceFetcherTest {

    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher = mockk()

    private val fetcher = DefaultSingleStakingBalanceFetcher(
        multiStakingBalanceFetcher = multiStakingBalanceFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiStakingBalanceFetcher)
    }

    @Test
    fun `GIVEN multi fetcher returns Right WHEN invoke THEN delegates with single id set and returns Right`() = runTest {
        // Arrange
        val params = SingleStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingId = tonId)

        val multiParams = MultiStakingBalanceFetcher.Params(
            userWalletId = userWalletId,
            stakingIds = setOf(tonId),
        )

        val multiResult = Unit.right()

        coEvery { multiStakingBalanceFetcher(params = multiParams) } returns multiResult

        // Act
        val actual = fetcher.invoke(params).isRight()

        // Assert
        Truth.assertThat(actual).isTrue()

        coVerify { multiStakingBalanceFetcher(params = multiParams) }
    }

    @Test
    fun `GIVEN multi fetcher returns Left WHEN invoke THEN returns the same Left`() = runTest {
        // Arrange
        val params = SingleStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingId = tonId)

        val multiParams = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = setOf(tonId))

        val multiResult = IllegalStateException().left()

        coEvery { multiStakingBalanceFetcher(params = multiParams) } returns multiResult

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        Truth.assertThat(actual).isEqualTo(multiResult)
        coVerify { multiStakingBalanceFetcher(params = multiParams) }
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
    }
}