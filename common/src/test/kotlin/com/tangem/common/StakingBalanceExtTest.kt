package com.tangem.common

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.P2PEthPoolExitQueue
import com.tangem.domain.models.staking.P2PEthPoolExitRequest
import com.tangem.domain.models.staking.P2PEthPoolStake
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalanceItem
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Characterization tests for [StakingBalanceExt] — the provider-agnostic staking-total math.
 *
 * These lock the CURRENT behaviour (they are not a spec) so the upcoming `BalanceContribution`
 * migration can prove parity. The Cardano / non-Cardano branch is driven through the REAL
 * [com.tangem.lib.crypto.BlockchainUtils.isIncludeStakingTotalBalance] via real network ids — it is
 * intentionally NOT mocked, otherwise the branch under test would be bypassed.
 *
 * Cardano => `isIncludeStakingTotalBalance == false` (principal already in the wallet balance).
 * Any other chain (Ethereum here) => `true` (staking principal not yet counted).
 */
internal class StakingBalanceExtTest {

    private val includeChainId = Blockchain.Ethereum.toNetworkId()
    private val cardanoChainId = Blockchain.Cardano.toNetworkId()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTotalWithRewardsStakingBalance {

        @Test
        fun `GIVEN StakeKit on including chain WHEN getTotalWithRewards THEN sums all items`() {
            // Arrange
            val balance = stakeKit(staked = BigDecimal(9), rewards = BigDecimal(1))

            // Act
            val actual = balance.getTotalWithRewardsStakingBalance(includeChainId)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(10))
        }

