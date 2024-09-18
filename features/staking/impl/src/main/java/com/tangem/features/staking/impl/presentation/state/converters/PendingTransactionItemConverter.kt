package com.tangem.features.staking.impl.presentation.state.converters

import com.tangem.core.ui.extensions.*
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.features.staking.impl.R
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime

internal object PendingTransactionItemConverter : Converter<PendingTransaction, BalanceItem?> {

    override fun convert(value: PendingTransaction): BalanceItem? {
        val balanceType = value.type ?: return null
        val title = balanceType.getTitle(value.validator?.name)
        return title?.let {
            BalanceItem(
                groupId = value.groupId ?: return null,
                type = balanceType,
                amount = value.amount ?: return null,
                rawCurrencyId = value.rawCurrencyId,
                validatorAddress = value.validator?.address,
                date = DateTime.now(),
                pendingActions = emptyList(),
                isPending = true,
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
}
