package com.tangem.domain.models.staking

import com.google.common.truth.Truth
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Tests [StakingBalance.Data] as a [BalanceContribution] — the scalar "what do you add to the total?" contract.
 *
 * The `isStakedIncludedInNetworkBalance` flag is the pre-resolved per-network rule (Cardano stakes are already
 * inside the network balance, so only rewards may be added on top). Parity with the legacy
 * `getTotalWithRewardsStakingBalance` math is asserted in `:common` — `StakingBalanceExtTest` — which is the
 * module that can see both this contract and `BlockchainUtils`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakingBalanceContributionTest {

    @ParameterizedTest
    @ProvideTestModels
    fun totalDeltaCryptoAmount(model: DeltaTestModel) {
        // Act
        val actual = model.balance.totalDeltaCryptoAmount()

        // Assert
        Truth.assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN any staking data WHEN kind THEN it is the staking kind owned by this module`() {
        // Arrange
        val balance = stakeKit(items = listOf(balanceItem(amount = 1, type = BalanceType.STAKED)))

        // Act
        val actual = balance.kind

        // Assert
        Truth.assertThat(actual).isEqualTo(StakingBalance.CONTRIBUTION_KIND)
    }

    @Test
    fun `GIVEN cached staking data WHEN source THEN the balance source is exposed as the contribution source`() {
        // Arrange
        val balance = stakeKit(
            items = listOf(balanceItem(amount = 1, type = BalanceType.STAKED)),
            source = StatusSource.ONLY_CACHE,
        )

        // Act
        val actual: BalanceContribution = balance

        // Assert
        Truth.assertThat(actual.source).isEqualTo(StatusSource.ONLY_CACHE)
    }

    @Test
    fun `GIVEN empty and error balances WHEN checked THEN they are not contributions`() {
        // Arrange
        val stakingId = StakingID(integrationId = "integration", address = "0x1")
        val empty = StakingBalance.Empty(stakingId = stakingId, source = StatusSource.ACTUAL)
        val error = StakingBalance.Error(stakingId = stakingId)

        // Assert — only non-empty balances can move a total
        Truth.assertThat(empty).isNotInstanceOf(BalanceContribution::class.java)
        Truth.assertThat(error).isNotInstanceOf(BalanceContribution::class.java)
    }

    private fun provideTestModels() = listOf(
        DeltaTestModel(
            description = "StakeKit, stake not in network balance -> every item counts",
            balance = stakeKit(
                items = listOf(
                    balanceItem(amount = 9, type = BalanceType.STAKED),
                    balanceItem(amount = 1, type = BalanceType.REWARDS),
                ),
            ),
            expected = BigDecimal(10),
        ),
        DeltaTestModel(
            description = "StakeKit, stake already in network balance -> rewards only",
            balance = stakeKit(
                items = listOf(
                    balanceItem(amount = 9, type = BalanceType.STAKED),
                    balanceItem(amount = 1, type = BalanceType.REWARDS),
                ),
                isStakedIncludedInNetworkBalance = true,
            ),
            expected = BigDecimal(1),
        ),
        DeltaTestModel(
            description = "StakeKit with types outside the four totals -> they still count",
            balance = stakeKit(
                items = listOf(
                    balanceItem(amount = 1, type = BalanceType.AVAILABLE),
                    balanceItem(amount = 2, type = BalanceType.STAKED),
                    balanceItem(amount = 4, type = BalanceType.PREPARING),
                    balanceItem(amount = 8, type = BalanceType.LOCKED),
                    balanceItem(amount = 16, type = BalanceType.UNKNOWN),
                ),
            ),
            expected = BigDecimal(31),
        ),
        DeltaTestModel(
            description = "StakeKit with no rewards, stake already in network balance -> zero",
            balance = stakeKit(
                items = listOf(balanceItem(amount = 9, type = BalanceType.STAKED)),
                isStakedIncludedInNetworkBalance = true,
            ),
            expected = BigDecimal.ZERO,
        ),
        DeltaTestModel(
            description = "P2PEthPool, stake not in network balance -> staked + unstaking + withdrawable + rewards",
            balance = p2pEthPool(staked = 5, unstaking = 2, withdrawable = 1, rewards = 3),
            expected = BigDecimal(11),
        ),
        DeltaTestModel(
            description = "P2PEthPool, stake already in network balance -> rewards only",
            balance = p2pEthPool(
                staked = 5,
                unstaking = 2,
                withdrawable = 1,
                rewards = 3,
                isStakedIncludedInNetworkBalance = true,
            ),
            expected = BigDecimal(3),
        ),
        DeltaTestModel(
            description = "P2PEthPool with several accounts -> all of them count",
            balance = p2pEthPool(
                accounts = listOf(
                    account(staked = 5, unstaking = 0, withdrawable = 0, rewards = 1),
                    account(staked = 3, unstaking = 0, withdrawable = 0, rewards = 2),
                ),
            ),
            expected = BigDecimal(11),
        ),
    )

    internal data class DeltaTestModel(
        val description: String,
        val balance: StakingBalance.Data,
        val expected: BigDecimal,
    ) {

        override fun toString(): String = description
    }

    // region Fixtures
    private fun stakeKit(
        items: List<BalanceItem>,
        source: StatusSource = StatusSource.ACTUAL,
        isStakedIncludedInNetworkBalance: Boolean = false,
    ): StakingBalance.Data.StakeKit {
        return StakingBalance.Data.StakeKit(
            stakingId = StakingID(integrationId = "integration", address = "0x1"),
            source = source,
            balance = YieldBalanceItem(items = items, integrationId = "integration"),
            isStakedIncludedInNetworkBalance = isStakedIncludedInNetworkBalance,
        )
    }

    /** [BalanceItem] is a plain data carrier here — only [BalanceItem.amount] and [BalanceItem.type] are read. */
    private fun balanceItem(amount: Int, type: BalanceType): BalanceItem {
        return mockk(relaxed = true) {
            every { this@mockk.amount } returns BigDecimal(amount)
            every { this@mockk.type } returns type
        }
    }

    private fun p2pEthPool(
        staked: Int = 0,
        unstaking: Int = 0,
        withdrawable: Int = 0,
        rewards: Int = 0,
        isStakedIncludedInNetworkBalance: Boolean = false,
        accounts: List<P2PEthPoolStakingAccount> = listOf(
            account(staked = staked, unstaking = unstaking, withdrawable = withdrawable, rewards = rewards),
        ),
    ): StakingBalance.Data.P2PEthPool {
        return StakingBalance.Data.P2PEthPool(
            stakingId = StakingID(integrationId = "integration", address = "0x1"),
            source = StatusSource.ACTUAL,
            accounts = accounts,
            isStakedIncludedInNetworkBalance = isStakedIncludedInNetworkBalance,
        )
    }

    /** Unstaking / withdrawable are derived from the exit queue by claimability, so they go in as requests. */
    private fun account(staked: Int, unstaking: Int, withdrawable: Int, rewards: Int): P2PEthPoolStakingAccount {
        return P2PEthPoolStakingAccount(
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
    // endregion
}