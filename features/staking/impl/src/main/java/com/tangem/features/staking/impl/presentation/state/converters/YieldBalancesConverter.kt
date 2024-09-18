package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.PendingTransaction
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
    private val pendingTransactionsProvider: Provider<List<PendingTransaction>>,
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

            val pendingTransactions = pendingTransactionsProvider()
            val pendingTransactionBalanceItems = pendingTransactions.mapNotNull {
                PendingTransactionItemConverter.convert(it)
            }

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
                balance = mergeRealAndPendingTransactions(
                    real = yieldBalance.balance.items,
                    pending = pendingTransactionBalanceItems,
                ).mapBalances(),
            )
        } else {
            InnerYieldBalanceState.Empty
        }
    }

    private fun mergeRealAndPendingTransactions(
        real: List<BalanceItem>,
        pending: List<BalanceItem>,
    ): List<BalanceItem> {
        val map = real.associateBy { Triple(it.groupId, it.type, it.amount) }.toMutableMap()

        pending.forEach { pendingItem ->
            val key = Triple(pendingItem.groupId, pendingItem.type, pendingItem.amount)
            map[key] = pendingItem
        }
        // TODO staking add removal
        return map.values.toList()
    }

    private fun List<BalanceItem>.mapBalances() = asSequence()
        .filterNot { it.amount.isZero() || it.type == BalanceType.REWARDS }
        .mapNotNull(balanceItemConverter::convert)
        .sortedByDescending { it.cryptoDecimal }
        .sortedBy { it.type.order }
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
