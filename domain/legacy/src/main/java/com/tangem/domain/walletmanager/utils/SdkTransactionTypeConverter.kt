package com.tangem.domain.walletmanager.utils

import android.content.res.AssetManager
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.blockchain.common.txhistory.TransactionHistoryItem
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.SmartContractMethod
import com.tangem.utils.converter.Converter
import okio.buffer
import okio.source

class SdkTransactionTypeConverter(
    private val assetManager: AssetManager,
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
            TransactionHistoryItem.TransactionType.Transfer -> TxHistoryItem.TransactionType.Transfer
            is TransactionHistoryItem.TransactionType.ContractMethod -> {
                return when (val name = smartContractMethods[value.id]?.name) {
                    "transfer" -> TxHistoryItem.TransactionType.Transfer
                    "approve" -> TxHistoryItem.TransactionType.Approve
                    "swap" -> TxHistoryItem.TransactionType.Swap
                    null -> TxHistoryItem.TransactionType.UnknownOperation
                    else -> TxHistoryItem.TransactionType.Operation(name = name.replaceFirstChar { it.titlecase() })
                }
            }
        }
    }

    private fun readSmartContractMethods(): Map<String, SmartContractMethod> {
        return assetManager.open("contract_methods.json").use { stream ->
            adapter.fromJson(stream.source().buffer())
        } ?: emptyMap()
    }
}