package com.tangem.domain.models.staking

import java.math.BigDecimal

fun P2PEthPoolStakingAccount.toStakingBalanceEntries(vaultName: String? = null): List<StakingBalanceEntry> {
    return buildList {
        if (stake.assets > BigDecimal.ZERO) {
            add(createStakedEntry(vaultAddress, stake.assets, vaultName))
        }
        exitQueue.requests.filter { !it.isClaimable }.forEach { add(createUnstakingEntry(vaultAddress, it, vaultName)) }
        if (availableToWithdraw > BigDecimal.ZERO) {
            add(createWithdrawableEntry(vaultAddress, availableToWithdraw, vaultName))
        }
        if (stake.totalEarnedAssets > BigDecimal.ZERO) {
            add(createRewardsEntry(vaultAddress, stake.totalEarnedAssets, vaultName))
        }
    }
}

private fun createStakedEntry(vaultAddress: String, amount: BigDecimal, vaultName: String?): StakingBalanceEntry {
    return StakingBalanceEntry(
        id = vaultAddress,
        type = StakingEntryType.STAKED,
        amount = amount,
        validator = ValidatorInfo(address = vaultAddress, name = vaultName),
        date = null,
        actions = StakingEntryActions.P2PEthPool(ticket = null, estimatedWithdrawalDate = null, isClaimable = false),
        isPending = false,
        rawCurrencyId = null,
    )
}

private fun createUnstakingEntry(
    vaultAddress: String,
    request: P2PEthPoolExitRequest,
    vaultName: String?,
): StakingBalanceEntry {
    return StakingBalanceEntry(
        id = "${vaultAddress}_${request.ticket}",
        type = StakingEntryType.UNSTAKING,
        amount = request.totalAssets,
        validator = ValidatorInfo(address = vaultAddress, name = vaultName),
        date = request.withdrawalTimestamp,
        actions = StakingEntryActions.P2PEthPool(
            ticket = request.ticket,
            estimatedWithdrawalDate = request.withdrawalTimestamp,
            isClaimable = false,
        ),
        isPending = false,
        rawCurrencyId = null,
    )
}

private fun createWithdrawableEntry(
    vaultAddress: String,
    amount: BigDecimal,
    vaultName: String?,
): StakingBalanceEntry {
    return StakingBalanceEntry(
        id = "${vaultAddress}_withdrawable",
        type = StakingEntryType.WITHDRAWABLE,
        amount = amount,
        validator = ValidatorInfo(address = vaultAddress, name = vaultName),
        date = null,
        actions = StakingEntryActions.P2PEthPool(
            ticket = null,
            estimatedWithdrawalDate = null,
            isClaimable = true,
        ),
        isPending = false,
        rawCurrencyId = null,
    )
}

private fun createRewardsEntry(vaultAddress: String, amount: BigDecimal, vaultName: String?): StakingBalanceEntry {
    return StakingBalanceEntry(
        id = "${vaultAddress}_rewards",
        type = StakingEntryType.REWARDS,
        amount = amount,
        validator = ValidatorInfo(address = vaultAddress, name = vaultName),
        date = null,
        actions = StakingEntryActions.P2PEthPool(ticket = null, estimatedWithdrawalDate = null, isClaimable = false),
        isPending = false,
        rawCurrencyId = null,
    )
}