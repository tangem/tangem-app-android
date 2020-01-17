package com.tangem.wallet.tezos

import android.net.Uri
import android.os.StrictMode
import android.text.InputFilter
import android.util.Log
import com.tangem.App
import com.tangem.data.network.ServerApiTezos
import com.tangem.data.network.model.TezosAccountResponse
import com.tangem.data.network.model.TezosForgeBody
import com.tangem.data.network.model.TezosOperationContent
import com.tangem.data.network.model.TezosPreapplyBody
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.tasks.SignTask
import com.tangem.tangem_card.util.Util
import com.tangem.util.CryptoUtil
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.wallet.*
import io.reactivex.observers.DisposableSingleObserver
import org.spongycastle.jcajce.provider.digest.Blake2b
import java.math.BigDecimal

class TezosEngine : CoinEngine {
    constructor()

    constructor(context: TangemContext) : super(context) {
        if (context.coinData == null) {
            coinData = TezosData()
            context.coinData = coinData
        } else if (context.coinData is TezosData) {
            coinData = context.coinData as TezosData
        } else {
            throw Exception("Invalid type of Blockchain data for XlmEngine")
        }
    }

    private val TAG = TezosEngine::class.java.simpleName

    var coinData: TezosData? = null

    private fun getDecimals() = 6

    @Throws(Exception::class)
    private fun checkBlockchainDataExists() {
        if (coinData == null) throw Exception("No blockchain data")
    }

    override fun awaitingConfirmation(): Boolean {
        return App.pendingTransactionsStorage.hasTransactions(ctx.card)
    }

    override fun getBalanceHTML(): String? {
        val balance = balance
        return if (balance != null) {
            balance.toDescriptionString(getDecimals())
        } else {
            ""
        }
    }

    override fun getBalanceCurrency(): String? {
        return "XTZ"
    }

    override fun isBalanceNotZero(): Boolean {
        if (coinData == null) return false
        return if (balance == null) false else balance!!.notZero()
    }

