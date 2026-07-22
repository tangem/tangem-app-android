package com.tangem.data.transaction.convertes

import com.tangem.datasource.api.gasless.models.GaslessBatchTransactionDataDTO
import com.tangem.datasource.api.gasless.models.GaslessBatchTransactionRequest
import com.tangem.domain.transaction.models.Eip7702Authorization
import com.tangem.domain.transaction.models.GaslessBatchTransactionData

/**
 * Builder for creating complete [GaslessBatchTransactionRequest] from domain model.
 * Combines batch transaction data with signature and user information.
 *
 * Reuses [GaslessTxDataToGaslessRequestConverter] for transaction and fee conversion
 * to avoid duplicating mapping logic.
 */
class GaslessBatchTransactionRequestBuilder(
    private val converter: GaslessTxDataToGaslessRequestConverter = GaslessTxDataToGaslessRequestConverter(),
    private val eip7702AuthConverter: Eip7702AuthorizationConverter = Eip7702AuthorizationConverter(),
) {

    /**
     * Creates complete gasless batch transaction request.
     *
     * @param gaslessBatchTransaction domain model of batch transaction
     * @param signature transaction signature in hex format (with 0x prefix)
     * @param userAddress user's Ethereum address
     * @param chainId blockchain network chain ID
     * @param eip7702Auth optional EIP-7702 authorization for account abstraction
     * @return complete request ready for API submission
     */
    fun build(
        gaslessBatchTransaction: GaslessBatchTransactionData,
        signature: String,
        userAddress: String,
        chainId: Int,
        eip7702Auth: Eip7702Authorization? = null,
    ): GaslessBatchTransactionRequest {
        return GaslessBatchTransactionRequest(
            gaslessTransaction = GaslessBatchTransactionDataDTO(
                transactions = gaslessBatchTransaction.transactions.map { converter.convertTransaction(it) },
                fee = converter.convertFee(gaslessBatchTransaction.fee),
                nonce = gaslessBatchTransaction.nonce.toString(),
            ),
            signature = signature,
            userAddress = userAddress,
            chainId = chainId,
            eip7702Auth = eip7702Auth?.let(eip7702AuthConverter::convert),
        )
    }
}