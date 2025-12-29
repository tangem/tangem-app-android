package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingBalanceEntry
import com.tangem.domain.models.staking.StakingEntryActions
import com.tangem.domain.models.staking.StakingEntryType
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingTarget
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.utils.toTextReference
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.BlockchainUtils.isTon
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.Instant
import java.math.BigDecimal
import java.util.Calendar

internal class StakingBalanceEntryConverter(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val integration: StakingIntegration,
) : Converter<StakingBalanceEntry, BalanceState?> {

    override fun convert(value: StakingBalanceEntry): BalanceState? {
        val appCurrency = appCurrencyProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency

        val target = findTarget(value)
        val cryptoAmount = value.getBalanceValue()
        val fiatAmount = cryptoCurrencyStatus.value.fiatRate?.times(cryptoAmount)

        val title = value.type.getTitle(target?.name ?: value.validator?.name)
        return title?.let {
            BalanceState(
                groupId = value.id,
                target = target,
                title = title,
                subtitle = getSubtitle(value),
                type = value.type.toBalanceType(),
                cryptoValue = cryptoAmount.parseBigDecimal(cryptoCurrency.decimals),
                cryptoAmount = cryptoAmount,
                formattedCryptoAmount = stringReference(
                    cryptoAmount.format { crypto(cryptoCurrency) },
                ),
                fiatAmount = fiatAmount,
                formattedFiatAmount = stringReference(
                    fiatAmount.format {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                        )
                    },
                ),
                rawCurrencyId = value.rawCurrencyId,
                pendingActions = value.getPendingActions().toPersistentList(),
                isClickable = value.isClickable(),
                isPending = value.isPending,
                targetAddress = value.validator?.address,
            )
        }
    }

    private fun findTarget(entry: StakingBalanceEntry): StakingTarget? {
        return integration.targets.firstOrNull {
            entry.validator?.address?.contains(it.address, ignoreCase = true) == true
        }
    }

    private fun StakingBalanceEntry.getBalanceValue(): BigDecimal {
        val isIncludeStakingTotalBalance = BlockchainUtils.isIncludeStakingTotalBalance(
            blockchainId = cryptoCurrencyStatus.currency.network.rawId,
        )
        return if (isIncludeStakingTotalBalance) {
            amount
        } else {
            val stakingBalance = cryptoCurrencyStatus.value.stakingBalance
            if (stakingBalance is StakingBalance.Data.StakeKit) {
                amount - stakingBalance.totalRewards
            } else {
                amount
            }
        }
    }

    private fun StakingEntryType.getTitle(validatorName: String?): TextReference? = when (this) {
        StakingEntryType.PREPARING,
        StakingEntryType.STAKED,
        -> validatorName?.let { stringReference(it) }
        StakingEntryType.WITHDRAWABLE -> resourceReference(R.string.staking_unstaked)
        StakingEntryType.UNSTAKING -> resourceReference(R.string.staking_unstaking)
        StakingEntryType.LOCKED -> resourceReference(R.string.staking_locked)
        StakingEntryType.AVAILABLE,
        StakingEntryType.REWARDS,
        StakingEntryType.UNLOCKING,
        StakingEntryType.UNKNOWN,
        -> null
    }

    private fun getSubtitle(entry: StakingBalanceEntry): TextReference? = when (entry.type) {
        StakingEntryType.UNSTAKING -> getUnbondingDate(entry.date)
        StakingEntryType.WITHDRAWABLE -> resourceReference(R.string.staking_tap_to_withdraw)
        StakingEntryType.LOCKED -> {
            val hasVoteLocked = entry.getPendingActions().any { it.type == StakingActionType.VOTE_LOCKED }
            if (hasVoteLocked) {
                resourceReference(R.string.staking_tap_to_unlock_or_vote)
            } else {
                resourceReference(R.string.staking_tap_to_unlock)
            }
        }
        StakingEntryType.PREPARING -> {
            val warmupPeriod = integration.warmupPeriodDays
            TextReference.Combined(
                wrappedList(
                    resourceReference(R.string.staking_details_warmup_period),
                    stringReference(" "),
                    pluralReference(R.plurals.common_days, warmupPeriod, wrappedList(warmupPeriod)),
                ),
            )
        }
        StakingEntryType.AVAILABLE,
        StakingEntryType.STAKED,
        StakingEntryType.UNLOCKING,
        StakingEntryType.REWARDS,
        StakingEntryType.UNKNOWN,
        -> null
    }

    private fun getUnbondingDate(date: Instant?): TextReference? {
        val cooldownPeriod = integration.cooldownPeriod ?: return null
        if (date == null) {
            return TextReference.Combined(
                wrappedList(
                    resourceReference(R.string.staking_details_unbonding_period),
                    stringReference(" "),
                    cooldownPeriod.toTextReference(),
                ),
            )
        }

        val nowCalendar = Calendar.getInstance()
        nowCalendar.resetHours()

        val endDate = Calendar.getInstance()
        endDate.timeInMillis = date.toEpochMilliseconds()
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

    private fun StakingBalanceEntry.isClickable(): Boolean {
        val networkId = cryptoCurrencyStatus.currency.network.rawId
        return when {
            // TON allows withdrawing funds in the preparing state, unlike other networks.
            isTon(networkId) && this.type == StakingEntryType.PREPARING -> {
                getPendingActions().any { it.type == StakingActionType.WITHDRAW }
            }
            else -> this.type.isClickableType() && !this.isPending
        }
    }

    private fun StakingEntryType.isClickableType(): Boolean = when (this) {
        StakingEntryType.STAKED,
        StakingEntryType.WITHDRAWABLE,
        StakingEntryType.LOCKED,
        -> true
        else -> false
    }

    private fun StakingBalanceEntry.getPendingActions(): List<PendingAction> {
        return when (val actions = this.actions) {
            is StakingEntryActions.StakeKit -> actions.pendingActions
            is StakingEntryActions.P2PEthPool -> if (type == StakingEntryType.WITHDRAWABLE) {
                listOf(STUB_WITHDRAW_ACTION)
            } else {
                emptyList()
            }
        }
    }

    private fun StakingEntryType.toBalanceType(): BalanceType {
        return when (this) {
            StakingEntryType.AVAILABLE -> BalanceType.AVAILABLE
            StakingEntryType.STAKED -> BalanceType.STAKED
            StakingEntryType.PREPARING -> BalanceType.PREPARING
            StakingEntryType.LOCKED -> BalanceType.LOCKED
            StakingEntryType.UNSTAKING -> BalanceType.UNSTAKING
            StakingEntryType.UNLOCKING -> BalanceType.UNLOCKING
            StakingEntryType.WITHDRAWABLE -> BalanceType.UNSTAKED
            StakingEntryType.REWARDS -> BalanceType.REWARDS
            StakingEntryType.UNKNOWN -> BalanceType.UNKNOWN
        }
    }

    private fun Calendar.resetHours() {
        this[Calendar.HOUR_OF_DAY] = 0
        this[Calendar.MINUTE] = 0
        this[Calendar.SECOND] = 0
        this[Calendar.MILLISECOND] = 0
    }

    private companion object {
        val STUB_WITHDRAW_ACTION = PendingAction(
            StakingActionType.WITHDRAW,
            passthrough = "",
            args = null,
        )
        const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000
    }
}