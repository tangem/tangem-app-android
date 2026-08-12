package com.tangem.domain.staking.model

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.contributionOrNull
import com.tangem.domain.models.staking.StakingBalance

/**
 * Staking balance of this currency, or `null` when it has none.
 */
val CryptoCurrencyStatus.Value.stakingBalanceData: StakingBalance.Data?
    get() = contributionOrNull<StakingBalance.Data>() ?: stakingBalance as? StakingBalance.Data

/**
 * The source half of [stakingBalanceData].
 */
val CryptoCurrencyStatus.Value.stakingSource: StatusSource
    get() = stakingBalanceData?.source ?: sources.stakingBalanceSource