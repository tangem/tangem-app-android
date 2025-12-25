package com.tangem.domain.models.staking

import java.math.BigDecimal

fun P2PEthPoolStakingAccount.toStakingBalanceEntries(vaultName: String? = null): List<StakingBalanceEntry> {
    return buildList {
        if (stake.assets > BigDecimal.ZERO) {
            add(
                StakingBalanceEntry(
                    id = vaultAddress,
                    type = StakingEntryType.STAKED,
                    amount = stake.assets,
                    validator = ValidatorInfo(address = vaultAddress, name = vaultName),
                    date = null,
                    actions = StakingEntryActions.P2PEthPool(
                        ticket = null,
                        estimatedWithdrawalDate = null,
                        isClaimable = false,
                    ),
                    isPending = false,
                    rawCurrencyId = null,
                ),
            )
        }

        exitQueue.requests.forEach { request ->
            add(
                StakingBalanceEntry(
                    id = "${vaultAddress}_${request.ticket}",
                    type = StakingEntryType.UNSTAKING,
                    amount = request.totalAssets,
                    validator = ValidatorInfo(address = vaultAddress, name = vaultName),
                    date = request.withdrawalTimestamp,
                    actions = StakingEntryActions.P2PEthPool(
                        ticket = request.ticket,
                        estimatedWithdrawalDate = request.withdrawalTimestamp,
                        isClaimable = request.isClaimable,
                    ),
                    isPending = false,
                    rawCurrencyId = null,
                ),
            )
        }

        if (availableToWithdraw > BigDecimal.ZERO) {
            add(
                StakingBalanceEntry(
                    id = "${vaultAddress}_withdrawable",
                    type = StakingEntryType.WITHDRAWABLE,
                    amount = availableToWithdraw,
                    validator = ValidatorInfo(address = vaultAddress, name = vaultName),
                    date = null,
                    actions = StakingEntryActions.P2PEthPool(
                        ticket = null,
                        estimatedWithdrawalDate = null,
                        isClaimable = true,
                    ),
                    isPending = false,
                    rawCurrencyId = null,
                ),
            )
        }
    }
}