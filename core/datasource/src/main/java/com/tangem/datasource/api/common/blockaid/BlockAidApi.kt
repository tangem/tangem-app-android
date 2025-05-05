package com.tangem.datasource.api.common.blockaid

import com.tangem.datasource.api.common.blockaid.models.request.DomainScanRequest
import com.tangem.datasource.api.common.blockaid.models.request.EvmTransactionScanRequest
import com.tangem.datasource.api.common.blockaid.models.request.SolanaTransactionScanRequest
import com.tangem.datasource.api.common.blockaid.models.response.DomainScanResponse
import com.tangem.datasource.api.common.blockaid.models.response.TransactionScanResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BlockAidApi {

    @POST("site/scan")
    suspend fun scanDomain(@Body request: DomainScanRequest): DomainScanResponse

    @POST("evm/json-rpc/scan")
    suspend fun scanJsonRpc(@Body request: EvmTransactionScanRequest): TransactionScanResponse

    @POST("solana/message/scan")
    suspend fun scanSolanaMessage(@Body request: SolanaTransactionScanRequest): TransactionScanResponse
}