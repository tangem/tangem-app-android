package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceGroupedState
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.toPersistentList

internal class YieldBalancesConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val yield: Yield,
) : Converter<Unit, InnerYieldBalanceState> {
    override fun convert(value: Unit): InnerYieldBalanceState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val appCurrency = appCurrencyProvider()

        val cryptoCurrency = cryptoCurrencyStatus.currency
        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance

        return if (yieldBalance is YieldBalance.Data) {
            val cryptoRewardsValue = yieldBalance.getRewardStakingBalance()
            val fiatRewardsValue = cryptoCurrencyStatus.value.fiatRate?.times(cryptoRewardsValue)
            val groupedBalances = getGroupedBalance(yieldBalance.balance)
            val isRewardsClaimable = yieldBalance.balance.items
                .filter { it.type == BalanceType.REWARDS }
                .any { it.pendingActions.isNotEmpty() }
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
                isRewardsToClaim = !cryptoRewardsValue.isNullOrZero(),
                isRewardsClaimable = isRewardsClaimable,
                balance = groupedBalances,
            )
        } else {
            InnerYieldBalanceState.Empty
        }
    }

    private fun getGroupedBalance(balance: YieldBalanceItem) = balance.items
        .sortedBy { it.type }
        .groupBy { it.type.toGroup() }
        .mapNotNull { item ->
            val (title, footer) = getGroupTitle(item.key)
            val isClickable = getClickableType(item.key)
            title?.let {
                BalanceGroupedState(
                    items = item.value.mapBalances().toPersistentList(),
                    footer = footer,
                    title = it,
                    type = item.key,
                    isClickable = isClickable,
                )
            }
        }
        .filterNot { it.items.isEmpty() }
        .toPersistentList()

    private fun List<BalanceItem>.mapBalances(): List<BalanceState> {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val appCurrency = appCurrencyProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency
        return this
            .filterNot { it.amount.isZero() }
            .mapNotNull { balance ->
                val validator = yield.validators.firstOrNull {
                    balance.validatorAddress?.contains(it.address, ignoreCase = true) == true
                }
                val cryptoAmount = balance.amount
                val fiatAmount = cryptoCurrencyStatus.value.fiatRate?.times(cryptoAmount)
                val unbondingPeriod = yield.metadata.cooldownPeriod.days
                validator?.let {
                    BalanceState(
                        validator = validator,
                        cryptoValue = cryptoAmount.parseBigDecimal(cryptoCurrency.decimals),
                        cryptoDecimal = cryptoAmount,
                        cryptoAmount = stringReference(
                            BigDecimalFormatter.formatCryptoAmount(
                                cryptoAmount = cryptoAmount,
                                cryptoCurrency = cryptoCurrency,
                            ),
                        ),
                        fiatAmount = stringReference(
                            BigDecimalFormatter.formatFiatAmount(
                                fiatAmount = fiatAmount,
                                fiatCurrencyCode = appCurrency.code,
                                fiatCurrencySymbol = appCurrency.symbol,
                            ),
                        ),
                        rawCurrencyId = balance.rawCurrencyId,
                        unbondingPeriod = pluralReference(
                            id = R.plurals.common_days,
                            count = unbondingPeriod,
                            formatArgs = wrappedList(unbondingPeriod),
                        ),
                        pendingActions = balance.pendingActions.toPersistentList(),
                    )
                }
            }
    }

    private fun BalanceType.toGroup() = when (this) {
        BalanceType.REWARDS,
        BalanceType.UNKNOWN,
        -> BalanceType.UNKNOWN
        else -> this
    }

    private fun getGroupTitle(type: BalanceType) = when (type) {
        BalanceType.STAKED -> resourceReference(R.string.staking_active) to
            resourceReference(R.string.staking_active_footer)
        BalanceType.UNSTAKED -> resourceReference(R.string.staking_unstaked) to
            resourceReference(R.string.staking_unstaked_footer)
        BalanceType.UNSTAKING -> resourceReference(R.string.staking_unstaking) to null
        BalanceType.AVAILABLE -> null to null
        BalanceType.PREPARING -> null to null
        BalanceType.REWARDS -> null to null
        BalanceType.LOCKED -> null to null
        BalanceType.UNLOCKING -> null to null
        BalanceType.UNKNOWN -> null to null
    }

    private fun getClickableType(type: BalanceType) = when (type) {
        BalanceType.STAKED,
        BalanceType.UNSTAKED,
        -> true
        BalanceType.AVAILABLE,
        BalanceType.UNSTAKING,
        BalanceType.PREPARING,
        BalanceType.REWARDS,
        BalanceType.LOCKED,
        BalanceType.UNLOCKING,
        BalanceType.UNKNOWN,
        -> false
    }
}