    override fun hasBalanceInfo(): Boolean {
        return if (coinData == null) false else coinData!!.balance != null
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

    override fun getFeeCurrency(): String? {
        return "XTZ"
    }

    override fun validateAddress(address: String?): Boolean {
        val prefixedHashWithChecksum = Base58.decodeBase58(address)

        if (prefixedHashWithChecksum == null || prefixedHashWithChecksum.size != 27) return false

        val prefixedHash = prefixedHashWithChecksum.copyOf(23)
        val checksum = prefixedHashWithChecksum.copyOfRange(23, 27)

        val calcChecksum = CryptoUtil.doubleSha256(prefixedHash).copyOfRange(0, 4)

        return calcChecksum.contentEquals(checksum)
    }

    override fun isNeedCheckNode(): Boolean {
        return false
    }

    override fun getWalletExplorerUri(): Uri? {
        return Uri.parse("https://tezblock.io/account/" + ctx.coinData.wallet)
    }

    override fun getShareWalletUri(): Uri? {
        return Uri.parse(ctx.coinData.wallet)
    }

    override fun getAmountInputFilters(): Array<InputFilter>? {
        return arrayOf(DecimalDigitsInputFilter(getDecimals()))
    }

    override fun checkNewTransactionAmount(amount: Amount): Boolean {
        if (coinData == null) return false
        return amount <= balance
    }

    override fun checkNewTransactionAmountAndFee(amountValue: Amount?,
                                                 feeValue: Amount?,
                                                 isIncludeFee: Boolean
    ): Boolean {
        try {
            checkBlockchainDataExists()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return false
        }
        if (feeValue == null || amountValue == null) return false
        if (feeValue.isZero || amountValue.isZero) return false
        if (isIncludeFee && (amountValue > balance || amountValue < feeValue)) return false
        if (!isIncludeFee && amountValue.add(feeValue) > balance) return false

        return true
    }

    override fun validateBalance(balanceValidator: BalanceValidator): Boolean {
        try {
            if (ctx.card.offlineBalance == null &&
                    !ctx.coinData.isBalanceReceived ||
                    !ctx.coinData.isBalanceReceived &&
                    ctx.card.remainingSignatures != ctx.card.maxSignatures
            ) {
                balanceValidator.setScore(0)
                balanceValidator.firstLine = R.string.balance_validator_first_line_unknown_balance
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance)
                return false
            }
            if (coinData!!.isBalanceReceived) {
                balanceValidator.setScore(100)
                balanceValidator.firstLine = R.string.balance_validator_first_line_verified_balance
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain)
                if (balance!!.isZero) {
                    balanceValidator.firstLine = R.string.balance_validator_first_line_empty_wallet
                    balanceValidator.setSecondLine(R.string.empty_string)
                }
            }
            if (ctx.card.offlineBalance != null &&
                    !coinData!!.isBalanceReceived &&
                    ctx.card.remainingSignatures ==
                    ctx.card.maxSignatures &&
                    balance!!.notZero()
            ) {
                balanceValidator.setScore(80)
                balanceValidator.firstLine = R.string.balance_validator_first_line_verified_offline
                balanceValidator.setSecondLine(
                        R.string.balance_validator_second_line_internet_to_get_balance
                )
            }
            return true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            return false
        }
    }

    override fun getBalance(): Amount? {
        var balanceAmount: Amount? = null

        if (hasBalanceInfo()) {
            val xtzBalance = BigDecimal
                    .valueOf(coinData!!.balance!!).movePointLeft(getDecimals())
            balanceAmount = Amount(xtzBalance, balanceCurrency)
        }

        return balanceAmount
    }

    override fun evaluateFeeEquivalent(fee: String?): String? {
        return if (!coinData!!.amountEquivalentDescriptionAvailable) "" else try {
            val feeAmount = Amount(fee, feeCurrency)
            feeAmount.toEquivalentString(coinData!!.rate.toDouble())
        } catch (e: java.lang.Exception) {
            ""
        }
    }

    override fun getBalanceEquivalent(): String? {
        if (coinData == null || !coinData!!.amountEquivalentDescriptionAvailable) return ""
        val balance = balance ?: return ""
        return balance.toEquivalentString(coinData!!.rate.toDouble())
    }

    override fun calculateAddress(pkUncompressed: ByteArray): String? {
        val publicKeyHash = Blake2b.Blake2b160().digest(pkUncompressed)

        val tz1Prefix = Util.hexToBytes("06A19F")
        val prefixedHash = tz1Prefix + publicKeyHash

        val checksum = CryptoUtil.doubleSha256(prefixedHash).copyOfRange(0, 4)
        val prefixedHashWithChecksum = prefixedHash + checksum

        return Base58.encodeBase58(prefixedHashWithChecksum)
    }

    fun calculateTezosPublicKey(pkUncompressed: ByteArray): String {
        val edpkPrefix = Util.hexToBytes("0D0F25D9")
        val prefixedPubKey = edpkPrefix + pkUncompressed

        val checksum = CryptoUtil.doubleSha256(prefixedPubKey).copyOfRange(0, 4)
        val prefixedHashWithChecksum = prefixedPubKey + checksum

        return Base58.encodeBase58(prefixedHashWithChecksum)
    }

    override fun convertToAmount(internalAmount: InternalAmount): Amount {
        return Amount(internalAmount.movePointLeft(getDecimals()), balanceCurrency)
    }

    override fun convertToAmount(strAmount: String, currency: String): Amount {
        return Amount(strAmount, currency)
    }

    override fun convertToInternalAmount(amount: Amount): InternalAmount {
        return InternalAmount(amount.movePointRight(getDecimals()), "mutez")
    }

    override fun convertToInternalAmount(bytes: ByteArray?): InternalAmount? {
        if (bytes == null) return null
        val reversed = ByteArray(bytes.size)
        for (i in bytes.indices) reversed[i] = bytes[bytes.size - i - 1]
        return InternalAmount(Util.byteArrayToLong(reversed), "mutez")
    }

    override fun convertToByteArray(internalAmount: InternalAmount): ByteArray? {
        val bytes = Util.longToByteArray(internalAmount.longValueExact())
        val reversed = ByteArray(bytes.size)
        for (i in bytes.indices) reversed[i] = bytes[bytes.size - i - 1]
        return reversed
    }

    override fun createCoinData(): CoinData {
        return TezosData()
    }

    override fun getUnspentInputsDescription() = ""

    override fun constructTransaction(
            amountValue: Amount,
            feeValue: Amount,
            IncFee: Boolean,
            targetAddress: String
    ): SignTask.TransactionToSign {

        checkBlockchainDataExists()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val finalAmount = if (IncFee) {
            amountValue.minus(feeValue)
        } else {
            amountValue
        }

        val serverApiTezos = ServerApiTezos()
        val headerResponse = serverApiTezos.header

        val contents = arrayListOf<TezosOperationContent>()

        var counter = coinData!!.counter!!

        if (!coinData!!.publicKeyReavealed!!) {
            counter++
            val revealOp = TezosOperationContent(
                    kind = "reveal",
                    source = coinData!!.wallet,
                    fee = "1300",
                    counter = counter.toString(),
                    gas_limit = "10000",
                    storage_limit = "0",
                    public_key = coinData!!.tezosPublicKey!!
            )

            contents.add(revealOp)
        }

        counter++
        val transactionOp = TezosOperationContent(
                kind = "transaction",
                source = coinData!!.wallet,
                fee = "1350",
                counter = counter.toString(),
                gas_limit = "10600",
                storage_limit = "277",
                destination = targetAddress,
                amount = finalAmount.movePointRight(getDecimals()).toBigInteger().toString()
        )

        contents.add(transactionOp)

        val tezosForgeBody = TezosForgeBody(headerResponse.hash!!, contents)
        val forgeResponse = serverApiTezos.forgeOperations(tezosForgeBody)
        val watermark = "03"
        val forgedBytes = Util.hexToBytes(watermark + forgeResponse)

        return object : SignTask.TransactionToSign {
            override fun isSigningMethodSupported(signingMethod: TangemCard.SigningMethod): Boolean {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash
            }

            @Throws(java.lang.Exception::class)
            override fun getHashesToSign(): Array<ByteArray?> {
                val dataForSign = arrayOfNulls<ByteArray>(1)
                dataForSign[0] = Blake2b.Blake2b256().digest(forgedBytes)
                return dataForSign
            }

            @Throws(java.lang.Exception::class)
            override fun getRawDataToSign(): ByteArray? {
                throw java.lang.Exception("Signing of raw transaction not supported for " + this.javaClass.simpleName)
            }

            @Throws(java.lang.Exception::class)
            override fun getHashAlgToSign(): String? {
                throw java.lang.Exception("Signing of raw transaction not supported for " + this.javaClass.simpleName)
            }

            @Throws(java.lang.Exception::class)
            override fun getIssuerTransactionSignature(dataToSignByIssuer: ByteArray?): ByteArray? {
                throw java.lang.Exception("Transaction validation by issuer not supported in this version")
            }

            @Throws(java.lang.Exception::class)
            override fun onSignCompleted(signFromCard: ByteArray): ByteArray? {
                val edsigPrefix = Util.hexToBytes("09F5CD8612")
                val prefixedSignature = edsigPrefix + signFromCard
                val checksum = CryptoUtil.doubleSha256(prefixedSignature).copyOfRange(0, 4)
                val prefixedSignatureWithChecksum = prefixedSignature + checksum

                val preapplyBody = TezosPreapplyBody(
                        protocol = headerResponse.protocol!!,
                        branch = headerResponse.hash!!,
                        contents = contents,
                        signature = Base58.encodeBase58(prefixedSignatureWithChecksum)
                )

                try {
                    serverApiTezos.peapplyOperations(preapplyBody)
                } catch (e: java.lang.Exception) {
                    ctx.error = e.message
                    return null
                }

                val txForSend = Util.hexToBytes(forgeResponse) + signFromCard
                notifyOnNeedSendTransaction(txForSend)
                return txForSend
            }
        }
    }

    override fun requestBalanceAndUnspentTransactions(
            blockchainRequestsCallbacks: BlockchainRequestsCallbacks
    ) {
        coinData!!.tezosPublicKey = calculateTezosPublicKey(ctx.card.walletPublicKey)

        val serverApiTezos = ServerApiTezos()

        val accountObserver = object : DisposableSingleObserver<TezosAccountResponse>() {
            override fun onSuccess(response: TezosAccountResponse) {
                coinData!!.balance = response.balance
                coinData!!.isBalanceReceived = true
                coinData!!.counter = response.counter

                if (serverApiTezos.isRequestsSequenceCompleted) {
                    blockchainRequestsCallbacks.onComplete(ctx.hasError())
                } else {
                    blockchainRequestsCallbacks.onProgress()
                }
            }

            override fun onError(e: Throwable) {
                Log.e(TAG, "requestBalanceAndUnspentTransactions error " + e.message)
                ctx.error = e.message
                e.printStackTrace()

                if (serverApiTezos.isRequestsSequenceCompleted) {
                    blockchainRequestsCallbacks.onComplete(false)
                } else {
                    blockchainRequestsCallbacks.onProgress()
                }
            }
        }

        val managerKeyObserver = object : DisposableSingleObserver<String>() {
            override fun onSuccess(response: String) {
                coinData!!.publicKeyReavealed = true

                if (serverApiTezos.isRequestsSequenceCompleted) {
                    blockchainRequestsCallbacks.onComplete(ctx.hasError())
                } else {
                    blockchainRequestsCallbacks.onProgress()
                }
            }

            override fun onError(e: Throwable) {
                Log.e(TAG, "requestFee error " + e.message)
                e.printStackTrace()

                coinData!!.publicKeyReavealed = false // error expected when key is not revealed

                if (serverApiTezos.isRequestsSequenceCompleted) {
                    blockchainRequestsCallbacks.onComplete(ctx.hasError())
                } else {
                    blockchainRequestsCallbacks.onProgress()
                }
            }
        }

        serverApiTezos.getAddress(coinData!!.wallet, accountObserver)
        serverApiTezos.getMangerKey(coinData!!.wallet, managerKeyObserver)

    }

    override fun requestFee(
            blockchainRequestsCallbacks: BlockchainRequestsCallbacks,
            targetAddress: String?,
            amount: Amount?
    ) {
        var fee: BigDecimal = BigDecimal.valueOf(0.00135)

        if (!coinData!!.publicKeyReavealed!!) {
            fee += BigDecimal.valueOf(0.0013)
        }

        val serverApiTezos = ServerApiTezos()

        val accountObserver = object : DisposableSingleObserver<TezosAccountResponse>() {
            override fun onSuccess(response: TezosAccountResponse) {
                if (response.balance == 0L) {
                    fee += BigDecimal.valueOf(0.257)
                }
                val feeAmount = Amount(fee, feeCurrency)
                coinData!!.minFee = feeAmount
                coinData!!.normalFee = feeAmount
                coinData!!.maxFee = feeAmount

                blockchainRequestsCallbacks.onComplete(true)
            }

            override fun onError(e: Throwable) {
                Log.e(TAG, "requestFee error " + e.message)
                ctx.error = e.message
                e.printStackTrace()

                blockchainRequestsCallbacks.onComplete(false)
            }
        }

        serverApiTezos.getAddress(targetAddress, accountObserver)
    }

    override fun requestSendTransaction(
            blockchainRequestsCallbacks: BlockchainRequestsCallbacks,
            txForSend: ByteArray?
    ) {

        if (txForSend == null) {
            blockchainRequestsCallbacks.onComplete(false)
        } else {
            val injectObserver = object : DisposableSingleObserver<Any>() {
                override fun onSuccess(response: Any) {
                    blockchainRequestsCallbacks.onComplete(true)
                }

                override fun onError(e: Throwable) {
                    blockchainRequestsCallbacks.onComplete(false)
                }
            }

            ServerApiTezos().injectOperations(Util.bytesToHex(txForSend), injectObserver)
        }
    }

    override fun allowSelectFeeLevel(): Boolean {
        return false
    }

    override fun pendingTransactionTimeoutInSeconds(): Int {
        return 60
    }
}