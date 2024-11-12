package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.lib.crypto.BlockchainUtils.isBSC
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class YieldBalancesConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val balancesToShowProvider: Provider<List<BalanceItem>>,
    private val yield: Yield,
) : Converter<Unit, InnerYieldBalanceState> {

    private val balanceItemConverter by lazy(LazyThreadSafetyMode.NONE) {
        BalanceItemConverter(cryptoCurrencyStatusProvider, appCurrencyProvider, yield)
    }

    override fun convert(value: Unit): InnerYieldBalanceState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val appCurrency = appCurrencyProvider()

        val cryptoCurrency = cryptoCurrencyStatus.currency
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance
        val balanceToShowItems = balancesToShowProvider()

        return if (yieldBalance is YieldBalance.Data || balanceToShowItems.any { it.isPending }) {
            val cryptoRewardsValue = (yieldBalance as? YieldBalance.Data)?.getRewardStakingBalance()

            val fiatRate = cryptoCurrencyStatus.value.fiatRate
            val fiatRewardsValue = if (fiatRate != null && cryptoRewardsValue != null) {
                fiatRate.times(cryptoRewardsValue)
            } else {
                null
            }
            val (type, isActionable) = getRewardBlockType()
            InnerYieldBalanceState.Data(
                rewardsCrypto = cryptoRewardsValue.format { crypto(cryptoCurrency) },
                rewardsFiat = fiatRewardsValue.format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                },
                rewardBlockType = type,
                isActionable = isActionable,
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

    private fun getRewardBlockType(): Pair<RewardBlockType, Boolean> {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val blockchainId = cryptoCurrencyStatus.currency.network.id.value
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data
        val rewards = yieldBalance?.balance?.items
            ?.filter { it.type == BalanceType.REWARDS && !it.amount.isZero() }

        val isActionable = rewards?.any { it.pendingActions.isNotEmpty() } == true
        val isRewardsClaimable = rewards?.isNotEmpty() == true

        return when {
            isSolana(blockchainId) || isBSC(blockchainId) -> RewardBlockType.RewardUnavailable to false
            isRewardsClaimable -> RewardBlockType.Rewards to isActionable
            else -> RewardBlockType.NoRewards to false
        }
    }
}