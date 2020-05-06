package com.tangem.blockchain.blockchains.ethereum.network

class EthereumProvider(private val api: EthereumApi, private val apiKey: String) {
    suspend fun getBalance(address: String) = api.post(createEthereumBody(EthereumMethod.GET_BALANCE, address), apiKey)
    suspend fun getTokenBalance(address: String, contractAddress: String) = api.post(createEthereumBody(EthereumMethod.CALL, address, contractAddress), apiKey)
    suspend fun getTxCount(address: String) = api.post(createEthereumBody(EthereumMethod.GET_TRANSACTION_COUNT, address), apiKey)
    suspend fun getPendingTxCount(address: String) = api.post(createEthereumBody(EthereumMethod.GET_PENDING_COUNT, address), apiKey)
    suspend fun getGasPrice() = api.post(createEthereumBody(EthereumMethod.GAS_PRICE), apiKey)
    suspend fun sendTransaction(transaction: String) = api.post(createEthereumBody(EthereumMethod.SEND_RAW_TRANSACTION, transaction = transaction), apiKey)
}

private fun createEthereumBody(
        method: EthereumMethod,
        address: String? = null,
        contractAddress: String? = null,
        transaction: String? = null): EthereumBody {

    return when (method) {
        EthereumMethod.GET_BALANCE ->
            EthereumBody(method = EthereumMethod.GET_BALANCE.value, params = listOf(address ?: "", "latest"))
        EthereumMethod.GET_TRANSACTION_COUNT ->
            EthereumBody(method = EthereumMethod.GET_TRANSACTION_COUNT.value, params = listOf(address ?: "", "latest"))
        EthereumMethod.GET_PENDING_COUNT ->
            EthereumBody(method = EthereumMethod.GET_TRANSACTION_COUNT.value, params = listOf(address ?: "", "pending"))
        EthereumMethod.GAS_PRICE ->
            EthereumBody(method = EthereumMethod.GAS_PRICE.value)
        EthereumMethod.SEND_RAW_TRANSACTION ->
            EthereumBody(method = EthereumMethod.SEND_RAW_TRANSACTION.value, params = listOf(transaction ?: ""))
        EthereumMethod.CALL -> {
            EthereumBody(
                    method = EthereumMethod.CALL.value,
                    params = listOf(EthCallParams(
                            "0x70a08231000000000000000000000000" + address?.substring(2), contractAddress
                            ?: ""),
                            "latest"
                    ))
        }
    }
}