package com.tangem.common

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.totalContributionsDeltaOrNull
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

/**
 * Crypto amount this currency holds **outside** its network balance, or `null` when it holds none.
 *
 * Answers from [CryptoCurrencyStatus.Value.contributions], which `CryptoCurrencyStatusFactory` fills. The
 * legacy staking-only fallback stays for statuses built by producers that do not report contributions yet.
 *
 * Returning `null` for "nothing outside the network balance" (rather than zero) is load-bearing: callers return
 * the bare — possibly `null` — amount in that case, and a zero would turn a missing amount into `0`.
 */
fun CryptoCurrencyStatus.getExtraBalanceOrNull(): BigDecimal? {
    return value.totalContributionsDeltaOrNull()
        ?: (value.stakingBalance as? StakingBalance.Data)
            ?.getTotalWithRewardsStakingBalance(blockchainId = currency.network.rawId)
}

/**
 * Calculates the total fiat amount by adding the main fiat amount and the fiat value of the staked balance.
 */
fun CryptoCurrencyStatus.getTotalFiatAmount(): BigDecimal? {
    val fiatAmount = value.fiatAmount

    val fiatStakedBalance = value.fiatRate?.times(getExtraBalanceOrNull().orZero()) ?: return fiatAmount
    val totalAmount = fiatAmount?.plus(fiatStakedBalance) ?: return fiatStakedBalance

    return totalAmount
}

/**
 * Calculates the total cryptocurrency amount by adding the main crypto amount and the staked balance.
 */
fun CryptoCurrencyStatus.getTotalCryptoAmount(): BigDecimal? {
    val cryptoAmount = value.amount

    val cryptoStakedBalance = getExtraBalanceOrNull() ?: return cryptoAmount
    val totalAmount = cryptoAmount?.plus(cryptoStakedBalance) ?: return cryptoStakedBalance

    return totalAmount
}