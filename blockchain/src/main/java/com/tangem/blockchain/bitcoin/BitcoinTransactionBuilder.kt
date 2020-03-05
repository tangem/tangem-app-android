package com.tangem.blockchain.bitcoin

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.extensions.Result
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import java.math.BigDecimal
import java.math.BigInteger

class BitcoinTransactionBuilder(private val testNet: Boolean) {

    private lateinit var transaction: Transaction
    private var networkParameters: NetworkParameters? = null
    var unspentOutputs: List<UnspentTransaction>? = null

    fun buildToSign(
            transactionData: TransactionData): Result<List<ByteArray>> {

        if (unspentOutputs == null) return Result.Failure(Exception("Currently there's an unconfirmed transaction"))

        val change: BigDecimal = calculateChange(transactionData)

        networkParameters = if (testNet) {
            NetworkParameters.fromID(NetworkParameters.ID_TESTNET)
        } else {
            NetworkParameters.fromID(NetworkParameters.ID_MAINNET)
        }
        transaction = transactionData.toBitcoinJTransaction(networkParameters, unspentOutputs!!, change)

        val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
        for (input in transaction.inputs) {
            val index = input.index
            hashesForSign[index] = transaction.hashForSignature(index, input.scriptBytes, Transaction.SigHash.ALL, false).bytes
        }
        return Result.Success(hashesForSign)
    }

    private fun calculateChange(transactionData: TransactionData): BigDecimal {
        val fullAmount = unspentOutputs!!.map { it.amount }.reduce { acc, number -> acc + number }
        return fullAmount - (transactionData.amount.value!! + (transactionData.fee?.value
                ?: 0.toBigDecimal()))
    }

    fun buildToSend(signedTransaction: ByteArray, publicKey: ByteArray): ByteArray {
        for (index in transaction.inputs.indices) {
            transaction.inputs[index].scriptSig = createScript(index, signedTransaction, publicKey)
        }
        return transaction.bitcoinSerialize()
    }

    private fun createScript(index: Int, signedTransaction: ByteArray, publicKey: ByteArray): Script {
        val r = BigInteger(1, signedTransaction.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signedTransaction.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val signature = TransactionSignature(r, canonicalS)
        return ScriptBuilder.createInputScript(signature, ECKey.fromPublicOnly(publicKey))
    }

    fun getEstimateSize(transactionData: TransactionData, walletPublicKey: ByteArray): Result<Int> {
        val buildTransactionResult = buildToSign(transactionData)
        when (buildTransactionResult) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> {
                val hashes = buildTransactionResult.data
                val finalTransaction = buildToSend(ByteArray(64 * hashes.size) { 1 }, walletPublicKey)
                return Result.Success(finalTransaction.size)
            }
        }
    }
}

internal fun TransactionData.toBitcoinJTransaction(networkParameters: NetworkParameters?,
                                                   unspentOutputs: List<UnspentTransaction>,
                                                   change: BigDecimal): Transaction {
    val transaction = Transaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.hash), utxo.outputIndex, Script(utxo.outputScript))
    }
    transaction.addOutput(
            Coin.parseCoin(this.amount.value!!.toPlainString()),
            Address.fromString(networkParameters, this.destinationAddress))
    if (change != 0.toBigDecimal()) {
        transaction.addOutput(
                Coin.parseCoin(change.toPlainString()),
                Address.fromString(networkParameters,
                        this.sourceAddress))
    }
    return transaction
}

class UnspentTransaction(
        val amount: BigDecimal,
        val outputIndex: Long,
        val hash: ByteArray,
        val outputScript: ByteArray
)