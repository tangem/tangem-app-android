package com.tangem.data.transaction.convertes

import com.tangem.datasource.api.gasless.models.GaslessTransactionRequest
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessTransactionData

/**
 * Builder for creating complete GaslessTransactionRequest from domain model.
 * Combines transaction data with signature and user information.
 */
class GaslessTransactionRequestBuilder(
    private val converter: GaslessTxDataToGaslessRequestConverter = GaslessTxDataToGaslessRequestConverter(),
    private val eip7702AuthConverter: Eip7702AuthorizationConverter = Eip7702AuthorizationConverter(),
) {

    /**
     * Creates complete gasless transaction request.
     *
     * @param gaslessTransaction domain model of transaction
     * @param signature transaction signature in hex format (with 0x prefix)
     * @param userAddress user's Ethereum address
     * @param chainId blockchain network chain ID
     * @param eip7702Auth optional EIP-7702 authorization for account abstraction
     * @return complete request ready for API submission
     */
    fun build(
        gaslessTransaction: GaslessTransactionData,
        signature: String,
        userAddress: String,
        chainId: Int,
        eip7702Auth: Eip7702Authorization? = null,
    ): GaslessTransactionRequest {
        return GaslessTransactionRequest(
            gaslessTransaction = converter.convert(gaslessTransaction),
            signature = signature,
            userAddress = userAddress,
            chainId = chainId,
            eip7702Auth = eip7702Auth?.let(eip7702AuthConverter::convert),
        )
    }
}