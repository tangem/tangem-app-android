package com.tangem.wallet.tokenEmv

import android.text.InputFilter
import android.util.Log
import com.google.gson.Gson
import com.tangem.data.Blockchain
import com.tangem.data.network.ServerApiTokenEmv
import com.tangem.data.network.model.TokenEmvTransferBody
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.reader.CardProtocol.TangemException
import com.tangem.tangem_card.tasks.SignTask
import com.tangem.tangem_card.util.Util
import com.tangem.util.CryptoUtil
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.wallet.*
import com.tangem.wallet.EthTransaction.BruteRecoveryID2
import com.tangem.wallet.token.TokenEngine
import io.reactivex.observers.DisposableCompletableObserver
import org.apache.commons.lang3.SerializationUtils
import org.bitcoinj.core.ECKey
import org.kethereum.extensions.toBytesPadded
import org.kethereum.extensions.toFixedLengthByteArray
import java.math.BigInteger
import java.util.*

class TokenEmvEngine : TokenEngine {
    constructor() : super()
    constructor(context: TangemContext) : super(context)

    private val TAG = TokenEmvEngine::class.java.simpleName

    private fun hasLinkedContract(): Boolean {
        return ctx.card.issuerData != null && ctx.card.issuerData.size == 20
    }

    override fun getBlockchain(): Blockchain {
        return Blockchain.TokenEmv
    }

    override fun getBalance(): Amount? {
        return if (!hasBalanceInfo()) {
            null
        } else {
            convertToAmount(coinData.balanceInInternalUnits)
        }
    }

    override fun getBalanceHTML(): String? {
        return if (hasLinkedContract()) {
            if (balance != null) {
                balance!!.toDescriptionString(tokenDecimals)
            } else {
                ""
            }
        } else {
            "NO LINKED CONTRACT"
        }
    }

    override fun getBalanceCurrency(): String? {
        return ctx.card.getTokenSymbol()
    }

    override fun getAmountInputFilters(): Array<InputFilter>? {
        return arrayOf(DecimalDigitsInputFilter(tokenDecimals))
    }

    override fun getFeeCurrency(): String? {
        return balanceCurrency
    }

    override fun isBalanceNotZero(): Boolean {
        if (coinData == null) return false
        return if (coinData.balanceInInternalUnits == null) {
            false
        } else {
            coinData.balanceInInternalUnits.notZero()
        }
    }

    override fun getBalanceEquivalent(): String? {
        return ""
    }

    override fun evaluateFeeEquivalent(fee: String?): String? {
        return ""
    }

    override fun defineWallet() {
        try {
            if (hasLinkedContract()) {
                ctx.coinData.wallet = String.format("0x%s", BTCUtils.toHex(ctx.card.issuerData))
            } else {
                ctx.coinData.wallet = calculateAddress(ctx.card.walletPublicKey)
            }
        } catch (e: Exception) {
            ctx.coinData.wallet = "ERROR"
            throw TangemException("Can't define wallet address")
        }
    }

    override fun hasBalanceInfo(): Boolean {
        return coinData.balanceInInternalUnits != null
    }

