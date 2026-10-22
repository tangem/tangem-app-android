package com.tangem.domain.staking

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetStakingAvailabilityUseCaseTest {

    private val stakingRepository: StakingRepository = mockk()
    private val stakingErrorResolver: StakingErrorResolver = mockk()

    private val useCase = GetStakingAvailabilityUseCase(
        stakingRepository = stakingRepository,
        stakingErrorResolver = stakingErrorResolver,
    )

    private val userWalletId = UserWalletId(stringValue = "011")
    private val solana = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)
    private val available = StakingAvailability.Available(
        StakingOption.StakeKit(StakingIntegrationID.StakeKit.Coin.Solana, MockStaking.solanaYield),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakingRepository, stakingErrorResolver)
        every { stakingErrorResolver.resolve(any()) } returns RESOLVED_ERROR
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Invoke {

        @Test
        fun `GIVEN repository emits availabilities WHEN invoke THEN each wrapped in Right`() = runTest {
            // Arrange
            every { stakingRepository.getStakingAvailability(userWalletId, solana) } returns
                flowOf(available, StakingAvailability.TemporaryUnavailable)

            // Act
            val actual = getEmittedValues(useCase(userWalletId, solana))
            advanceUntilIdle()

            // Assert
            assertThat(actual).containsExactly(
                available.right(),
                StakingAvailability.TemporaryUnavailable.right(),
            ).inOrder()
        }

        @Test
        fun `GIVEN repository flow throws after value WHEN invoke THEN value then resolved Left`() = runTest {
            // Arrange
            every { stakingRepository.getStakingAvailability(userWalletId, solana) } returns flow {
                emit(available)
                throw IllegalStateException("boom")
            }

            // Act
            val actual = getEmittedValues(useCase(userWalletId, solana))
            advanceUntilIdle()

            // Assert
            assertThat(actual).containsExactly(available.right(), RESOLVED_ERROR.left()).inOrder()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeSync {

        @Test
        fun `GIVEN repository throws WHEN invokeSync THEN resolved Left`() = runTest {
            // Arrange
            coEvery { stakingRepository.getStakingAvailabilitySync(userWalletId, solana) } throws
                IllegalStateException("boom")

            // Act
            val actual = useCase.invokeSync(userWalletId, solana)

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
        }
    }

    private companion object {
        val RESOLVED_ERROR = StakingError.DomainError("resolved")
    }
}