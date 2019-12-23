package com.tangem.blockchain.bitcoin

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.toCanonicalised
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import java.io.ByteArrayOutputStream
import java.math.BigInteger

class BitcoinTransactionBuilder(private val testNet: Boolean) {

    private lateinit var transaction: Transaction
    private var networkParameters: NetworkParameters? = null
    var unspentOutputs: List<UnspentTransaction> = listOf()

    fun calculateChange(transactionData: TransactionData) : Long {
        val fullAmount = unspentOutputs.map { it.amount }.sum()
        return fullAmount - (transactionData.amount.value!!.toLong() + (transactionData.fee?.value!!.toLong()))
    }

    fun buildToSign(
            transactionData: TransactionData): List<ByteArray> {

        val change: Long = calculateChange(transactionData)

        networkParameters = if (testNet) {
            NetworkParameters.fromID(NetworkParameters.ID_TESTNET)
        } else {
            NetworkParameters.fromID(NetworkParameters.ID_MAINNET)
        }
        transaction = transactionData.toBitcoinJTransaction(networkParameters, unspentOutputs, change)

        val hashesForSign: MutableList<ByteArray> = mutableListOf()
        for (input in transaction.inputs) {
            val index = input.index
            hashesForSign[index] = transaction.hashForSignature(index, input.scriptBytes, Transaction.SigHash.ALL, false).bytes
        }
        return hashesForSign
    }

    fun buildToSend(signedTransaction: ByteArray, publicKey: ByteArray): ByteArray {
        for (index in transaction.inputs.indices) {
            transaction.inputs[index].scriptSig = createScript(index, signedTransaction, publicKey)
        }
        val serializer = BitcoinSerializer(networkParameters, false)
        val outputStream = ByteArrayOutputStream()
        serializer.serialize(transaction, outputStream)
        return outputStream.toByteArray()
    }

    private fun createScript(index: Int, signedTransaction: ByteArray, publicKey: ByteArray): Script {
        val r = BigInteger(1, signedTransaction.copyOfRange(index * 64, 32 + index * 64));
        val s = BigInteger(1, signedTransaction.copyOfRange(32 + index * 64, 64 + index * 64));
        val signature = TransactionSignature(r, s.toCanonicalised())
        return ScriptBuilder.createInputScript(signature, ECKey.fromPublicOnly(publicKey))
    }
}

internal fun TransactionData.toBitcoinJTransaction(networkParameters: NetworkParameters?,
                                                   unspentOutputs: List<UnspentTransaction>,
                                                   change: Long): Transaction {
    val transaction = Transaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.hash), utxo.outputIndex, Script(utxo.outputScript))
    }
    transaction.addOutput(
            Coin.parseCoin(this.amount.value!!.toPlainString()),
            Address.fromString(networkParameters, this.destinationAddress))
    if (change != 0L) {
        transaction.addOutput(
                Coin.parseCoin(change.toString()),
                Address.fromString(networkParameters,
                        this.sourceAddress))
    }
    return transaction
}

class UnspentTransaction(
        val amount: Long,
        val outputIndex: Long,
        val hash: ByteArray,
        val outputScript: ByteArray
)