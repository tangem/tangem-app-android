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
        val response = withContext(dispatchers.io) {
            when (data.params) {
                is TransactionParams.Evm -> {
                    api.scanJsonRpc(mapper.mapToEvmRequest(data))
                }
                is TransactionParams.Solana -> {
                    api.scanSolanaMessage(mapper.mapToSolanaRequest(data))
                }
            }
        }
        return mapper.mapToDomain(response)
    }
}