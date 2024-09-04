package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class YieldBalancesConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
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

        return if (yieldBalance is YieldBalance.Data) {
            val cryptoRewardsValue = yieldBalance.getRewardStakingBalance()
            val fiatRewardsValue = cryptoCurrencyStatus.value.fiatRate?.times(cryptoRewardsValue)

            InnerYieldBalanceState.Data(
                rewardsCrypto = BigDecimalFormatter.formatCryptoAmount(
                    cryptoAmount = cryptoRewardsValue,
                    cryptoCurrency = cryptoCurrency,
                ),
                rewardsFiat = BigDecimalFormatter.formatFiatAmount(
                    fiatAmount = fiatRewardsValue,
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                ),
                rewardBlockType = getRewardBlockType(),
                balance = yieldBalance.balance.items.mapBalances(),
            )
        } else {
            InnerYieldBalanceState.Empty
        }
    }

    private fun List<BalanceItem>.mapBalances() = sortedBy { it.type.order }
        .filterNot { it.amount.isZero() || it.type == BalanceType.REWARDS }
        .mapNotNull(balanceItemConverter::convert)
        .toPersistentList()

    private fun getRewardBlockType(): RewardBlockType {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data
        val isRewardsClaimable = yieldBalance?.balance?.items
            ?.filter { it.type == BalanceType.REWARDS }
            ?.any { it.pendingActions.isNotEmpty() }
            ?: false

        val isSolana = isSolana(cryptoCurrencyStatus.currency.network.id.value)

        return when {
            isSolana -> RewardBlockType.RewardUnavailable
            isRewardsClaimable -> RewardBlockType.Rewards
            else -> RewardBlockType.NoRewards
        }
    }
}