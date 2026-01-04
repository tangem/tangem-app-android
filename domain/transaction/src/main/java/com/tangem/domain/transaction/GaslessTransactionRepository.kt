package com.tangem.domain.transaction

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import java.math.BigInteger

interface GaslessTransactionRepository {

    fun isNetworkSupported(network: Network): Boolean

    fun getSupportedTokens(): Set<CryptoCurrency>

    fun getTokenFeeReceiverAddress(): String

    /**
     * Hardcoded value as baseGas
     */
    fun getBaseGasForTransaction(): BigInteger
}