package com.tangem.blockchain.blockchains.tezos

import com.tangem.blockchain.blockchains.tezos.TezosAddressService.Companion.calculateTezosChecksum
import com.tangem.blockchain.blockchains.tezos.network.TezosOperationContent
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.bigIntegerValue
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import org.bitcoinj.core.Base58
import org.spongycastle.jcajce.provider.digest.Blake2b

class TezosTransactionBuilder(private val walletPublicKey: ByteArray) {
    var counter: Long? = null

    fun buildContents(transactionData: TransactionData,
                      publicKeyRevealed: Boolean
    ): Result<List<TezosOperationContent>> {
        if (counter == null) return Result.Failure(Exception("counter is null"))
        var counter = counter!!

        val contents = arrayListOf<TezosOperationContent>()

        if (!publicKeyRevealed) {
            counter++
            val revealOp = TezosOperationContent(
                    kind = "reveal",
                    source = transactionData.sourceAddress,
                    fee = "1300",
                    counter = counter.toString(),
                    gas_limit = "10000",
                    storage_limit = "0",
                    public_key = encodePublicKey(walletPublicKey)
            )
            contents.add(revealOp)
        }

        counter++
        val transactionOp = TezosOperationContent(
                kind = "transaction",
                source = transactionData.sourceAddress,
                fee = "1350",
                counter = counter.toString(),
                gas_limit = "10600",
                storage_limit = "277",
                destination = transactionData.destinationAddress,
                amount = transactionData.amount.bigIntegerValue().toString()
        )
        contents.add(transactionOp)

        return Result.Success(contents)
    }

    fun buildToSign(forgedContents: String): ByteArray {
        val genericOperationWatermark = "03"
        return Blake2b.Blake2b256().digest((genericOperationWatermark + forgedContents).hexToBytes())
    }

    fun buildToSend(signature: ByteArray, forgedContents: String) = forgedContents + signature.toHexString()

    private fun encodePublicKey(pkUncompressed: ByteArray): String {
        val edpkPrefix = "0D0F25D9".hexToBytes()
        val prefixedPubKey = edpkPrefix + pkUncompressed

        val checksum = prefixedPubKey.calculateTezosChecksum()
        val prefixedHashWithChecksum = prefixedPubKey + checksum

        return Base58.encode(prefixedHashWithChecksum)
    }
}