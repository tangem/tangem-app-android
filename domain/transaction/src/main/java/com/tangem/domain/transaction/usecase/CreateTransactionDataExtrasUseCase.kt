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
                        callData = CompiledSmartContractCallData(data.toCallDataBytes()),
                        network = network,
                        nonce = nonce,
                        gasLimit = gasLimit,
                    )
                },
            ) {
                raise(it)
            }
        }

    /**
     * Call data arrives as a hex string from an external service (Express `txData`). [hexToBytes] sizes the
     * result as `length / 2` and would silently drop the last nibble of an odd-length string, so truncated call
     * data would be signed and sent as if it were complete. Require `0x` + an even number of hex digits instead.
     */
    private fun String.toCallDataBytes(): ByteArray {
        val digits = removePrefix(HEX_PREFIX)
        require(digits.length % 2 == 0) { "Call data has an odd number of hex digits" }
        require(digits.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) { "Call data is not hex" }
        return digits.hexToBytes()
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

    private companion object {
        const val HEX_PREFIX = "0x"
    }
}