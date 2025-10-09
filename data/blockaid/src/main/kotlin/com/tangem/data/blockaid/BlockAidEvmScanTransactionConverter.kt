package com.tangem.data.blockaid

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.common.blockaid.models.request.BlockAidScanOptions
import com.tangem.datasource.api.common.blockaid.models.request.Data
import com.tangem.datasource.api.common.blockaid.models.request.EvmTransactionBulkScanRequest
import com.tangem.datasource.api.common.blockaid.models.response.TransactionMetadata
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.TransactionData as SDKTransactionData

internal class BlockAidEvmScanTransactionConverter(
    private val blockchain: Blockchain,
) : Converter<List<SDKTransactionData.Uncompiled>, EvmTransactionBulkScanRequest> {

    @Suppress("NullableToStringCall")
    override fun convert(value: List<SDKTransactionData.Uncompiled>): EvmTransactionBulkScanRequest {
        return EvmTransactionBulkScanRequest(
            chain = blockchain.getChainId().toString(),
            options = listOf(BlockAidScanOptions.GasEstimation.value),
            metadata = TransactionMetadata(domain = "https://tangem.com"),
            data = value.map { transactionData ->
                Data(
                    from = transactionData.sourceAddress,
                    to = transactionData.destinationAddress,
                    data = (transactionData.extras as? EthereumTransactionExtras)?.callData?.dataHex.orEmpty(),
                )
            },
            aggregated = false,
        )
    }
}