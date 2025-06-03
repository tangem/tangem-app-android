package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.staking.utils.getRewardStakingBalance
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.features.staking.impl.presentation.state.YieldReward
import com.tangem.lib.crypto.BlockchainUtils.isStakingRewardUnavailable
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class YieldBalancesConverter(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val balancesToShowProvider: Provider<List<BalanceItem>>,
    private val yield: Yield,
) : Converter<Unit, InnerYieldBalanceState> {

    private val balanceItemConverter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceItemConverter(cryptoCurrencyStatus, appCurrencyProvider, yield)
    }

    override fun convert(value: Unit): InnerYieldBalanceState {
        val appCurrency = appCurrencyProvider()

        val cryptoCurrency = cryptoCurrencyStatus.currency
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data
        val balanceToShowItems = balancesToShowProvider()

        return if (yieldBalance != null || balanceToShowItems.any { it.isPending }) {
            val cryptoRewardsValue = yieldBalance?.getRewardStakingBalance()

            val fiatRate = cryptoCurrencyStatus.value.fiatRate
            val fiatRewardsValue = if (fiatRate != null && cryptoRewardsValue != null) {
                fiatRate.times(cryptoRewardsValue)
            } else {
                null
            }
            val type = getRewardBlockType()
            val pendingRewardsConstraints = yieldBalance?.balance?.items
                ?.firstOrNull { it.type == BalanceType.REWARDS }
                ?.pendingActionsConstraints
                ?.firstOrNull { it.type == StakingActionType.CLAIM_REWARDS }

            InnerYieldBalanceState.Data(
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
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data
        val rewards = yieldBalance?.balance?.items
            ?.filter { it.type == BalanceType.REWARDS && !it.amount.isZero() }

        val isActionable = rewards?.any { it.pendingActions.isNotEmpty() } == true
        val isRewardsClaimable = rewards?.isNotEmpty() == true

        return when {
            isStakingRewardUnavailable(blockchainId) -> RewardBlockType.RewardUnavailable
            isRewardsClaimable && isActionable -> RewardBlockType.Rewards
            isRewardsClaimable && !isActionable -> RewardBlockType.RewardsRequirementsError
            else -> RewardBlockType.NoRewards
        }
    }
}