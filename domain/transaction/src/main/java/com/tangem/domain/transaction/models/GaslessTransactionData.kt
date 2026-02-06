package com.tangem.domain.transaction.models

import java.math.BigInteger

/**
 * Domain model for gasless transaction.
 * Represents complete transaction data with fee delegation metadata.
 */
data class GaslessTransactionData(
    /** Transaction details */
    val transaction: Transaction,
    /** Fee payment configuration */
    val fee: Fee,
    /** Nonce from user's contract */
    val nonce: BigInteger,
) {

    /**
     * Core transaction data.
     *
     * @property to destination address
     * @property value transaction value in wei (currently always 0 for gasless)
     * @property data encoded transaction data (contract call)
     */
    data class Transaction(
        val to: String,
        val value: BigInteger,
        val data: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Transaction

            if (to != other.to) return false
            if (value != other.value) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = to.hashCode()
            result = 31 * result + value.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * Fee configuration for gasless transaction.
     *
     * @property feeToken token address used for fee payment
     * @property maxTokenFee maximum fee amount in fee token units
     * @property coinPriceInToken price of native coin in fee token units
     * @property feeTransferGasLimit gas limit for fee token transfer
     * @property baseGas base gas cost (currently constant BASE_GAS, may vary in future)
     * @property feeReceiver address receiving the fee payment
     */
    data class Fee(
        val feeToken: String,
        val maxTokenFee: BigInteger,
        val coinPriceInToken: BigInteger,
        val feeTransferGasLimit: BigInteger,
        val baseGas: BigInteger,
        val feeReceiver: String,
    )
}