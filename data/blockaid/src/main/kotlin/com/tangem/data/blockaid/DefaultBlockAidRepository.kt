package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.dapp.DAppData
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.GasEstimationResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.blockaid.converters.GasEstimationResponseConverter
import com.tangem.datasource.api.common.blockaid.BlockAidApi
import com.tangem.datasource.api.common.blockaid.models.request.DomainScanRequest
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import com.tangem.blockchain.common.TransactionData as SDKTransactionData

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
            is TransactionParams.Bitcoin -> scanBitcoinTransaction(data = data)
        }
    }

    override suspend fun getGasEstimation(
        cryptoCurrency: CryptoCurrency,
        transactionDataList: List<SDKTransactionData.Uncompiled>,
    ): GasEstimationResult {
        val blockchain = cryptoCurrency.network.toBlockchain()
        return when {
            blockchain.isEvm() -> scanEvmTransactionBulk(blockchain, transactionDataList)
            else -> error("Gas estimation with BlockAid not supported by ${blockchain.fullName}")
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

    @Suppress("UnusedParameter")
    private suspend fun scanBitcoinTransaction(data: TransactionData): CheckTransactionResult {
        // TODO: BlockAid API doesn't support Bitcoin transaction scanning yet
        // When support is added, implement: api.scanBitcoinTransaction(mapper.mapToBitcoinRequest(data))
        return CheckTransactionResult(
            validation = com.domain.blockaid.models.transaction.ValidationResult.FAILED_TO_VALIDATE,
            description = "Bitcoin transaction validation is not yet supported by BlockAid",
            simulation = com.domain.blockaid.models.transaction.SimulationResult.FailedToSimulate,
        )
    }

    private suspend fun scanEvmTransactionBulk(
        blockchain: Blockchain,
        transactionDataList: List<SDKTransactionData.Uncompiled>,
    ): GasEstimationResult = withContext(dispatchers.io) {
        val response = api.scanEvmTransactionBulk(
            BlockAidEvmScanTransactionConverter(blockchain).convert(transactionDataList),
        )

        GasEstimationResponseConverter.convert(response)
    }
}