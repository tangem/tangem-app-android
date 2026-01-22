package com.tangem.domain.models.staking

import com.tangem.domain.models.staking.StakingEntryType.Companion.fromBalanceType

fun BalanceItem.toStakingBalanceEntry(validatorName: String? = null): StakingBalanceEntry {
    return StakingBalanceEntry(
        id = groupId,
        type = fromBalanceType(type),
        amount = amount,
        validator = validatorAddress?.let {
            ValidatorInfo(address = it, name = validatorName)
        },
        date = date,
        actions = StakingEntryActions.StakeKit(
            pendingActions = pendingActions,
            pendingActionsConstraints = pendingActionsConstraints,
        ),
        isPending = isPending,
        rawCurrencyId = rawCurrencyId,
    )
}

fun List<BalanceItem>.toStakingBalanceEntries(
    validatorNameResolver: (String?) -> String? = { null },
): List<StakingBalanceEntry> {
    return map { it.toStakingBalanceEntry(validatorNameResolver(it.validatorAddress)) }
}