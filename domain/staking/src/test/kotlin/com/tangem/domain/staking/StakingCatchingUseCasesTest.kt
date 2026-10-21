package com.tangem.domain.staking

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.SubmitHashData
import com.tangem.domain.staking.model.UnsubmittedTransactionMetadata
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.repositories.StakeKitRepository
import com.tangem.domain.staking.repositories.StakeKitTransactionHashRepository
import com.tangem.domain.staking.repositories.StakingErrorResolver
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.mock.staking.MockStaking
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakingCatchingUseCasesTest {

    private val stakeKitRepository: StakeKitRepository = mockk()
    private val stakingRepository: StakingRepository = mockk()
    private val hashRepository: StakeKitTransactionHashRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val stakingErrorResolver: StakingErrorResolver = mockk()

    private val userWalletId = UserWalletId(stringValue = "011")
    private val solana = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)
    private val network = solana.network
    private val params = ActionParams(
        actionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
        integrationId = MockStaking.SOLANA_ID,
        amount = BigDecimal.ONE,
        address = MockStaking.DELEGATOR_ADDRESS,
        validatorAddress = MockStaking.VALIDATOR_ADDRESS,
        token = MockStaking.solToken,
    )
    private val fee = Fee.Common(Amount(currencySymbol = "SOL", value = BigDecimal("0.001"), decimals = 9))
    private val amount = Amount(currencySymbol = "SOL", value = BigDecimal.ONE, decimals = 9)

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitRepository, stakingRepository, hashRepository, walletManagersFacade, stakingErrorResolver)
        every { stakingErrorResolver.resolve(any()) } returns RESOLVED_ERROR
    }

    private fun verifyBoomResolved() {
        verify(exactly = 1) {
            stakingErrorResolver.resolve(match { it is IllegalStateException && it.message == "boom" })
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EstimateGas {

        private val useCase = EstimateGasUseCase(stakeKitRepository, stakingErrorResolver)

        @Test
        fun `GIVEN repository throws WHEN invoke THEN resolved error returned`() = runTest {
            // Arrange
            coEvery { stakeKitRepository.estimateGas(userWalletId, network, params) } throws
                IllegalStateException("boom")

            // Act
            val actual = useCase(userWalletId, network, params)

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetConstructedStakingTransaction {

        private val useCase = GetConstructedStakingTransactionUseCase(stakeKitRepository, stakingErrorResolver)

        @Test
        fun `GIVEN repository throws WHEN invoke THEN resolved error returned`() = runTest {
            // Arrange
            coEvery {
                stakeKitRepository.constructTransaction(Network.RawID("solana"), fee, amount, "tx-1")
            } throws IllegalStateException("boom")

            // Act
            val actual = useCase(Network.RawID("solana"), fee, amount, "tx-1")

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetYieldByCurrency {

        private val useCase = GetYieldUseCase(stakeKitRepository, stakingErrorResolver)

        @Test
        fun `GIVEN repository throws WHEN invoke by currency THEN resolved error returned`() = runTest {
            // Arrange
            coEvery { stakeKitRepository.getYield(solana.id, "SOL") } throws IllegalStateException("boom")

            // Act
            val actual = useCase(solana.id, "SOL")

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetYieldById {

        private val useCase = GetYieldUseCase(stakeKitRepository, stakingErrorResolver)

        @Test
        fun `GIVEN repository throws WHEN invoke by id THEN resolved error returned`() = runTest {
            // Arrange
            coEvery { stakeKitRepository.getYield(MockStaking.SOLANA_ID) } throws IllegalStateException("boom")

            // Act
            val actual = useCase(MockStaking.SOLANA_ID)

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsAnyTokenStaked {

        private val useCase = IsAnyTokenStakedUseCase(stakingRepository, stakingErrorResolver)

        @Test
        fun `GIVEN repository throws WHEN invoke THEN resolved error returned`() = runTest {
            // Arrange
            coEvery { stakingRepository.isAnyTokenStaked(userWalletId) } throws IllegalStateException("boom")

            // Act
            val actual = useCase(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SaveUnsubmittedHash {

        private val useCase = SaveUnsubmittedHashUseCase(hashRepository, stakingErrorResolver)

        @Test
        fun `GIVEN hash WHEN invoke THEN metadata stored`() = runTest {
            // Arrange
            coEvery { hashRepository.storeUnsubmittedHash(any()) } just Runs

            // Act
            val actual = useCase(transactionId = "tx-1", transactionHash = "0xhash")

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify {
                hashRepository.storeUnsubmittedHash(
                    UnsubmittedTransactionMetadata(transactionHash = "0xhash", transactionId = "tx-1"),
                )
            }
        }

        @Test
        fun `GIVEN store throws WHEN invoke THEN resolved error returned`() = runTest {
            // Arrange
            coEvery { hashRepository.storeUnsubmittedHash(any()) } throws IllegalStateException("boom")

            // Act
            val actual = useCase(transactionId = "tx-1", transactionHash = "0xhash")

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendUnsubmittedHashes {

        private val useCase = SendUnsubmittedHashesUseCase(hashRepository, stakingErrorResolver)

        @Test
        fun `GIVEN repository throws WHEN invoke THEN resolved error returned`() = runTest {
            // Arrange
            coEvery { hashRepository.sendUnsubmittedHashes() } throws IllegalStateException("boom")

            // Act
            val actual = useCase()

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SubmitHash {

        private val useCase = SubmitHashUseCase(hashRepository, stakingErrorResolver)

        @Test
        fun `GIVEN submit data WHEN invoke THEN id and hash passed in right order`() = runTest {
            // Arrange
            coEvery { hashRepository.submitHash(any(), any()) } just Runs

            // Act
            val actual = useCase(SubmitHashData(transactionHash = "0xhash", transactionId = "tx-1"))

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify { hashRepository.submitHash(transactionId = "tx-1", transactionHash = "0xhash") }
        }

        @Test
        fun `GIVEN repository throws WHEN invoke THEN resolved error returned`() = runTest {
            // Arrange
            coEvery { hashRepository.submitHash(any(), any()) } throws IllegalStateException("boom")

            // Act
            val actual = useCase(SubmitHashData(transactionHash = "0xhash", transactionId = "tx-1"))

            // Assert
            assertThat(actual).isEqualTo(RESOLVED_ERROR.left())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchStakingTokens {

        private val useCase = FetchStakingTokensUseCase(stakeKitRepository, stakingErrorResolver)

        // Pins current behaviour: Raise.catch returns the resolved error as a value, it is never raised
        @Test
        fun `GIVEN fetch throws WHEN invoke THEN error resolved but Right returned`() = runTest {
            // Arrange
            coEvery { stakeKitRepository.fetchYields() } throws IllegalStateException("boom")

            // Act
            val actual = useCase()

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            verifyBoomResolved()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CheckAccountInitialized {

        private val useCase = CheckAccountInitializedUseCase(walletManagersFacade)

        @Test
        fun `GIVEN facade throws WHEN invoke THEN throwable returned as Left`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.isAccountInitialized(userWalletId, network) } throws
                IllegalStateException("boom")

            // Act
            val actual = useCase(userWalletId, network)

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(IllegalStateException::class.java)
            assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo("boom")
            verify(exactly = 0) { stakingErrorResolver.resolve(any()) }
        }
    }

    private companion object {
        val RESOLVED_ERROR = StakingError.DomainError("resolved")
    }
}