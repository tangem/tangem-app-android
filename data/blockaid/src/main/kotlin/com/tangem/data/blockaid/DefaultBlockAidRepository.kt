package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.tangem.datasource.api.common.blockaid.BlockAidApi
import com.tangem.datasource.api.common.blockaid.models.request.DomainScanRequest
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultBlockAidRepository(
    private val api: BlockAidApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val mapper: BlockAidMapper,
) : BlockAidRepository {

    override suspend fun verifyDAppDomain(data: DAppData): CheckDAppResult {
        val response = withContext(dispatchers.io) {
            api.scanDomain(DomainScanRequest(data.url))
        }
        return mapper.mapToDomain(response)
    }

    override suspend fun verifyTransaction(data: TransactionData): CheckTransactionResult {
        return when (data.params) {
            is TransactionParams.Evm -> scanEvmTransaction(data = data)
            is TransactionParams.Solana -> scanSolanaTransaction(data = data)
        }
    }

    private suspend fun scanEvmTransaction(data: TransactionData): CheckTransactionResult =
        withContext(dispatchers.io) {
            val response = api.scanJsonRpc(mapper.mapToEvmRequest(data))
            mapper.mapToDomain(response)
        }

    private suspend fun scanSolanaTransaction(data: TransactionData): CheckTransactionResult =
        withContext(dispatchers.io) {
            val response = api.scanSolanaMessage(mapper.mapToSolanaRequest(data))
            mapper.mapToDomain(response)
        }
}