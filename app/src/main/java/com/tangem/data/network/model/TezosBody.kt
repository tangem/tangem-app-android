package com.tangem.data.network.model

data class TezosForgeBody(
        val branch: String,
        val contents: List<TezosOperationContent>
)

data class TezosOperationContent(
        val kind: String,
        val source: String,
        val fee: String,
        val counter: String,
        val gas_limit: String,
        val storage_limit: String,
        val public_key: String? = null,
        val destination: String? = null,
        val amount: String? = null
)

data class TezosPreapplyBody(
        val protocol: String,
        val branch: String,
        val contents: List<TezosOperationContent>,
        val signature: String
)