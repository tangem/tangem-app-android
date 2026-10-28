package com.tangem.domain.staking.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.StakingOption
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakingAvailabilityListUseCaseTest {

    private val stakingRepository: StakingRepository = mockk()

    private val useCase = StakingAvailabilityListUseCase(stakingRepository = stakingRepository)

    private val userWalletId = UserWalletId(stringValue = "011")
    private val factory = MockCryptoCurrencyFactory()
    private val solana = factory.createCoin(Blockchain.Solana)
    private val bitcoin = factory.createCoin(Blockchain.Bitcoin)
    private val available = StakingAvailability.Available(
        StakingOption.StakeKit(StakingIntegrationID.StakeKit.Coin.Solana, MockStaking.solanaYield),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakingRepository)
    }

    @Test
    fun `GIVEN two currencies WHEN invokeSync THEN availability per currency`() = runTest {
        // Arrange
        coEvery { stakingRepository.getStakingAvailabilitySync(userWalletId, solana) } returns available
        coEvery { stakingRepository.getStakingAvailabilitySync(userWalletId, bitcoin) } returns
            StakingAvailability.Unavailable

        // Act
        val actual = useCase.invokeSync(userWalletId, listOf(solana, bitcoin))

        // Assert
        assertThat(actual).isEqualTo(
            mapOf<CryptoCurrency, StakingAvailability>(
                solana to available,
                bitcoin to StakingAvailability.Unavailable,
            ),
        )
    }

    // Pins current behaviour: unlike other staking use cases, exceptions are not caught
    @Test
    fun `GIVEN repository throws WHEN invokeSync THEN exception propagates`() = runTest {
        // Arrange
        coEvery { stakingRepository.getStakingAvailabilitySync(userWalletId, solana) } throws
            IllegalStateException("boom")

        // Act
        val error = runCatching { useCase.invokeSync(userWalletId, listOf(solana)) }.exceptionOrNull()

        // Assert
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
        assertThat(error).hasMessageThat().isEqualTo("boom")
    }
}