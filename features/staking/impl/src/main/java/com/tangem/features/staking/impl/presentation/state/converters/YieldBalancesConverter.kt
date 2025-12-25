package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.RewardBlockType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingBalanceEntry
import com.tangem.domain.models.staking.StakingEntryType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.features.staking.impl.presentation.state.YieldReward
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.isStakingRewardUnavailable
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class YieldBalancesConverter(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val balancesToShowProvider: Provider<List<StakingBalanceEntry>>,
    private val integration: StakingIntegration,
) : Converter<Unit, InnerYieldBalanceState> {

    private val balanceEntryConverter by lazy(LazyThreadSafetyMode.NONE) {
        StakingBalanceEntryConverter(cryptoCurrencyStatus, appCurrencyProvider, integration)
    }

    override fun convert(value: Unit): InnerYieldBalanceState {
        val appCurrency = appCurrencyProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val stakingBalance = cryptoCurrencyStatus.value.stakingBalance
        val balanceEntries = balancesToShowProvider()
        val hasStakingData = stakingBalance is StakingBalance.Data

        return if (hasStakingData || balanceEntries.any { it.isPending }) {
            val cryptoRewardsValue = (stakingBalance as? StakingBalance.Data)?.totalRewards

            val fiatRate = cryptoCurrencyStatus.value.fiatRate
            val fiatRewardsValue = if (fiatRate != null && cryptoRewardsValue != null) {
                fiatRate.times(cryptoRewardsValue)
            } else {
                null
            }
            val type = getRewardBlockType(stakingBalance)
            val pendingRewardsConstraints = getRewardConstraints(stakingBalance)

            InnerYieldBalanceState.Data(
                integrationId = stakingBalance?.stakingId?.integrationId,
                reward = YieldReward(
                    rewardsCrypto = cryptoRewardsValue.format { crypto(cryptoCurrency) },
                    rewardsFiat = fiatRewardsValue.format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                    rewardBlockType = type,
                    rewardConstraints = pendingRewardsConstraints,
                ),
                isActionable = type.isActionable,
                balances = balanceEntries.mapBalances(),
            )
        } else {
            InnerYieldBalanceState.Empty
        }
    }

    private fun List<StakingBalanceEntry>.mapBalances() = asSequence()
        .filterNot { it.amount.isZero() || it.type == StakingEntryType.REWARDS }
        .mapNotNull(balanceEntryConverter::convert)
        .sortedByDescending { it.cryptoAmount }
        .sortedBy { it.type.order }
        .toPersistentList()

    private fun getRewardBlockType(stakingBalance: StakingBalance?): RewardBlockType {
        val blockchainId = cryptoCurrencyStatus.currency.network.rawId

        if (stakingBalance is StakingBalance.Data.P2PEthPool) {
            return if (isStakingRewardUnavailable(blockchainId)) {
                RewardBlockType.RewardUnavailable.DefaultRewardUnavailable
            } else {
                RewardBlockType.NoRewards
            }
        }

        val stakeKitBalance = stakingBalance as? StakingBalance.Data.StakeKit
        val rewards = stakeKitBalance?.balance?.items
            ?.filter { it.type == BalanceType.REWARDS && !it.amount.isZero() }

        val isActionable = rewards?.any { it.pendingActions.isNotEmpty() } == true
        val isRewardsClaimable = rewards?.isNotEmpty() == true

        return when {
            isStakingRewardUnavailable(blockchainId) -> {
                if (BlockchainUtils.isSolana(blockchainId)) {
                    RewardBlockType.RewardUnavailable.SolanaRewardUnavailable
                } else {
                    RewardBlockType.RewardUnavailable.DefaultRewardUnavailable
                }
            }
            isRewardsClaimable && isActionable -> RewardBlockType.Rewards
            isRewardsClaimable && !isActionable -> RewardBlockType.RewardsRequirementsError
            else -> RewardBlockType.NoRewards
        }
    }

    private fun getRewardConstraints(stakingBalance: StakingBalance?) =
        (stakingBalance as? StakingBalance.Data.StakeKit)
            ?.balance?.items
            ?.firstOrNull { it.type == BalanceType.REWARDS }
            ?.pendingActionsConstraints
            ?.firstOrNull { it.type == StakingActionType.CLAIM_REWARDS }
}