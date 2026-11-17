package com.tangem.data.blockaid

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.store.datasource.blockaid.models.request.BlockAidScanOptions
import com.tangem.store.datasource.blockaid.models.request.Data
import com.tangem.store.datasource.blockaid.models.request.EvmTransactionBulkScanRequest
import com.tangem.store.datasource.blockaid.models.response.TransactionMetadata
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
                    value = transactionData.coinValueHex(),
                )
            },
            aggregated = false,
        )
    }

    /** A payable call carries the coin it sends: without it the simulation reverts */
    private fun SDKTransactionData.Uncompiled.coinValueHex(): String? {
        if (amount.type != AmountType.Coin) return null
        val wei = amount.bigIntegerValue()?.takeIf { it.signum() > 0 } ?: return null
        return HEX_PREFIX + wei.toString(HEX_RADIX)
    }

    private companion object {
        const val HEX_RADIX = 16
    }
}