package com.tangem.wallet.eos

import com.fasterxml.jackson.annotation.JsonInclude
import io.jafka.jeos.core.common.transaction.TransactionAction
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class EosPackedTransaction(
        var expiration: String? = null,//"2018-08-30T02:30:49"
        var refBlockNum: Long? = null,
        var refBlockPrefix: Long? = null,

        var maxNetUsageWords: Int? = null,
        var maxCpuUsageMs: Int? = null,
        var delaySec: Int? = null,
        var contextFreeActions: ArrayList<TransactionAction> = ArrayList<TransactionAction>(),
        var actions: List<TransactionAction> = ArrayList<TransactionAction>(),

        var transactionExtensions: ArrayList<String> = ArrayList<String>(),
        //private List<String> signatures;
        var contextFreeData: ArrayList<String> = ArrayList<String>(),

        //
        var region: String? = null
)