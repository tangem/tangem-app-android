package com.tangem.blockchain.blockchains.bitcoin

import com.tangem.blockchain.blockchains.litecoin.LitecoinMainNetParams
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import java.math.BigDecimal
import java.math.BigInteger

open class BitcoinTransactionBuilder(
        private val walletPublicKey: ByteArray, blockchain: Blockchain
) {

    private lateinit var transaction: Transaction
    protected var networkParameters = when (blockchain) {
        Blockchain.Bitcoin, Blockchain.BitcoinCash -> MainNetParams()
        Blockchain.BitcoinTestnet -> TestNet3Params()
        Blockchain.Litecoin -> LitecoinMainNetParams()
        else -> throw Exception("${blockchain.fullName} blockchain is not supported by ${this::class.simpleName}")
    }
    var unspentOutputs: List<BitcoinUnspentOutput>? = null

    open fun buildToSign(
            transactionData: TransactionData): Result<List<ByteArray>> {

        if (unspentOutputs == null) return Result.Failure(Exception("Currently there's an unconfirmed transaction"))

        val change: BigDecimal = calculateChange(transactionData, unspentOutputs!!)

        transaction = transactionData.toBitcoinJTransaction(networkParameters, unspentOutputs!!, change)

        val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
        for (input in transaction.inputs) {
            val index = input.index
            hashesForSign[index] = transaction.hashForSignature(index, input.scriptBytes, Transaction.SigHash.ALL, false).bytes
        }
        return Result.Success(hashesForSign)
    }

    open fun buildToSend(signedTransaction: ByteArray): ByteArray {
        for (index in transaction.inputs.indices) {
            transaction.inputs[index].scriptSig = createScript(index, signedTransaction, walletPublicKey)
        }
        return transaction.bitcoinSerialize()
    }

    fun getEstimateSize(transactionData: TransactionData): Result<Int> {
        val buildTransactionResult = buildToSign(transactionData)
        when (buildTransactionResult) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> {
                val hashes = buildTransactionResult.data
                val finalTransaction = buildToSend(ByteArray(64 * hashes.size) { 1 })
                return Result.Success(finalTransaction.size)
            }
        }
    }

    fun calculateChange(transactionData: TransactionData, unspentOutputs: List<BitcoinUnspentOutput>): BigDecimal {
        val fullAmount = unspentOutputs!!.map { it.amount }.reduce { acc, number -> acc + number }
        return fullAmount - (transactionData.amount.value!! + (transactionData.fee?.value
                ?: 0.toBigDecimal()))
    }

    open fun createScript(index: Int, signedTransaction: ByteArray, publicKey: ByteArray): Script {
        val r = BigInteger(1, signedTransaction.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signedTransaction.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val signature = TransactionSignature(r, canonicalS)
        return ScriptBuilder.createInputScript(signature, ECKey.fromPublicOnly(publicKey))
    }
}

internal fun TransactionData.toBitcoinJTransaction(networkParameters: NetworkParameters?,
                                                   unspentOutputs: List<BitcoinUnspentOutput>,
                                                   change: BigDecimal): Transaction {
    val transaction = Transaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.transactionHash), utxo.outputIndex, Script(utxo.outputScript))
    }
    transaction.addOutput(
            Coin.parseCoin(this.amount.value!!.toPlainString()),
            Address.fromString(networkParameters, this.destinationAddress))
    if (!change.isZero()) {
        transaction.addOutput(
                Coin.parseCoin(change.toPlainString()),
                Address.fromString(networkParameters,
                        this.sourceAddress))
    }
    return transaction
}

class BitcoinUnspentOutput(
        val amount: BigDecimal,
        val outputIndex: Long,
        val transactionHash: ByteArray,
        val outputScript: ByteArray
)