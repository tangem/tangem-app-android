package com.tangem.common

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.totalContributionsDeltaOrNull
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

/**
 * Crypto amount this currency holds **outside** its network balance, or `null` when it holds none.
 *
 * This is the single point where the balance-contributions migration is gated. When
 * `TWI_1717_BALANCE_CONTRIBUTIONS` is on, `CryptoCurrencyStatusFactory` fills
 * [CryptoCurrencyStatus.Value.contributions] and the generic sum is used; when it is off the list stays empty and
 * the legacy staking-only path answers instead. Both branches produce the same number — see
 * `StakingBalanceExtTest.ContributionParity`.
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