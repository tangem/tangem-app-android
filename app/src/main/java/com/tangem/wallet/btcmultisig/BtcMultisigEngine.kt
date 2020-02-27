package com.tangem.wallet.btcmultisig

import com.google.common.primitives.UnsignedBytes
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.reader.CardProtocol.TangemException
import com.tangem.tangem_card.tasks.SignTask
import com.tangem.tangem_card.util.Util
import com.tangem.wallet.TangemContext
import com.tangem.wallet.btc.BtcEngine
import org.bitcoinj.core.*
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptBuilder
import java.math.BigInteger

class BtcMultisigEngine : BtcEngine {
    val networkParameters = MainNetParams.get()

    constructor(): super()

    constructor(context: TangemContext): super(context)

    override fun defineWallet() {
        try {
            val wallet = calculateAddress(ctx.card.walletPublicKeyRar)
            ctx.coinData.wallet = wallet
        } catch (e: Exception) {
            ctx.coinData.wallet = "ERROR"
            throw TangemException("Can't define wallet address")
        }
    }

    override fun calculateAddress(compressedPublicKey: ByteArray): String? {
        if (ctx.card.issuerData != null && ctx.card.issuerData.size == 33) {
            val script = createMultisigOutputScript()
            val scriptHash = Utils.sha256hash160(script.program)
            val address = LegacyAddress.fromScriptHash(networkParameters, scriptHash)
            return address.toBase58()
        } else {
            return Util.byteArrayToHexString(compressedPublicKey)
        }
    }

    override fun constructTransaction(amountValue: Amount, feeValue: Amount, IncFee: Boolean, targetAddress: String): SignTask.TransactionToSign {
        val fee = convertToInternalAmount(feeValue).longValueExact()
        var amount = convertToInternalAmount(amountValue).longValueExact()
        var change = coinData.balanceInInternalUnits.longValueExact() - amount

        if (IncFee) {
            amount -= fee
        } else {
            change -= fee
        }

        val transaction = Transaction(networkParameters)

        for (utxo in coinData.unspentTransactions) {
            transaction.addInput(
                    Sha256Hash.wrap(utxo.txID),
                    utxo.outputN.toLong(),
                    Script(Util.hexToBytes(utxo.script))
            )
        }

        transaction.addOutput(
                Coin.valueOf(amount),
                Address.fromString(networkParameters, targetAddress)
        )
        if (change != 0L) {
            transaction.addOutput(
                    Coin.valueOf(change),
                    Address.fromString(networkParameters, coinData.wallet)
            )
        }

        return object : SignTask.TransactionToSign {
            override fun isSigningMethodSupported(signingMethod: TangemCard.SigningMethod): Boolean {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash
            }

            override fun getHashesToSign(): Array<ByteArray> {
                val hashesForSign: MutableList<ByteArray> = MutableList(transaction.inputs.size) { byteArrayOf() }
                for (input in transaction.inputs) {
                    val index = input.index
                    val outputScript = createMultisigOutputScript()
                    hashesForSign[index] = transaction.hashForSignature(index, outputScript, Transaction.SigHash.ALL, false).bytes
                }
                return hashesForSign.toTypedArray()
            }

            @Throws(java.lang.Exception::class)
            override fun getRawDataToSign(): ByteArray {
                throw java.lang.Exception("Signing of raw transaction not supported for " + this.javaClass.simpleName)
            }

            override fun getHashAlgToSign(): String {
                return "sha-256x2"
            }

            @Throws(java.lang.Exception::class)
            override fun getIssuerTransactionSignature(dataToSignByIssuer: ByteArray): ByteArray {
                throw java.lang.Exception("Issuer validation not supported!")
            }


            override fun onSignCompleted(signFromCard: ByteArray): ByteArray {
                for (index in transaction.inputs.indices) {
                    transaction.inputs[index].scriptSig =
                            createMultisigInputScript(index, signFromCard)
                }
                val txForSend = transaction.bitcoinSerialize()
                notifyOnNeedSendTransaction(txForSend)
                return txForSend
            }
        }
    }

    private fun createMultisigInputScript(index: Int, signedTransaction: ByteArray): Script {
        val r = BigInteger(1, signedTransaction.copyOfRange(index * 64, 32 + index * 64))
        val s = BigInteger(1, signedTransaction.copyOfRange(32 + index * 64, 64 + index * 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val signature = TransactionSignature(r, canonicalS)

        val outputScript = createMultisigOutputScript()
        return ScriptBuilder.createP2SHMultiSigInputScript(mutableListOf(signature), outputScript)
    }

    private fun createMultisigOutputScript(): Script {
        val publicKeys =  mutableListOf(ctx.card.walletPublicKeyRar, ctx.card.issuerData)
        publicKeys.sortWith(UnsignedBytes.lexicographicalComparator())
        val publicEcKeys =
                MutableList(publicKeys.size) { i -> ECKey.fromPublicOnly(publicKeys[i]) }

        return Script(Script.createMultiSigOutputScript(1, publicEcKeys))
    }
}