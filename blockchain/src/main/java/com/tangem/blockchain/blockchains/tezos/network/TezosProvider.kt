package com.tangem.blockchain.blockchains.tezos.network

import com.tangem.blockchain.blockchains.tezos.TezosAddressService.Companion.calculateTezosChecksum
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.retryIO
import com.tangem.common.extensions.hexToBytes
import org.bitcoinj.core.Base58

class TezosProvider(private val api: TezosApi) {
    private val decimals = Blockchain.Tezos.decimals()

    suspend fun getInfo(address: String): Result<TezosInfoResponse> {
        return try {
                val addressData = retryIO { api.getAddressData(address) }
                Result.Success(TezosInfoResponse(
                        balance = addressData.balance!!.toBigDecimal().movePointLeft(decimals),
                        counter = addressData.counter!!
                ))
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    suspend fun isPublicKeyRevealed(address: String): Result<Boolean> {
        return try {
            retryIO { api.getManagerKey(address) }
            Result.Success(true)
        } catch (exception: Exception) { //TODO: check exception
            Result.Success(false)
        }
    }

    suspend fun getHeader(): Result<TezosHeader> {
        return try {
            val headerResponse = retryIO { api.getHeader() }
            Result.Success(TezosHeader(
                    hash = headerResponse.hash!!,
                    protocol = headerResponse.protocol!!
            ))
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    suspend fun forgeContents(headerHash: String, contents: List<TezosOperationContent>): Result<String> {
        return try {
            val forgedContents = retryIO { api.forgeOperations(TezosForgeBody(headerHash, contents)) }
            Result.Success(forgedContents)
        } catch (exception: Exception) {
            Result.Failure(exception)
        }
    }

    suspend fun checkTransaction(
            header: TezosHeader,
            contents: List<TezosOperationContent>,
            signature: ByteArray
    ): SimpleResult {
        return try {
            val tezosPreapplyBody = TezosPreapplyBody(
                    protocol = header.protocol,
                    branch = header.hash,
                    contents = contents,
                    signature = encodeSignature(signature)
            )
            retryIO { api.preapplyOperations(listOf(tezosPreapplyBody)) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception)
        }
    }

    suspend fun sendTransaction(transaction: String): SimpleResult {
        return try {
            retryIO { api.sendTransaction(transaction) }
            SimpleResult.Success
        } catch (exception: Exception) {
            SimpleResult.Failure(exception)
        }
    }

    private fun encodeSignature(signature: ByteArray): String {
        val edsigPrefix = "09F5CD8612".hexToBytes()
        val prefixedSignature = edsigPrefix + signature
        val checksum = prefixedSignature.calculateTezosChecksum()
        val prefixedSignatureWithChecksum = prefixedSignature + checksum

        return Base58.encode(prefixedSignatureWithChecksum)
    }
}
