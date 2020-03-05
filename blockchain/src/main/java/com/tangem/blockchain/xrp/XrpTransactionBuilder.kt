package com.tangem.blockchain.xrp

import com.ripple.core.coretypes.AccountID
import com.ripple.core.coretypes.Amount
import com.ripple.core.coretypes.uint.UInt32
import com.ripple.crypto.ecdsa.ECDSASignature
import com.ripple.utils.HashUtils
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.extensions.bigIntegerValue
import com.tangem.blockchain.xrp.override.XrpPayment
import com.tangem.blockchain.xrp.override.XrpSignedTransaction
import org.bitcoinj.core.ECKey
import java.math.BigInteger

class XrpTransactionBuilder(walletPublicKey: ByteArray) {
    private val canonicalPublicKey = XrpAddressFactory.canonizePublicKey(walletPublicKey)
    var sequence: Long? = null
    private var transaction: XrpSignedTransaction? = null

    fun buildToSign(transactionData: TransactionData): ByteArray {
        val payment = XrpPayment()
        payment.putTranslated(AccountID.Account, transactionData.sourceAddress)
        payment.putTranslated(AccountID.Destination, transactionData.destinationAddress)
        payment.putTranslated(Amount.Amount, transactionData.amount.bigIntegerValue().toString())
        payment.putTranslated(UInt32.Sequence, sequence)
        payment.putTranslated(Amount.Fee, transactionData.fee!!.bigIntegerValue().toString())

        transaction = payment.prepare(canonicalPublicKey)

        return if (canonicalPublicKey[0] == 0xED.toByte()) {
            transaction!!.signingData
        } else {
            HashUtils.halfSha512(transaction!!.signingData)
        }
    }

    fun buildToSend(signature: ByteArray): String {
        if (canonicalPublicKey[0] == 0xED.toByte()) {
            transaction!!.addSign(signature)
        } else {
            val derSignature = encodeDerSignature(signature)
            transaction!!.addSign(derSignature)
        }
        return transaction!!.tx_blob
    }

    private fun encodeDerSignature(signature: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s
        val ecdsaSignature = ECDSASignature(r, canonicalS)
        return ecdsaSignature.encodeToDER()
    }
}