package com.tangem.domain.transaction.usecase.gasless

/**
 * EIP-712 schema identifiers of the `Tangem7702GaslessExecutor` contract.
 *
 * A signature over this schema with `verifyingContract` = the user's EIP-7702-delegated account authorizes
 * arbitrary calls from that account, so only [Eip712TypedDataBuilder] may produce such payloads; external
 * signing entry points (e.g. WalletConnect) must reject them.
 */
object GaslessTypedDataSchema {
    const val DOMAIN_NAME = "Tangem7702GaslessExecutor"
    const val DOMAIN_VERSION = "1"
    const val PRIMARY_TYPE = "GaslessTransaction"
    const val PRIMARY_TYPE_BATCH = "GaslessBatchTransaction"

    val primaryTypes: Set<String> = setOf(PRIMARY_TYPE, PRIMARY_TYPE_BATCH)
}