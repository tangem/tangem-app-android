package com.tangem.wallet.tokenEmv

import android.net.Uri
import android.text.InputFilter
import android.util.Log
import com.google.gson.Gson
import com.tangem.data.Blockchain
import com.tangem.data.network.ServerApiInfura
import com.tangem.data.network.ServerApiTokenEmv
import com.tangem.data.network.model.*
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.reader.CardProtocol.TangemException
import com.tangem.tangem_card.reader.TLV
import com.tangem.tangem_card.reader.TLVList
import com.tangem.tangem_card.tasks.SignTask
import com.tangem.tangem_card.util.Util
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.wallet.EthTransaction
import com.tangem.wallet.Keccak256
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import com.tangem.wallet.token.TokenEngine
import io.reactivex.observers.DisposableSingleObserver
import org.apache.commons.lang3.SerializationUtils
import org.kethereum.extensions.toBytesPadded
import java.math.BigInteger

class TokenEmvEngine : TokenEngine {
    constructor() : super()
    constructor(context: TangemContext) : super(context)

    private val TAG = TokenEmvEngine::class.java.simpleName

    private fun hasLinkedContract(): Boolean {
        return ctx.card.issuerData != null && ctx.card.issuerData.size == 44
    }

    override fun getBlockchain(): Blockchain {
        return Blockchain.TokenEmv
    }