    override fun isExtractPossible(): Boolean {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.loaded_wallet_error_obtaining_blockchain_data)
        } else if (!isBalanceNotZero) {
            ctx.setMessage(R.string.general_wallet_empty)
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.loaded_wallet_message_wait)
        } else {
            return true
        }
        return false
    }

    override fun checkNewTransactionAmountAndFee(amount: Amount, fee: Amount?, isFeeIncluded: Boolean): Boolean {
        try {
            if (isFeeIncluded && (amount > balance || amount < fee)) return false
            if (!isFeeIncluded && amount.add(fee) > balance) return false
        } catch (e: NumberFormatException) {
            e.printStackTrace()
        }
        return true
    }

    override fun constructTransaction(amountValue: Amount, feeValue: Amount?, IncFee: Boolean, targetAddress: String?): SignTask.TransactionToSign? {
        val functionBytes = "transfer".toByteArray()

        val contractHex = coinData.wallet.substring(2)
        val contractBytes = Util.hexToBytes(contractHex)
        val recipientHex = targetAddress!!.substring(2)
        val recipientBytes = Util.hexToBytes(recipientHex)

        val amountBytes = convertToInternalAmount(amountValue).toBigInteger().toBytesPadded(32)
        val feeLimitBytes = convertToInternalAmount(feeValue).toBigInteger().toBytesPadded(32)

        val sequenceBytes = coinData.sequence.toBigInteger().toBytesPadded(4)

        val hashToSign  = Keccak256().digest(contractBytes + functionBytes
                + amountBytes + recipientBytes + feeLimitBytes + sequenceBytes)

        return object : SignTask.TransactionToSign {
            override fun isSigningMethodSupported(signingMethod: TangemCard.SigningMethod): Boolean {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash
            }

            override fun getHashesToSign(): Array<ByteArray> {
                return arrayOf(hashToSign)
            }

            @Throws(java.lang.Exception::class)
            override fun getRawDataToSign(): ByteArray {
                throw java.lang.Exception("Signing of raw transaction not supported for " + this.javaClass.simpleName)
            }

            @Throws(java.lang.Exception::class)
            override fun getHashAlgToSign(): String {
                throw java.lang.Exception("Signing of raw transaction not supported for " + this.javaClass.simpleName)
            }

            @Throws(java.lang.Exception::class)
            override fun getIssuerTransactionSignature(dataToSignByIssuer: ByteArray): ByteArray {
                throw java.lang.Exception("Transaction validation by issuer not supported in this version")
            }

            @Throws(java.lang.Exception::class)
            override fun onSignCompleted(signFromCard: ByteArray): ByteArray {
                val r = BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32))
                var s: BigInteger? = BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64))
                s = CryptoUtil.toCanonicalised(s)

                val publicKey = ctx.getCard().getWalletPublicKey()

                val verified = ECKey.verify(hashToSign, ECKey.ECDSASignature(r, s), publicKey)
                if (!verified) {
                    Log.e(this.javaClass.simpleName + "-CHECK", "sign Failed.")
                }

                val v = BruteRecoveryID2(ECDSASignatureETH(r, s), hashToSign, publicKey)
                if (v != 27 && v != 28) {
                    Log.e(TAG, "invalid v")
                    throw java.lang.Exception("Error in " + this.javaClass.simpleName + " - invalid v")
                }
                Log.e(TAG, this.javaClass.simpleName + " V: " + v.toString())

                var rBytes = r.toByteArray()
                if (rBytes.size == 33) {
                    rBytes = rBytes.copyOfRange(1,33)
                }
                val sBytes = s.toByteArray()

                val tokenEmvTransferBody = TokenEmvTransferBody(
                        contract = contractHex,
                        amount = Util.byteArrayToHexString(amountBytes),
                        recipient = recipientHex,
                        feeLimit = Util.byteArrayToHexString(feeLimitBytes),
                        sequence = coinData.sequence,
                        r = Util.byteArrayToHexString(rBytes),
                        s = Util.byteArrayToHexString(sBytes),
                        v = v
                )

                val jsonBody = Gson().toJson(tokenEmvTransferBody)
                val serializedBody = SerializationUtils.serialize(jsonBody)

                notifyOnNeedSendTransaction(serializedBody)
                return serializedBody
            }
        }
    }

    override fun requestSendTransaction(blockchainRequestsCallbacks: BlockchainRequestsCallbacks, txForSend: ByteArray?) {
        val jsonBody = SerializationUtils.deserialize<String>(txForSend)
        val tokenEmvTransferBody = Gson().fromJson(jsonBody, TokenEmvTransferBody::class.java)

        val transferObserver = object : DisposableCompletableObserver() {
            override fun onComplete() {
                blockchainRequestsCallbacks.onComplete(true)
            }

            override fun onError(e: Throwable) {
                blockchainRequestsCallbacks.onComplete(false)
            }
        }

        ServerApiTokenEmv().transfer(tokenEmvTransferBody, transferObserver)
    }

    // TODO: move all below to the server
    fun constructTransfer(tokenEmvTransferBody: TokenEmvTransferBody, feeValue: Amount?): SignTask.TransactionToSign {
        val contractAddress = tokenEmvTransferBody.contract

        val nonceValue = coinData.confirmedTXCount
        val weiFee: BigInteger = convertToInternalAmount(feeValue).toBigIntegerExact() //TODO: get fee as usual but set multiplier m = BigInteger.valueOf(100000), use normal?
        var gasLimitInt = 100000

        val gasPrice = weiFee.divide(BigInteger.valueOf(gasLimitInt.toLong()))
        val gasLimit = BigInteger.valueOf(gasLimitInt.toLong())
        val chainId = this.chainIdNum
        val amountZero = BigInteger.ZERO

        val transferSignature = "transfer(uint256,address,uint8,bytes32,bytes32,uint256,uint256,uint32)".toByteArray()
        val selector = Keccak256().digest(transferSignature).copyOf(4)

        val tokenAmount = Util.hexToBytes(tokenEmvTransferBody.amount).toFixedLengthByteArray(32)
        val recipient = Util.hexToBytes(tokenEmvTransferBody.recipient).toFixedLengthByteArray(32)
        val sigV = BigInteger.valueOf(tokenEmvTransferBody.v.toLong()).toBytesPadded(32)
        val sigR = Util.hexToBytes(tokenEmvTransferBody.r).toFixedLengthByteArray(32)
        val sigS = Util.hexToBytes(tokenEmvTransferBody.s).toFixedLengthByteArray(32)
        val tokenFee = BigInteger.valueOf(1).toBytesPadded(32) //TODO: calculate using equivalent data
        val tokenFeeLimit = Util.hexToBytes(tokenEmvTransferBody.feeLimit).toFixedLengthByteArray(32)
        val sequence = BigInteger.valueOf(tokenEmvTransferBody.sequence.toLong()).toBytesPadded(32)

        val data = selector + tokenAmount + recipient + sigV + sigR + sigS + tokenFee + tokenFeeLimit + sequence

        val tx = EthTransaction.create(contractAddress, amountZero, nonceValue, gasPrice, gasLimit, chainId, data)

        return object : SignTask.TransactionToSign {
            override fun isSigningMethodSupported(signingMethod: TangemCard.SigningMethod): Boolean {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash
            }

            override fun getHashesToSign(): Array<ByteArray> {
                return arrayOf(tx.rawHash)
            }

            @Throws(java.lang.Exception::class)
            override fun getRawDataToSign(): ByteArray {
                throw java.lang.Exception("Signing of raw transaction not supported for " + this.javaClass.simpleName)
            }

            @Throws(java.lang.Exception::class)
            override fun getHashAlgToSign(): String {
                throw java.lang.Exception("Signing of raw transaction not supported for " + this.javaClass.simpleName)
            }

            @Throws(java.lang.Exception::class)
            override fun getIssuerTransactionSignature(dataToSignByIssuer: ByteArray): ByteArray {
                throw java.lang.Exception("Transaction validation by issuer not supported in this version")
            }

            @Throws(java.lang.Exception::class)
            override fun onSignCompleted(signFromCard: ByteArray): ByteArray {
                val publicKey = ctx.getCard().getWalletPublicKey()
                val for_hash = tx.rawHash
                val r = BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32))
                var s: BigInteger? = BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64))
                s = CryptoUtil.toCanonicalised(s)
                val f = ECKey.verify(for_hash, ECKey.ECDSASignature(r, s), publicKey)
                if (!f) {
                    Log.e(this.javaClass.simpleName + "-CHECK", "sign Failed.")
                }
                tx.signature = ECDSASignatureETH(r, s)
                val v = BruteRecoveryID2(tx.signature, for_hash, publicKey)
                if (v != 27 && v != 28) {
                    Log.e(TAG, "invalid v")
                    throw java.lang.Exception("Error in " + this.javaClass.simpleName + " - invalid v")
                }
                tx.signature.v = v.toByte()
                Log.e(TAG, this.javaClass.simpleName + " V: " + v.toString())
                val txForSend = tx.encoded
                notifyOnNeedSendTransaction(txForSend)
                return txForSend
            }
        }
    }
}