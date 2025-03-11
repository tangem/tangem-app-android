package com.tangem.domain.tokens.model.warnings

import com.tangem.domain.tokens.model.CryptoCurrency
import org.joda.time.DateTime
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

    data object SomeNetworksUnreachable : CryptoCurrencyWarning()

    data class SomeNetworksNoAccount(
        val amountToCreateAccount: BigDecimal,
        val amountCurrency: CryptoCurrency,
    ) : CryptoCurrencyWarning()

    data object TopUpWithoutReserve : CryptoCurrencyWarning()

    /**
     * Represents wallet blockchain rent
     * @param rent Amount that will be charged in overtime if the blockchain does not have an amount greater than
     * the [exemptionAmount]
     * @param exemptionAmount Amount that should be on the blockchain balance not to pay rent
     */
    data class Rent(val rent: BigDecimal, val exemptionAmount: BigDecimal) : CryptoCurrencyWarning()

    data class SwapPromo(
        val startDateTime: DateTime,
        val endDateTime: DateTime,
    ) : CryptoCurrencyWarning()

    data object BeaconChainShutdown : CryptoCurrencyWarning()

    data object MigrationMaticToPol : CryptoCurrencyWarning()

    /**
     * Shows a warning about an available fee resource for a transaction in several blockchains (ex. Koinos)
     */
    data class FeeResourceInfo(
        val amount: BigDecimal,
        val maxAmount: BigDecimal?,
    ) : CryptoCurrencyWarning()

    data object TokensInBetaWarning : CryptoCurrencyWarning()

    data object UsedOutdatedDataWarning : CryptoCurrencyWarning()
}