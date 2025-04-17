package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import java.math.BigInteger

class CreateTransactionDataExtrasUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(data: String, network: Network, gasLimit: BigInteger? = null, nonce: BigInteger? = null) =
        Either.catch {
            requireNotNull(
                transactionRepository.createTransactionDataExtras(
                    callData = CompiledSmartContractCallData(data.hexToBytes()),
                    network = network,
                    nonce = nonce,
                    gasLimit = gasLimit,
                ),
            ) { "Failed to create transaction" }
        }
}