    override fun getChainIdNum(): Int {
        return EthTransaction.ChainEnum.Ropsten.value
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
                ctx.coinData.wallet = TLVList.fromBytes(ctx.card.issuerData).getTLV(TLV.Tag.TAG_Token_Contract_Address).asString
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

    override fun getWalletExplorerUri(): Uri {
        return Uri.parse("https://ropsten.etherscan.io/token/" + getContractAddress(ctx.card) + "?a=" + ctx.coinData.wallet)
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

        val sequence = ctx.card.SignedHashes
        val sequenceBytes = sequence.toBigInteger().toBytesPadded(4)

        val hashToSign = Keccak256().digest(contractBytes + functionBytes + amountBytes + recipientBytes + feeLimitBytes + sequenceBytes)

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
//                val r = BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32))
//                var s: BigInteger? = BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64))
//                s = CryptoUtil.toCanonicalised(s)
//
//                val publicKey = ctx.getCard().getWalletPublicKey()
//
//                val verified = ECKey.verify(hashToSign, ECKey.ECDSASignature(r, s), publicKey)
//                if (!verified) {
//                    Log.e(this.javaClass.simpleName + "-CHECK", "sign Failed.")
//                }
//
//                val v = BruteRecoveryID2(ECDSASignatureETH(r, s), hashToSign, publicKey)
//                if (v != 27 && v != 28) {
//                    Log.e(TAG, "invalid v")
//                    throw java.lang.Exception("Error in " + this.javaClass.simpleName + " - invalid v")
//                }
//                Log.e(TAG, this.javaClass.simpleName + " V: " + v.toString())
//
//                var rBytes = r.toByteArray()
//                if (rBytes.size == 33) {
//                    rBytes = rBytes.copyOfRange(1,33)
//                }
//                val sBytes = s.toByteArray()
//
//                val tokenEmvTransferBody = TokenEmvTransferBody(
//                        CID = Util.bytesToHex(ctx.card.cid),
//                        publicKey = Util.bytesToHex(ctx.card.walletPublicKey),
//                        contract = contractHex,
//                        amount = Util.byteArrayToHexString(amountBytes),
//                        recipient = recipientHex,
//                        feeLimit = Util.byteArrayToHexString(feeLimitBytes),
//                        sequence = sequence,
//                        r = Util.byteArrayToHexString(rBytes),
//                        s = Util.byteArrayToHexString(sBytes),
//                        v = v
//                )

                val tokenEmvTransferBody = TokenEmvTransferBody(
                        CID = Util.bytesToHex(ctx.card.cid),
                        publicKey = Util.bytesToHex(ctx.card.walletPublicKey),
                        amount = amountValue.toValueString(),
                        currency = amountValue.currency,
                        recipient = targetAddress,
                        feeLimit = feeValue!!.toValueString(),
                        sequence = sequence,
                        signature = Util.bytesToHex(signFromCard)
                )

                val jsonBody = Gson().toJson(tokenEmvTransferBody)
                val serializedBody = SerializationUtils.serialize(jsonBody)

                notifyOnNeedSendTransaction(serializedBody)
                return serializedBody
            }
        }
    }

    override fun requestBalanceAndUnspentTransactions(blockchainRequestsCallbacks: BlockchainRequestsCallbacks) {
        val serverApiInfura = ServerApiInfura(ctx.blockchain)

        val responseListener: ServerApiInfura.ResponseListener = object : ServerApiInfura.ResponseListener {
            override fun onSuccess(method: String, infuraResponse: InfuraResponse) {
                try {
                    var balanceCap = infuraResponse.result
                    balanceCap = balanceCap!!.substring(2)
                    val l = BigInteger(balanceCap, 16)
                    coinData.balanceInInternalUnits = InternalAmount(l, ctx.card.tokenSymbol)
                    coinData.isBalanceReceived = true
                    //                              Log.i("$TAG eth_call", balanceCap)
                } catch (e: java.lang.Exception) {
                    onFail(method, e.message ?: "invalid response")
                }
                blockchainRequestsCallbacks.onComplete(true)
            }

            override fun onFail(method: String, message: String) {
                Log.e(TAG, "onFail: $method $message")
                ctx.error = message
                blockchainRequestsCallbacks.onComplete(false)
            }
        }

        serverApiInfura.setResponseListener(responseListener)

        if (validateAddress(getContractAddress(ctx.card))) {
            serverApiInfura.requestData(ServerApiInfura.INFURA_ETH_CALL, 67, coinData.wallet, getContractAddress(ctx.card), "")
        } else {
            ctx.error = "Smart contract address not defined"
            blockchainRequestsCallbacks.onComplete(false)
        }
    }

    override fun requestFee(blockchainRequestsCallbacks: BlockchainRequestsCallbacks, targetAddress: String?, amount: Amount) {

        val tokenEmvTransferBody = TokenEmvGetTransferFeeBody(
                CID = Util.bytesToHex(ctx.card.cid),
                publicKey = Util.bytesToHex(ctx.card.walletPublicKey)
        )

        val observer = object : DisposableSingleObserver<TokenEmvGetTransferFeeAnswer>() {
            override fun onError(e: Throwable) {
                ctx.error = "Can't get transfer fee, ${e.message}"
                blockchainRequestsCallbacks.onComplete(false)
            }

            override fun onSuccess(t: TokenEmvGetTransferFeeAnswer) {
                if (t.success != null && t.success) {
                    ctx.error = null
                    coinData.minFee = Amount(t.fee, t.currency)
                    coinData.normalFee = Amount(t.fee, t.currency)
                    coinData.maxFee = Amount(t.fee, t.currency)
                    blockchainRequestsCallbacks.onComplete(true)
                } else {
                    if (t.error != null) {
                        ctx.error = t.error
                    } else {
                        ctx.error = "Can't get transfer fee, code ${t.errorCode}"
                    }
                    blockchainRequestsCallbacks.onComplete(false)
                }
            }

        }

        ServerApiTokenEmv().getTransferFee(tokenEmvTransferBody, observer)
        blockchainRequestsCallbacks.onComplete(true)
    }

    override fun requestSendTransaction(blockchainRequestsCallbacks: BlockchainRequestsCallbacks, txForSend: ByteArray?) {
        val jsonBody = SerializationUtils.deserialize<String>(txForSend)

        Log.e(TAG, jsonBody)

        val tokenEmvTransferBody = Gson().fromJson(jsonBody, TokenEmvTransferBody::class.java)

        val observer = object : DisposableSingleObserver<TokenEmvTransferAnswer>() {
            override fun onError(e: Throwable) {
                ctx.error = "Can't send transfer, ${e.message}"
                blockchainRequestsCallbacks.onComplete(false)
            }

            override fun onSuccess(t: TokenEmvTransferAnswer) {
                if (t.success != null && t.success) {
                    ctx.error = null
                    blockchainRequestsCallbacks.onComplete(true)
                } else {
                    if (t.error != null) {
                        ctx.error = t.error
                    } else {
                        ctx.error = "Can't send transfer, code ${t.errorCode}"
                    }
                    blockchainRequestsCallbacks.onComplete(false)
                }

            }
        }

        ServerApiTokenEmv().transfer(tokenEmvTransferBody, observer)
    }

    override fun allowSelectFeeLevel(): Boolean {
        return false
    }

    override fun pendingTransactionTimeoutInSeconds(): Int {
        return 60
    }

}