package com.tangem.data.staking

import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime

internal object PendingTransactionItemConverter : Converter<PendingTransaction, BalanceItem?> {

    override fun convert(value: PendingTransaction): BalanceItem? {
        return BalanceItem(
            groupId = value.groupId ?: return null,
            token = value.token,
            type = value.type,
            amount = value.amount,
            rawCurrencyId = value.rawCurrencyId,
            validatorAddress = value.validator?.address,
            date = DateTime.now(),
            pendingActions = emptyList(),
            isPending = true,
        )
    }
}