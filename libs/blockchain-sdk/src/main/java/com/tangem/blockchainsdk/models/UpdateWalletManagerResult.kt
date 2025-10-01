package com.tangem.blockchainsdk.models

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import java.math.BigDecimal

/** Result of updating wallet manager */
sealed class UpdateWalletManagerResult {

    /** Missed derivation */
    data object MissedDerivation : UpdateWalletManagerResult()

    /**
     * Unreachable result.
     * [selectedAddress] and [addresses] can be null if error is happened while getting available addresses.
     *
     * @property selectedAddress selected address
     * @property addresses       available addresses
     */
    data class Unreachable(
        val selectedAddress: String? = null,
        val addresses: Set<Address>? = null,
    ) : UpdateWalletManagerResult()

    /**
     * Verified
     *
     * @property selectedAddress     selected address
     * @property addresses           available addresses
     * @property currenciesAmounts   amounts of added crypto currencies
     * @property currentTransactions current transactions
     */
    data class Verified(
        val selectedAddress: String,
        val addresses: Set<Address>,
        val currenciesAmounts: Set<CryptoCurrencyAmount>,
        val currentTransactions: Set<CryptoCurrencyTransaction>,
    ) : UpdateWalletManagerResult()

    /**
     * No account
     *
     * @property selectedAddress       selected address
     * @property addresses             available addresses
     * @property amountToCreateAccount required amount to create account
     * @property errorMessage          error message
     */
    data class NoAccount(
        val selectedAddress: String,
        val addresses: Set<Address>,
        val amountToCreateAccount: BigDecimal,
        val errorMessage: String,
    ) : UpdateWalletManagerResult()

    /** Crypto currency amount */
    sealed interface CryptoCurrencyAmount {

        /** Represents amount as [BigDecimal] */
        val value: BigDecimal

        /**
         * Coin
         *
         * @property value amount value
         */
        data class Coin(override val value: BigDecimal) : CryptoCurrencyAmount

        sealed interface Token : CryptoCurrencyAmount {
            val currencyRawId: CryptoCurrency.RawID?
            val contractAddress: String

            /**
             * Basic Token
             *
             * @property currencyRawId   crypto currency id
             * @property contractAddress token contract address
             * @property value           amount value
             */
            data class BasicToken(
                override val value: BigDecimal,
                override val currencyRawId: CryptoCurrency.RawID?,
                override val contractAddress: String,
            ) : Token

            /**
             * Yield Supply Token
             *
             * @property currencyRawId      crypto currency id
             * @property contractAddress    token contract address
             * @property value              amount value
             * @property yieldSupplyStatus  status of the yield token
             */
            data class YieldSupplyToken(
                override val value: BigDecimal,
                override val currencyRawId: CryptoCurrency.RawID?,
                override val contractAddress: String,
                val yieldSupplyStatus: YieldSupplyStatus,
            ) : Token
        }
    }

    /** Crypto currency transaction */
    sealed interface CryptoCurrencyTransaction {

        /** Transaction info */
        val txInfo: TxInfo

        /**
         * Coin
         *
         * @property txInfo transaction info
         */
        data class Coin(override val txInfo: TxInfo) : CryptoCurrencyTransaction

        /**
         * Token
         *
         * @property txInfo          transaction info
         * @property tokenId         token unique identifier
         * @property contractAddress token contract address
         */
        data class Token(
            override val txInfo: TxInfo,
            val tokenId: String?,
            val contractAddress: String,
        ) : CryptoCurrencyTransaction
    }

    /**
     * Address
     *
     * @property value string representation of the address
     * @property type  address type
     */
    data class Address(val value: String, val type: Type) {

        enum class Type {
            Primary, Secondary,
        }
    }
}