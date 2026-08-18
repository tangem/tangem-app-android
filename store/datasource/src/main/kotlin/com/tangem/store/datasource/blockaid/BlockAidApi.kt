package com.tangem.store.datasource.blockaid

import com.tangem.store.datasource.blockaid.models.request.DomainScanRequest
import com.tangem.store.datasource.blockaid.models.request.EvmTransactionBulkScanRequest
import com.tangem.store.datasource.blockaid.models.request.EvmTransactionScanRequest
import com.tangem.store.datasource.blockaid.models.request.SolanaTransactionScanRequest
import com.tangem.store.datasource.blockaid.models.response.DomainScanResponse
import com.tangem.store.datasource.blockaid.models.response.GasEstimationResponse
import com.tangem.store.datasource.blockaid.models.response.SolanaTransactionResponse
import com.tangem.store.datasource.blockaid.models.response.TransactionScanResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface BlockAidApi {

    @POST("site/scan")
    suspend fun scanDomain(@Body request: DomainScanRequest): DomainScanResponse

    @POST("evm/json-rpc/scan")
    suspend fun scanJsonRpc(@Body request: EvmTransactionScanRequest): TransactionScanResponse

    @POST("solana/message/scan")
    suspend fun scanSolanaMessage(@Body request: SolanaTransactionScanRequest): SolanaTransactionResponse

    @POST("evm/transaction-bulk/scan")
    suspend fun scanEvmTransactionBulk(@Body request: EvmTransactionBulkScanRequest): List<GasEstimationResponse>
}