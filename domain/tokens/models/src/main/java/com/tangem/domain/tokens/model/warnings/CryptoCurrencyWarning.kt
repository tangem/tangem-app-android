package com.tangem.domain.tokens.model.warnings

import com.tangem.domain.tokens.model.CryptoCurrency
import java.math.BigDecimal

sealed class CryptoCurrencyWarning {

    data class ExistentialDeposit(
        val currencyName: String,
        val edStringValueWithSymbol: String,
    ) : CryptoCurrencyWarning()

    data class BalanceNotEnoughForFee(
        val tokenCurrency: CryptoCurrency,
        val coinCurrency: CryptoCurrency,
    ) : CryptoCurrencyWarning()

    data class CustomTokenNotEnoughForFee(
        val currency: CryptoCurrency,
        val feeCurrency: CryptoCurrency?,
        val networkName: String,
        val feeCurrencyName: String,
        val feeCurrencySymbol: String,
    ) : CryptoCurrencyWarning()

    object SomeNetworksUnreachable : CryptoCurrencyWarning()

    data class SomeNetworksNoAccount(
        val amountToCreateAccount: BigDecimal,
        val amountCurrency: CryptoCurrency,
    ) : CryptoCurrencyWarning()

    /**
     * Represents wallet blockchain rent
     * @param rent Amount that will be charged in overtime if the blockchain does not have an amount greater than
     * the [exemptionAmount]
     * @param exemptionAmount Amount that should be on the blockchain balance not to pay rent
     */
    data class Rent(val rent: BigDecimal, val exemptionAmount: BigDecimal) : CryptoCurrencyWarning()

    data class HasPendingTransactions(val blockchainSymbol: String) : CryptoCurrencyWarning()

    object SwapPromo : CryptoCurrencyWarning()
}
