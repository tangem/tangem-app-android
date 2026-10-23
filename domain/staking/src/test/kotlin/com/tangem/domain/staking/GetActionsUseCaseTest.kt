package com.tangem.domain.staking

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.StakeKitActionRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetActionsUseCaseTest {

    private val stakeKitActionRepository: StakeKitActionRepository = mockk()
    private val stakingErrorResolver: StakingErrorResolver = mockk()

    private val useCase = GetActionsUseCase(
        stakeKitActionRepository = stakeKitActionRepository,
        stakingErrorResolver = stakingErrorResolver,
    )

    private val userWalletId = UserWalletId(stringValue = "011")
    private val solana = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitActionRepository, stakingErrorResolver)
        every { stakingErrorResolver.resolve(any()) } returns RESOLVED_ERROR
    }

    @Test
    fun `GIVEN repository flow throws WHEN invoke THEN resolved Left emitted`() = runTest {
        // Arrange
        every { stakeKitActionRepository.get(userWalletId, solana.id) } returns flow {
            throw IllegalStateException("boom")
        }

        // Act
        val actual = getEmittedValues(useCase(userWalletId, solana.id))
        advanceUntilIdle()

        // Assert
        assertThat(actual).containsExactly(RESOLVED_ERROR.left())
    }

    private companion object {
        val RESOLVED_ERROR = StakingError.DomainError("resolved")
    }
}