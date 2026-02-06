package com.tangem.common

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.utils.extensions.orZero
import java.math.BigDecimal

/**
 * Calculates the total fiat amount by adding the main fiat amount and the fiat value of the staked balance.
 */
fun CryptoCurrencyStatus.getTotalFiatAmount(): BigDecimal? {
    val fiatAmount = value.fiatAmount

    val fiatStakedBalance = value.fiatRate?.times(getStakedBalance().orZero()) ?: return fiatAmount
    val totalAmount = fiatAmount?.plus(fiatStakedBalance) ?: return fiatStakedBalance

    return totalAmount
}

/**
 * Calculates the total cryptocurrency amount by adding the main crypto amount and the staked balance.
 */
fun CryptoCurrencyStatus.getTotalCryptoAmount(): BigDecimal? {
    val cryptoAmount = value.amount

    val cryptoStakedBalance = getStakedBalance() ?: return cryptoAmount
    val totalAmount = cryptoAmount?.plus(cryptoStakedBalance) ?: return cryptoStakedBalance

    return totalAmount
}

private fun CryptoCurrencyStatus.getStakedBalance() = (value.stakingBalance as? StakingBalance.Data)
    ?.getTotalWithRewardsStakingBalance(blockchainId = currency.network.rawId)