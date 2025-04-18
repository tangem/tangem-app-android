package com.domain.blockaid.models.transaction

/**
 * Input data for BlockAid's transaction check
 *
 * @property chain Chain name, for ex. "ethereum"
 * @property accountAddress The address of the account (wallet) received the request in hex string format
 * @property method Transaction method, for ex. "eth_signTransaction"
 * @property domainUrl Url of the DApp domain
 */
data class TransactionData(
    val chain: String,
    val accountAddress: String,
    val method: String,
    val domainUrl: String,
    val params: TransactionParams,
)

sealed class TransactionParams {

    /**
     * Parameters for Ethereum based transactions, can be taken from [WcSdkSessionRequest.JSONRPCRequest.params]
     */
    data class Evm(
        val params: String,
    ) : TransactionParams()

    /**
     * Parameters for Solana transactions
     *
     * @property transactions Base64-encoded serialized list of transactions
     */
    data class Solana(
        val transactions: List<String>,
    ) : TransactionParams()
}