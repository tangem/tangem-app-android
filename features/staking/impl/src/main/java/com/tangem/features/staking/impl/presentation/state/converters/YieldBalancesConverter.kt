package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceItem
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.RewardBlockType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.utils.getRewardStakingBalance
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
    private val balancesToShowProvider: Provider<List<BalanceItem>>,
    private val integration: StakingIntegration,
) : Converter<Unit, InnerYieldBalanceState> {

    private val balanceItemConverter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceItemConverter(cryptoCurrencyStatus, appCurrencyProvider, integration)
    }

    override fun convert(value: Unit): InnerYieldBalanceState {
        val appCurrency = appCurrencyProvider()

        val cryptoCurrency = cryptoCurrencyStatus.currency
        val stakeKitBalance = cryptoCurrencyStatus.value.stakingBalance as? StakingBalance.Data.StakeKit
        val balanceToShowItems = balancesToShowProvider()

        return if (stakeKitBalance != null || balanceToShowItems.any { it.isPending }) {
            val cryptoRewardsValue = stakeKitBalance?.getRewardStakingBalance()

            val fiatRate = cryptoCurrencyStatus.value.fiatRate
            val fiatRewardsValue = if (fiatRate != null && cryptoRewardsValue != null) {
                fiatRate.times(cryptoRewardsValue)
            } else {
                null
            }
            val type = getRewardBlockType()
            val pendingRewardsConstraints = stakeKitBalance?.balance?.items
                ?.firstOrNull { it.type == BalanceType.REWARDS }
                ?.pendingActionsConstraints
                ?.firstOrNull { it.type == StakingActionType.CLAIM_REWARDS }

            InnerYieldBalanceState.Data(
                integrationId = stakeKitBalance?.stakingId?.integrationId,
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
                balances = balanceToShowItems.mapBalances(),
            )
        } else {
            // TODO p2p
            InnerYieldBalanceState.Empty
        }
    }

    private fun List<BalanceItem>.mapBalances() = asSequence()
        .filterNot { it.amount.isZero() || it.type == BalanceType.REWARDS }
        .mapNotNull(balanceItemConverter::convert)
        .sortedByDescending { it.cryptoAmount }
        .sortedBy { it.type.order }
        .toPersistentList()

    private fun getRewardBlockType(): RewardBlockType {
        val blockchainId = cryptoCurrencyStatus.currency.network.rawId
        val stakeKitBalance = cryptoCurrencyStatus.value.stakingBalance as? StakingBalance.Data.StakeKit
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
}