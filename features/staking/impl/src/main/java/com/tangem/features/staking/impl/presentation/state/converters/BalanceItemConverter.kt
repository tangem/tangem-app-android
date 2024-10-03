package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.BalanceType.Companion.isClickable
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList
import org.joda.time.DateTime
import java.util.Calendar

internal class BalanceItemConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val yield: Yield,
) : Converter<BalanceItem, BalanceState?> {

    override fun convert(value: BalanceItem): BalanceState? {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val appCurrency = appCurrencyProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency

        val validator = yield.validators.firstOrNull {
            value.validatorAddress?.contains(it.address, ignoreCase = true) == true
        }

        val cryptoAmount = value.amount
        val fiatAmount = cryptoCurrencyStatus.value.fiatRate?.times(cryptoAmount)

        val title = value.type.getTitle(validator?.name)
        return title?.let {
            BalanceState(
                groupId = value.groupId,
                validator = validator,
                title = title,
                subtitle = getSubtitle(value),
                type = value.type,
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
                rawCurrencyId = value.rawCurrencyId,
                pendingActions = value.pendingActions.toPersistentList(),
                isClickable = value.type.isClickable() && !value.isPending,
                isPending = value.isPending,
            )
        }
    }

    private fun BalanceType.getTitle(validatorName: String?) = when (this) {
        BalanceType.PREPARING,
        BalanceType.STAKED,
        -> validatorName?.let { stringReference(it) }
        BalanceType.UNSTAKED -> resourceReference(R.string.staking_unstaked)
        BalanceType.UNSTAKING -> resourceReference(R.string.staking_unstaking)
        BalanceType.LOCKED -> resourceReference(R.string.staking_locked)
        BalanceType.AVAILABLE,
        BalanceType.REWARDS,
        BalanceType.UNLOCKING,
        BalanceType.UNKNOWN,
        -> null
    }

    private fun getSubtitle(balance: BalanceItem) = when (balance.type) {
        BalanceType.UNSTAKING -> getUnbondingDate(balance.date)
        BalanceType.UNSTAKED -> resourceReference(R.string.staking_tap_to_withdraw)
        BalanceType.LOCKED -> if (balance.pendingActions.any { it.type == StakingActionType.VOTE_LOCKED }) {
            resourceReference(R.string.staking_tap_to_unlock_or_vote)
        } else {
            resourceReference(R.string.staking_tap_to_unlock)
        }
        BalanceType.PREPARING -> {
            val warmupPeriod = yield.metadata.warmupPeriod.days
            combinedReference(
                resourceReference(R.string.staking_details_warmup_period),
                stringReference(" "),
                pluralReference(R.plurals.common_days, warmupPeriod, wrappedList(warmupPeriod)),
            )
        }
        BalanceType.AVAILABLE,
        BalanceType.STAKED,
        BalanceType.UNLOCKING,
        BalanceType.REWARDS,
        BalanceType.UNKNOWN,
        -> null
    }

    private fun getUnbondingDate(date: DateTime?): TextReference? {
        val unbondingPeriod = yield.metadata.cooldownPeriod?.days ?: return null
        if (date == null) {
            return combinedReference(
                resourceReference(R.string.staking_details_unbonding_period),
                stringReference(" "),
                pluralReference(R.plurals.common_days, unbondingPeriod, wrappedList(unbondingPeriod)),
            )
        }

        val nowCalendar = Calendar.getInstance()
        nowCalendar.resetHours()

        val endDate = Calendar.getInstance()
        endDate.timeInMillis = date.millis
        endDate.resetHours()

        val days = ((endDate.timeInMillis - nowCalendar.timeInMillis) / DAY_IN_MILLIS).toInt()
        return if (days > 0) {
            resourceReference(
                R.string.common_left,
                wrappedList(
                    pluralReference(R.plurals.common_days, days, wrappedList(days)),
                ),
            )
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
