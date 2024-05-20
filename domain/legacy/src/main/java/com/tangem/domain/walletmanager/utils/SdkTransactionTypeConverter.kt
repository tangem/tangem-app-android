package com.tangem.domain.walletmanager.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem
import com.tangem.datasource.asset.reader.AssetReader
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter

internal class SdkTransactionTypeConverter(
    private val assetReader: AssetReader,
    private val moshi: Moshi,
) : Converter<TransactionHistoryItem.TransactionType, TxHistoryItem.TransactionType> {

    private val adapter: JsonAdapter<Map<String, SmartContractMethod>> by lazy {
        moshi.adapter(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                SmartContractMethod::class.java,
            ),
        )
    }
    private val smartContractMethods by lazy { readSmartContractMethods() }

    override fun convert(value: TransactionHistoryItem.TransactionType): TxHistoryItem.TransactionType {
        return when (value) {
            is TransactionHistoryItem.TransactionType.ContractMethod ->
                getTransactionType(methodName = smartContractMethods[value.id]?.name)
            is TransactionHistoryItem.TransactionType.ContractMethodName -> getTransactionType(methodName = value.name)
            is TransactionHistoryItem.TransactionType.Transfer -> TxHistoryItem.TransactionType.Transfer
        }
    }

    private fun getTransactionType(methodName: String?): TxHistoryItem.TransactionType {
        return when (methodName) {
            "transfer" -> TxHistoryItem.TransactionType.Transfer
            "approve" -> TxHistoryItem.TransactionType.Approve
            "swap" -> TxHistoryItem.TransactionType.Swap
            null -> TxHistoryItem.TransactionType.UnknownOperation
            else -> TxHistoryItem.TransactionType.Operation(name = methodName.replaceFirstChar { it.titlecase() })
        }
    }

    private fun readSmartContractMethods(): Map<String, SmartContractMethod> {
        val json = assetReader.readJson("contract_methods")
        return adapter.fromJson(json) ?: emptyMap()
    }
}