        @Test
        fun `GIVEN StakeKit on Cardano WHEN getTotalWithRewards THEN returns rewards only`() {
            // Arrange
            val balance = stakeKit(staked = BigDecimal(9), rewards = BigDecimal(1))

            // Act
            val actual = balance.getTotalWithRewardsStakingBalance(cardanoChainId)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(1))
        }

        @Test
        fun `GIVEN P2PEthPool on including chain WHEN getTotalWithRewards THEN sums staked plus unstaking plus withdrawable plus rewards`() {
            // Arrange
            val balance = p2pEthPool(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3)

            // Act
            val actual = balance.getTotalWithRewardsStakingBalance(includeChainId)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(11))
        }

        @Test
        fun `GIVEN P2PEthPool on Cardano WHEN getTotalWithRewards THEN returns rewards only`() {
            // Arrange
            val balance = p2pEthPool(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3)

            // Act
            val actual = balance.getTotalWithRewardsStakingBalance(cardanoChainId)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(3))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetTotalStakingBalance {

        @Test
        fun `GIVEN StakeKit on including chain WHEN getTotalStaking THEN sums non-reward items`() {
            // Arrange
            val balance = stakeKit(staked = BigDecimal(9), rewards = BigDecimal(1))

            // Act
            val actual = balance.getTotalStakingBalance(includeChainId)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(9))
        }

        @Test
        fun `GIVEN StakeKit on Cardano WHEN getTotalStaking THEN returns non-reward sum minus rewards`() {
            // Arrange
            val balance = stakeKit(staked = BigDecimal(9), rewards = BigDecimal(1))

            // Act — pins the current (odd) Cardano branch: (Σ non-reward items) − rewards = 9 − 1 = 8.
            // This test characterizes today's output; it deliberately does NOT judge whether it is correct.
            val actual = balance.getTotalStakingBalance(cardanoChainId)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(8))
        }

        @Test
        fun `GIVEN P2PEthPool WHEN getTotalStaking THEN sums staked plus unstaking plus withdrawable`() {
            // Arrange
            val balance = p2pEthPool(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3)

            // Act — P2PEthPool ignores the chain branch and never adds rewards here.
            val actual = balance.getTotalStakingBalance(includeChainId)

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(8))
        }
    }

    /**
     * The migration gate: `StakingBalance.Data.totalDeltaCryptoAmount()` must return exactly what
     * [getTotalWithRewardsStakingBalance] returns today, for every provider × network combination. The flag is
     * set the way `CryptoCurrencyStatusFactory` stamps it, so both sides are driven by the same real
     * [com.tangem.lib.crypto.BlockchainUtils] decision. Fixtures are real balances (not mocks) — a mocked
     * balance would intercept the member function under test.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ContributionParity {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stamped balance WHEN totalDeltaCryptoAmount THEN equals legacy getTotalWithRewards`(
            model: ParityTestModel,
        ) {
            // Arrange
            val stamped = model.balance.stampFor(model.networkId)

            // Act
            val actual = stamped.totalDeltaCryptoAmount()

            // Assert
            assertThat(actual).isEqualTo(model.balance.getTotalWithRewardsStakingBalance(model.networkId))
        }

        private fun provideTestModels() = listOf(
            ParityTestModel(
                description = "StakeKit on including chain",
                balance = realStakeKit(
                    balanceItem(amount = BigDecimal(9), type = BalanceType.STAKED),
                    balanceItem(amount = BigDecimal(1), type = BalanceType.REWARDS),
                ),
                networkId = includeChainId,
            ),
            ParityTestModel(
                description = "StakeKit on Cardano",
                balance = realStakeKit(
                    balanceItem(amount = BigDecimal(9), type = BalanceType.STAKED),
                    balanceItem(amount = BigDecimal(1), type = BalanceType.REWARDS),
                ),
                networkId = cardanoChainId,
            ),
            ParityTestModel(
                description = "StakeKit with types outside the four totals, on including chain",
                balance = realStakeKit(
                    balanceItem(amount = BigDecimal(1), type = BalanceType.AVAILABLE),
                    balanceItem(amount = BigDecimal(2), type = BalanceType.STAKED),
                    balanceItem(amount = BigDecimal(4), type = BalanceType.PREPARING),
                    balanceItem(amount = BigDecimal(8), type = BalanceType.LOCKED),
                    balanceItem(amount = BigDecimal(16), type = BalanceType.UNKNOWN),
                ),
                networkId = includeChainId,
            ),
            ParityTestModel(
                description = "StakeKit without rewards, on Cardano",
                balance = realStakeKit(balanceItem(amount = BigDecimal(9), type = BalanceType.STAKED)),
                networkId = cardanoChainId,
            ),
            ParityTestModel(
                description = "P2PEthPool on including chain",
                balance = realP2PEthPool(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3),
                networkId = includeChainId,
            ),
            ParityTestModel(
                description = "P2PEthPool on Cardano",
                balance = realP2PEthPool(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3),
                networkId = cardanoChainId,
            ),
        )

        /** Mirrors what `CryptoCurrencyStatusFactory` does when it attaches a balance to a status. */
        private fun StakingBalance.Data.stampFor(networkId: String): StakingBalance.Data {
            val isStakedIncludedInNetworkBalance = !BlockchainUtils.isIncludeStakingTotalBalance(networkId)

            return when (this) {
                is StakingBalance.Data.StakeKit -> copy(
                    isStakedIncludedInNetworkBalance = isStakedIncludedInNetworkBalance,
                )
                is StakingBalance.Data.P2PEthPool -> copy(
                    isStakedIncludedInNetworkBalance = isStakedIncludedInNetworkBalance,
                )
            }
        }
    }

    internal data class ParityTestModel(
        val description: String,
        val balance: StakingBalance.Data,
        val networkId: String,
    ) {

        override fun toString(): String = description
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetRewardStakingBalance {

        @Test
        fun `GIVEN StakeKit with mixed types WHEN getReward THEN sums only REWARDS items`() {
            // Arrange
            val balance = StakingBalance.Data.StakeKit(
                stakingId = mockk(relaxed = true),
                source = StatusSource.ACTUAL,
                balance = yieldBalance(
                    balanceItem(amount = BigDecimal(9), type = BalanceType.STAKED),
                    balanceItem(amount = BigDecimal(1), type = BalanceType.REWARDS),
                    balanceItem(amount = BigDecimal(2), type = BalanceType.REWARDS),
                ),
            )

            // Act
            val actual = balance.getRewardStakingBalance()

            // Assert
            assertThat(actual).isEqualTo(BigDecimal(3))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetValidatorsCount {

        @Test
        fun `GIVEN items with blank and duplicate validators WHEN getValidatorsCount THEN counts distinct non-blank addresses`() {
            // Arrange
            val balance = StakingBalance.Data.StakeKit(
                stakingId = mockk(relaxed = true),
                source = StatusSource.ACTUAL,
                balance = yieldBalance(
                    balanceItem(amount = BigDecimal.ONE, type = BalanceType.STAKED, validatorAddress = "A"),
                    balanceItem(amount = BigDecimal.ONE, type = BalanceType.STAKED, validatorAddress = "A"),
                    balanceItem(amount = BigDecimal.ONE, type = BalanceType.STAKED, validatorAddress = "B"),
                    balanceItem(amount = BigDecimal.ONE, type = BalanceType.STAKED, validatorAddress = null),
                    balanceItem(amount = BigDecimal.ONE, type = BalanceType.STAKED, validatorAddress = ""),
                ),
            )

            // Act
            val actual = balance.getValidatorsCount()

            // Assert
            assertThat(actual).isEqualTo(2)
        }
    }

    // region Fixtures
    private fun stakeKit(staked: BigDecimal, rewards: BigDecimal): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = mockk(relaxed = true),
            source = StatusSource.ACTUAL,
            balance = yieldBalance(
                balanceItem(amount = staked, type = BalanceType.STAKED),
                balanceItem(amount = rewards, type = BalanceType.REWARDS),
            ),
        )
    }

    /** P2PEthPool derives its four totals from accounts, so the derived values are stubbed directly. */
    private fun p2pEthPool(staked: Int, unstaking: Int, withdrawable: Int, rewards: Int): StakingBalance.Data.P2PEthPool {
        return mockk(relaxed = true) {
            every { totalStaked } returns BigDecimal(staked)
            every { unstakingAmount } returns BigDecimal(unstaking)
            every { withdrawableAmount } returns BigDecimal(withdrawable)
            every { totalRewards } returns BigDecimal(rewards)
        }
    }

    private fun yieldBalance(vararg items: BalanceItem): YieldBalanceItem {
        return YieldBalanceItem(items = items.toList(), integrationId = "")
    }

    private fun realStakeKit(vararg items: BalanceItem): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = StakingID(integrationId = "integration", address = "0x1"),
            source = StatusSource.ACTUAL,
            balance = yieldBalance(items = items),
        )
    }

    /** Unstaking / withdrawable are derived from the exit queue by claimability, so they go in as requests. */
    private fun realP2PEthPool(
        staked: Int,
        unstaking: Int,
        withdrawable: Int,
        rewards: Int,
    ): StakingBalance.Data.P2PEthPool {
        return StakingBalance.Data.P2PEthPool(
            stakingId = StakingID(integrationId = "integration", address = "0x1"),
            source = StatusSource.ACTUAL,
            accounts = listOf(
                P2PEthPoolStakingAccount(
                    delegatorAddress = "0xdelegator",
                    vaultAddress = "0xvault",
                    stake = P2PEthPoolStake(
                        assets = BigDecimal(staked),
                        totalEarnedAssets = BigDecimal(rewards),
                    ),
                    availableToUnstake = BigDecimal.ZERO,
                    availableToWithdraw = BigDecimal.ZERO,
                    exitQueue = P2PEthPoolExitQueue(
                        total = BigDecimal(unstaking + withdrawable),
                        requests = listOf(
                            exitRequest(totalAssets = unstaking, isClaimable = false),
                            exitRequest(totalAssets = withdrawable, isClaimable = true),
                        ),
                    ),
                ),
            ),
        )
    }

    private fun exitRequest(totalAssets: Int, isClaimable: Boolean): P2PEthPoolExitRequest {
        return P2PEthPoolExitRequest(
            ticket = "ticket-$totalAssets-$isClaimable",
            totalAssets = BigDecimal(totalAssets),
            timestamp = Instant.fromEpochSeconds(epochSeconds = 0),
            withdrawalTimestamp = null,
            isClaimable = isClaimable,
        )
    }

    private fun balanceItem(
        amount: BigDecimal,
        type: BalanceType,
        validatorAddress: String? = null,
    ): BalanceItem {
        return mockk(relaxed = true) {
            every { this@mockk.amount } returns amount
            every { this@mockk.type } returns type
            every { this@mockk.validatorAddress } returns validatorAddress
        }
    }
    // endregion
}