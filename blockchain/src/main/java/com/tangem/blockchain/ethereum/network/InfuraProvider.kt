package com.tangem.blockchain.ethereum.network

class InfuraProvider(private val api: InfuraApi) {
    suspend fun getBalance(address: String) = api.postToInfura(createInfuraBody(InfuraMethod.GET_BALANCE, address))
    suspend fun getTokenBalance(address: String, contractAddress: String) = api.postToInfura(createInfuraBody(InfuraMethod.CALL, address, contractAddress))
    suspend fun getTxCount(address: String) = api.postToInfura(createInfuraBody(InfuraMethod.GET_TRANSACTION_COUNT, address))
    suspend fun getPendingTxCount(address: String) = api.postToInfura(createInfuraBody(InfuraMethod.GET_PENDING_COUNT, address))
    suspend fun getGasPrice() = api.postToInfura(createInfuraBody(InfuraMethod.GAS_PRICE))
    suspend fun sendTransaction(transaction: String) = api.postToInfura(createInfuraBody(InfuraMethod.SEND_RAW_TRANSACTION, transaction = transaction))
}

private fun createInfuraBody(
        method: InfuraMethod,
        address: String? = null,
        contractAddress: String? = null,
        transaction: String? = null): InfuraBody {

    return when (method) {
        InfuraMethod.GET_BALANCE ->
            InfuraBody(method = InfuraMethod.GET_BALANCE.value, params = listOf(address ?: "", "latest"))
        InfuraMethod.GET_TRANSACTION_COUNT ->
            InfuraBody(method = InfuraMethod.GET_TRANSACTION_COUNT.value, params = listOf(address ?: "", "latest"))
        InfuraMethod.GET_PENDING_COUNT ->
            InfuraBody(method = InfuraMethod.GET_TRANSACTION_COUNT.value, params = listOf(address ?: "", "pending"))
        InfuraMethod.GAS_PRICE ->
            InfuraBody(method = InfuraMethod.GAS_PRICE.value)
        InfuraMethod.SEND_RAW_TRANSACTION ->
            InfuraBody(method = InfuraMethod.SEND_RAW_TRANSACTION.value, params = listOf(transaction ?: ""))
        InfuraMethod.CALL -> {
            InfuraBody(
                    method = InfuraMethod.CALL.value,
                    params = listOf(EthCallParams(
                            "0x70a08231000000000000000000000000" + address?.substring(2), contractAddress
                            ?: ""),
                            "latest"
                    ))
        }
    }
}