package com.tangem.domain.tokens.models.warnings

import com.tangem.domain.tokens.models.CryptoCurrency
import java.math.BigDecimal

sealed class CryptoCurrencyWarning {

    data class ExistentialDeposit(
        val currencyName: String,
        val edStringValueWithSymbol: String,
    ) : CryptoCurrencyWarning()

    data class BalanceNotEnoughForFee(
        val currency: CryptoCurrency,
        val blockchainFullName: String,
        val blockchainSymbol: String,
    ) : CryptoCurrencyWarning()

    object SomeNetworksUnreachable : CryptoCurrencyWarning()

    /**
     * Represents wallet blockchain rent
     * @param rent Amount that will be charged in overtime if the blockchain does not have an amount greater than
     * the [exemptionAmount]
     * @param exemptionAmount Amount that should be on the blockchain balance not to pay rent
     */
    data class Rent(val rent: BigDecimal, val exemptionAmount: BigDecimal) : CryptoCurrencyWarning()
}