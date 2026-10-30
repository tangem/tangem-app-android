package com.tangem.domain.staking

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetStakingTransactionsUseCaseTest {

    private val stakeKitRepository: StakeKitRepository = mockk()
    private val stakingErrorResolver: StakingErrorResolver = mockk()

    private val useCase = GetStakingTransactionsUseCase(
        stakeKitRepository = stakeKitRepository,
        stakingErrorResolver = stakingErrorResolver,
    )

    private val userWalletId = UserWalletId(stringValue = "011")
    private val network = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana).network
    private val params = ActionParams(
        actionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
        integrationId = MockStaking.SOLANA_ID,
        amount = BigDecimal.ONE,
        address = MockStaking.DELEGATOR_ADDRESS,
        validatorAddress = MockStaking.VALIDATOR_ADDRESS,
        token = MockStaking.solToken,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitRepository, stakingErrorResolver)
        every { stakingErrorResolver.resolve(any()) } returns RESOLVED_ERROR
    }

    private fun transaction(id: String = "tx-1") = StakingTransaction(
        id = id,
        network = NetworkType.SOLANA,
        status = StakingTransactionStatus.CREATED,
        type = StakingTransactionType.STAKE,
        hash = null,
        signedTransaction = null,
        unsignedTransaction = "00",
        stepIndex = 0,
        error = null,
        gasEstimate = null,
        stakeId = null,
        explorerUrl = null,
        ledgerHwAppId = null,
        isMessage = false,
    )

    @Test
    fun `GIVEN action with transactions WHEN invoke THEN transactions returned after 1000 ms`() = runTest {
        // Arrange
        coEvery { stakeKitRepository.createAction(userWalletId, network, params) } returns
            MockStaking.stakingAction(transactions = listOf(transaction()))

        // Act
        val actual = useCase(userWalletId, network, params)

        // Assert
        assertThat(actual).isEqualTo(listOf(transaction()).right())
        assertThat(testScheduler.currentTime).isEqualTo(1000L)
    }

    @Test
    fun `GIVEN action without transactions WHEN invoke THEN resolved error returned`() = runTest {
        // Arrange
        coEvery { stakeKitRepository.createAction(userWalletId, network, params) } returns
            MockStaking.stakingAction(transactions = null)

        // Act
        val actual = useCase(userWalletId, network, params)

        // Assert
        assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
        verify(exactly = 1) {
            stakingErrorResolver.resolve(
                match { it is IllegalStateException && it.message == "No available transaction to patch" },
            )
        }
        assertThat(testScheduler.currentTime).isEqualTo(1000L)
    }

    @Test
    fun `GIVEN createAction throws WHEN invoke THEN resolved error without delay`() = runTest {
        // Arrange
        coEvery { stakeKitRepository.createAction(userWalletId, network, params) } throws
            IllegalStateException("boom")

        // Act
        val actual = useCase(userWalletId, network, params)

        // Assert
        assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
        verify(exactly = 1) {
            stakingErrorResolver.resolve(match { it is IllegalStateException && it.message == "boom" })
        }
        assertThat(testScheduler.currentTime).isEqualTo(0L)
    }

    private companion object {
        val RESOLVED_ERROR = StakingError.DomainError("resolved")
    }
}