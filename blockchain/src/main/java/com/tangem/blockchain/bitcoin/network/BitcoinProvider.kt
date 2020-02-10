package com.tangem.blockchain.bitcoin.network

import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult

interface BitcoinProvider {
    suspend fun getInfo(address: String): Result<BitcoinAddressResponse>
    suspend fun getFee(): Result<BitcoinFee>
    suspend fun sendTransaction(transaction: String): SimpleResult
}