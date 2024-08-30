package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.common.extensions.isZero
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.staking.model.stakekit.BalanceType.Companion.isClickable
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceGroupedState
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.InnerYieldBalanceState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.toPersistentList
import org.joda.time.DateTime
import java.util.Calendar

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
            val isClickable = item.key.isClickable()
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
                val unbonding = getUnbondingDate(balance.date)
                val warmupPeriod = yield.metadata.warmupPeriod.days
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
                        unbondingPeriod = unbonding,
                        warmupPeriod = pluralReference(R.plurals.common_days, warmupPeriod, wrappedList(warmupPeriod)),
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
        BalanceType.PREPARING -> resourceReference(R.string.staking_preparing) to null
        BalanceType.REWARDS -> null to null
        BalanceType.LOCKED -> null to null
        BalanceType.UNLOCKING -> null to null
        BalanceType.UNKNOWN -> null to null
    }

    private fun getUnbondingDate(date: DateTime?): TextReference {
        val now = DateTime.now().millis
        val nowCalendar = Calendar.getInstance()
        nowCalendar.resetHours()

        val endDate = Calendar.getInstance()
        endDate.timeInMillis = date?.millis ?: now
        endDate.resetHours()

        val days = ((endDate.timeInMillis - nowCalendar.timeInMillis) / DAY_IN_MILLIS).toInt()
        return if (days > 0) {
            pluralReference(R.plurals.common_in_days, days, wrappedList(days))
        } else {
            resourceReference(R.string.common_today)
        }
    }

    private fun Calendar.resetHours() {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }

    private companion object {
        const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000
    }
}