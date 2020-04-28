package com.tangem.blockchain.blockchains.bitcoincash

import com.tangem.blockchain.blockchains.bitcoin.BitcoinTransactionBuilder
import com.tangem.blockchain.blockchains.bitcoin.BitcoinUnspentOutput
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.common.extensions.isZero
import org.bitcoinj.core.*
import org.bitcoinj.core.LegacyAddress.fromPubKeyHash
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import java.math.BigDecimal
import java.math.BigInteger

class BitcoinCashTransactionBuilder(private val walletPublicKey: ByteArray)
    : BitcoinTransactionBuilder(walletPublicKey) {

    private lateinit var transaction: BitcoinCashTransaction

    override fun buildToSign(
            transactionData: TransactionData): Result<List<ByteArray>> {

        if (unspentOutputs == null) return Result.Failure(Exception("Currently there's an unconfirmed transaction"))

        val change: BigDecimal = calculateChange(transactionData, unspentOutputs!!)

        networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET)
        transaction = transactionData.toBitcoinCashTransaction(networkParameters, unspentOutputs!!, change)

        val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
        for (input in transaction.inputs) {
            val index = input.index
            val value = Coin.parseCoin(unspentOutputs!![index].amount.toString())
            hashesForSign[index] = transaction.hashForSignatureWitness(index, input.scriptBytes, value, Transaction.SigHash.ALL, false).bytes
        }
        return Result.Success(hashesForSign)
    }

    override fun buildToSend(signedTransaction: ByteArray): ByteArray {
        for (index in transaction.inputs.indices) {
            transaction.inputs[index].scriptSig = createScript(index, signedTransaction, walletPublicKey)
        }
        return transaction.bitcoinSerialize()
    }

    override fun createScript(index: Int, signedTransaction: ByteArray, publicKey: ByteArray): Script {
        val r = BigInteger(1, signedTransaction.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signedTransaction.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s

        val sigHash = 0x41
        val signature = TransactionSignature(r, canonicalS, sigHash)
        return ScriptBuilder.createInputScript(signature, ECKey.fromPublicOnly(publicKey))
    }
}

internal fun TransactionData.toBitcoinCashTransaction(networkParameters: NetworkParameters?,
                                                      unspentOutputs: List<BitcoinUnspentOutput>,
                                                      change: BigDecimal): BitcoinCashTransaction {
    val transaction = BitcoinCashTransaction(networkParameters)
    for (utxo in unspentOutputs) {
        transaction.addInput(Sha256Hash.wrap(utxo.transactionHash), utxo.outputIndex, Script(utxo.outputScript))
    }
    val addressService = BitcoinCashAddressService()
    val sourceLegacyAddress =
            fromPubKeyHash(networkParameters, addressService.getPublicKeyHash(this.sourceAddress))
    val destinationLegacyAddress =
            fromPubKeyHash(networkParameters, addressService.getPublicKeyHash(this.destinationAddress))

    transaction.addOutput(
            Coin.parseCoin(this.amount.value!!.toPlainString()),
            destinationLegacyAddress
    )
    if (!change.isZero()) {
        transaction.addOutput(
                Coin.parseCoin(change.toPlainString()),
                sourceLegacyAddress
        )
    }
    return transaction
}