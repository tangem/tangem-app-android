package com.tangem.domain.transaction.usecase

import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.TransactionRepository
import java.math.BigInteger

class CreateTransactionDataExtrasUseCase(
    private val transactionRepository: TransactionRepository,
) {

    operator fun invoke(data: String, network: Network, gasLimit: BigInteger? = null, nonce: BigInteger? = null) =
        either {
            catch(
                {
                    transactionRepository.createTransactionDataExtras(
                        callData = CompiledSmartContractCallData(data.hexToBytes()),
                        network = network,
                        nonce = nonce,
                        gasLimit = gasLimit,
                    )
                },
            ) {
                raise(it)
            }
        }

    operator fun invoke(
        callData: SmartContractCallData,
        network: Network,
        gasLimit: BigInteger? = null,
        nonce: BigInteger? = null,
    ) = either {
        catch(
            {
                transactionRepository.createTransactionDataExtras(
                    callData = callData,
                    network = network,
                    nonce = nonce,
                    gasLimit = gasLimit,
                )
            },
        ) {
            raise(it)
        }
    }
}