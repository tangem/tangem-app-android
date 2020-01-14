package com.tangem.wallet.xlmtag

import android.net.Uri
import android.os.StrictMode
import android.text.InputFilter
import android.util.Log
import com.tangem.App
import com.tangem.data.Blockchain
import com.tangem.data.network.ServerApiStellar
import com.tangem.data.network.StellarRequest
import com.tangem.data.network.StellarRequest.Ledgers
import com.tangem.data.network.StellarRequest.SubmitTransaction
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.tasks.SignTask
import com.tangem.tangem_card.util.Util
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.wallet.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.stellar.sdk.*
import org.stellar.sdk.requests.RequestBuilder
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import java.io.IOException
import java.math.BigDecimal


class XlmTagEngine : CoinEngine {
    var coinData: XlmTagData? = null
    val operations = mutableListOf<OperationResponse>()

    constructor(context: TangemContext) : super(context) {
        if (context.coinData == null) {
            coinData = XlmTagData()
            context.coinData = coinData
        } else if (context.coinData is XlmTagData) {
            coinData = context.coinData as XlmTagData
        } else {
            throw Exception("Invalid type of Blockchain data for XlmEngine")
        }
    }

    constructor() : super() {}

    @Throws(Exception::class)
    private fun checkBlockchainDataExists() {
        if (coinData == null) throw Exception("No blockchain data")
    }

    override fun awaitingConfirmation(): Boolean {
        return App.pendingTransactionsStorage.hasTransactions(ctx.card)
    }

    override fun getBalanceHTML(): String {
        return if (hasBalanceInfo() && isBalanceNotZero && coinData!!.fundsFromTrustedSource) {
           ctx.getString(R.string.tag_genuine)
        } else if (coinData!!.fundsSentToTrustedSource) {
            ctx.getString(R.string.tag_claimed)
        } else {
            ctx.getString(R.string.tag_not_genuine)
        }
    }

    override fun getBalanceCurrency(): String {
        return "XLM"
    }

    override fun isBalanceNotZero(): Boolean {
        if (coinData == null) return false
        return if (coinData!!.getBalance() == null) false else coinData!!.getBalance()!!.notZero()
    }

    override fun hasBalanceInfo(): Boolean {
        return if (coinData == null) false else coinData!!.getBalance() != null || coinData!!.isError404
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

    override fun getFeeCurrency(): String {
        return "XLM"
    }

    override fun validateAddress(address: String): Boolean {
        try {
            val kp = KeyPair.fromAccountId(address)
        } catch (e: Exception) {
            return false
        }
        return true
    }

    override fun isNeedCheckNode(): Boolean {
        return false
    }

    override fun getWalletExplorerUri(): Uri {
        return Uri.parse("https://stellar.expert/explorer/public/account/" + ctx.coinData.wallet)
    }

    override fun getShareWalletUri(): Uri {
        return if (ctx.card.denomination != null) {
            Uri.parse(ctx.coinData.wallet + "?amount=" + convertToAmount(convertToInternalAmount(ctx.card.denomination)!!).toValueString())
        } else {
            Uri.parse(ctx.coinData.wallet)
        }
    }

    override fun getAmountInputFilters(): Array<InputFilter> {
        return arrayOf(DecimalDigitsInputFilter(decimals))
    }

    override fun checkNewTransactionAmount(amount: Amount): Boolean {
        return true
    }

    override fun checkNewTransactionAmountAndFee(amountValue: Amount, feeValue: Amount, isIncludeFee: Boolean): Boolean {
        return true
    }

    override fun validateBalance(balanceValidator: BalanceValidator): Boolean {
        return try {
            if (ctx.card.offlineBalance == null && !ctx.coinData.isBalanceReceived || !ctx.coinData.isBalanceReceived && ctx.card.remainingSignatures != ctx.card.maxSignatures) {
                if (coinData!!.isError404) {
                    balanceValidator.setScore(0)
                    balanceValidator.firstLine = R.string.balance_validator_first_line_no_account
                    balanceValidator.setSecondLine(R.string.balance_validator_second_line_create_account_instruction)
                } else {
                    balanceValidator.setScore(0)
                    balanceValidator.firstLine = R.string.balance_validator_first_line_unknown_balance
                    balanceValidator.setSecondLine(R.string.balance_validator_second_line_unverified_balance)
                    return false
                }
            }

            if (coinData!!.isBalanceReceived && coinData!!.isBalanceEqual) {
                balanceValidator.setScore(100)
                balanceValidator.firstLine = R.string.balance_validator_first_line_verified_balance
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_confirmed_in_blockchain)
                if (coinData!!.getBalance()!!.isZero) {
                    balanceValidator.firstLine = R.string.balance_validator_first_line_empty_wallet
                    balanceValidator.setSecondLine(R.string.empty_string)
                }
            }

            if (ctx.card.offlineBalance != null && !coinData!!.isBalanceReceived && ctx.card.remainingSignatures == ctx.card.maxSignatures && coinData!!.getBalance()!!.notZero()) {
                balanceValidator.setScore(80)
                balanceValidator.firstLine = R.string.balance_validator_first_line_verified_offline
                balanceValidator.setSecondLine(R.string.balance_validator_second_line_internet_to_get_balance)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun getBalance(): Amount? {
        return if (!hasBalanceInfo()) null else coinData!!.getBalance()!!
    }

    override fun evaluateFeeEquivalent(fee: String): String {
        return if (!coinData!!.amountEquivalentDescriptionAvailable) "" else try {
            val feeAmount = Amount(fee, feeCurrency)
            feeAmount.toEquivalentString(coinData!!.rate.toDouble())
        } catch (e: Exception) {
            ""
        }
    }

    override fun getBalanceEquivalent(): String {
        if (coinData == null || !coinData!!.amountEquivalentDescriptionAvailable) return ""
        val balance = balance ?: return ""
        return balance.toEquivalentString(coinData!!.rate.toDouble())
    }

    override fun calculateAddress(pkUncompressed: ByteArray): String {
        val kp = KeyPair.fromPublicKey(pkUncompressed)
        return kp.accountId
    }

    override fun convertToAmount(internalAmount: InternalAmount): Amount {
        val d = internalAmount.divide(multiplier)
        return Amount(d, balanceCurrency)
    }

    override fun convertToAmount(strAmount: String, currency: String): Amount {
        return Amount(strAmount, currency)
    }

    override fun convertToInternalAmount(amount: Amount): InternalAmount {
        val d = amount.multiply(multiplier)
        return InternalAmount(d, "stroops")
    }

    override fun convertToInternalAmount(bytes: ByteArray): InternalAmount? {
        if (bytes == null) return null
        val reversed = ByteArray(bytes.size)
        for (i in bytes.indices) reversed[i] = bytes[bytes.size - i - 1]
        return InternalAmount(Util.byteArrayToLong(reversed), "stroops")
    }

    override fun convertToByteArray(internalAmount: InternalAmount): ByteArray {
        val bytes = Util.longToByteArray(internalAmount.longValueExact())
        val reversed = ByteArray(bytes.size)
        for (i in bytes.indices) reversed[i] = bytes[bytes.size - i - 1]
        return reversed
    }

    override fun createCoinData(): CoinData {
        return XlmTagData()
    }

    override fun getUnspentInputsDescription(): String {
        return ""
    }

    @Throws(Exception::class)
    override fun constructTransaction(amountValue: Amount, feeValue: Amount, IncFee: Boolean, targetAddress: String): SignTask.TransactionToSign {
        var amountValue = amountValue
        checkBlockchainDataExists()
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        if (IncFee) {
            amountValue = Amount(amountValue.subtract(feeValue), amountValue.currency)
        }
        val operation: Operation
        operation = if (coinData!!.isTargetAccountCreated) PaymentOperation.Builder(KeyPair.fromAccountId(targetAddress), AssetTypeNative(), amountValue.toValueString()).build() else CreateAccountOperation.Builder(KeyPair.fromAccountId(targetAddress), amountValue.toValueString()).build()
        val transaction = TransactionEx.buildEx(60, coinData!!.accountResponse, operation)
        if (transaction.fee != convertToInternalAmount(feeValue).intValueExact()) {
            throw Exception("Invalid fee!")
        }
        return object : SignTask.TransactionToSign {
            override fun isSigningMethodSupported(signingMethod: TangemCard.SigningMethod): Boolean {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash || signingMethod == TangemCard.SigningMethod.Sign_Raw
            }

            @Throws(Exception::class)
            override fun getHashesToSign(): Array<ByteArray> {
                val dataForSign = arrayOf(transaction.hash())
                return dataForSign
            }

            @Throws(Exception::class)
            override fun getRawDataToSign(): ByteArray {
                return transaction.signatureBase()
            }

            override fun getHashAlgToSign(): String {
                return "sha-256"
            }

            @Throws(Exception::class)
            override fun getIssuerTransactionSignature(dataToSignByIssuer: ByteArray): ByteArray {
                throw Exception("Issuer validation not supported!")
            }

            @Throws(Exception::class)
            override fun onSignCompleted(signFromCard: ByteArray): ByteArray { // Sign the transaction to prove you are actually the person sending it.
                transaction.setSign(signFromCard)
                val txForSend = transaction.toEnvelopeXdrBase64().toByteArray()
                notifyOnNeedSendTransaction(txForSend)
                return txForSend
            }
        }
    }

    private fun checkTargetAccountCreated(blockchainRequestsCallbacks: BlockchainRequestsCallbacks, targetAddress: String, amount: Amount) {
        val serverApi = ServerApiStellar(ctx.blockchain)
        val listener: ServerApiStellar.Listener = object : ServerApiStellar.Listener {
            override fun onSuccess(request: StellarRequest.Base) {
                coinData!!.isTargetAccountCreated = true
                blockchainRequestsCallbacks.onComplete(true)
            }

            override fun onFail(request: StellarRequest.Base) {
                Log.i(TAG, "onFail: " + request.javaClass.simpleName + " " + request.error)
                if (request.errorResponse != null && request.errorResponse.code == 404) {
                    coinData!!.isTargetAccountCreated = false
                    if (amount.compareTo(coinData!!.reserve) >= 0) {
                        blockchainRequestsCallbacks.onComplete(true)
                    } else {
                        ctx.setError(R.string.confirm_transaction_error_not_enough_xlm_for_create)
                        blockchainRequestsCallbacks.onComplete(false)
                    }
                } else { // suppose account is created if anything goes wrong
                    coinData!!.isTargetAccountCreated = true
                    blockchainRequestsCallbacks.onComplete(true)
                }
            }
        }
        serverApi.setListener(listener)
        serverApi.requestData(ctx, StellarRequest.Balance(targetAddress))
    }

    private fun requestPayments(blockchainRequestsCallbacks: BlockchainRequestsCallbacks) {
        val server = Server("https://horizon.stellar.org/")
        val accountKeyPair = KeyPair.fromAccountId(coinData!!.wallet)

        try {
            var operationsPage = server.payments().forAccount(accountKeyPair).order(RequestBuilder.Order.DESC).execute()
            Log.e("Stellar", operationsPage.records.toString())

            operations.addAll(operationsPage.records)

            while (operations.size < OPERATIONS_LIMIT) {
                operationsPage = operationsPage.getNextPage(server.httpClient)
                operations.addAll(operationsPage.records)
                Log.e("Stellar", operationsPage.records.toString())
                Log.e("Stellar", "Operations downloaded: " + operationsPage.records.count())
                Log.e("Stellar", "Operations overall: " + operations.count())
            }

            parsePayments(blockchainRequestsCallbacks)
        } catch (e: Exception) {
            if (e.message != null) {
                ctx.error = e.message
                blockchainRequestsCallbacks.onComplete(false)
            } else {
                ctx.error = e.javaClass.name
                blockchainRequestsCallbacks.onComplete(false)
            }
        }
    }

    private fun parsePayments(blockchainRequestsCallbacks: BlockchainRequestsCallbacks) {
        coinData!!.fundsFromTrustedSource = operations.find { it is PaymentOperationResponse && it.from.accountId == TRUSTED_SOURCE } != null
        coinData!!.fundsSentToTrustedSource =
                (operations.find { it is PaymentOperationResponse && it.to.accountId == TRUSTED_DESTINATION } != null) && coinData!!.fundsFromTrustedSource
        Log.e("Stellar",
                "fundsFromTrustedSource $coinData!!.fundsFromTrustedSource, " +
                        "fundsSentToTrustedSource $coinData!!.fundsSentToTrustedSource")
        blockchainRequestsCallbacks.onComplete(!ctx.hasError())
    }

    override fun requestBalanceAndUnspentTransactions(blockchainRequestsCallbacks: BlockchainRequestsCallbacks) {
        CoroutineScope(Dispatchers.IO).launch { requestPayments(blockchainRequestsCallbacks) }
        val serverApi = ServerApiStellar(Blockchain.Stellar)
        val listener: ServerApiStellar.Listener = object : ServerApiStellar.Listener {
            override fun onSuccess(request: StellarRequest.Base) {
                Log.i(TAG, "onSuccess: " + request.javaClass.simpleName)
                if (request is StellarRequest.Balance) {
                    coinData!!.accountResponse = request.accountResponse
                    coinData!!.validationNodeDescription = serverApi.currentURL

                    blockchainRequestsCallbacks.onProgress()
                } else if (request is Ledgers) {
                    coinData!!.setLedgerResponse(request.ledgerResponse)
                    if (serverApi.isRequestsSequenceCompleted) {
                    } else {
                        blockchainRequestsCallbacks.onProgress()
                    }
                } else {
                    ctx.error = "Invalid request logic"
                    blockchainRequestsCallbacks.onComplete(false)
                }
            }

            override fun onFail(request: StellarRequest.Base) {
                Log.i(TAG, "onFail: " + request.javaClass.simpleName + " " + request.error)
                if (request.errorResponse != null && request.errorResponse.code == 404) {
                    coinData!!.isError404 = true
                } else {
                    ctx.error = request.error
                }
                if (serverApi.isRequestsSequenceCompleted) {
                    if (ctx.hasError()) {
                        blockchainRequestsCallbacks.onComplete(false)
                    } else {
                        blockchainRequestsCallbacks.onComplete(true)
                    }
                } else {
                    blockchainRequestsCallbacks.onProgress()
                }
            }
        }
        serverApi.setListener(listener)
        serverApi.requestData(ctx, StellarRequest.Balance(coinData!!.wallet))
        serverApi.requestData(ctx, Ledgers())
    }

    @Throws(Exception::class)
    override fun requestFee(blockchainRequestsCallbacks: BlockchainRequestsCallbacks, targetAddress: String, amount: Amount) {
        coinData!!.maxFee = coinData!!.baseFee
        coinData!!.normalFee = coinData!!.maxFee
        coinData!!.minFee = coinData!!.normalFee
        checkTargetAccountCreated(blockchainRequestsCallbacks, targetAddress, amount)
    }

    @Throws(IOException::class)
    override fun requestSendTransaction(blockchainRequestsCallbacks: BlockchainRequestsCallbacks, txForSend: ByteArray) {
        val serverApi = ServerApiStellar(ctx.blockchain)
        val listener: ServerApiStellar.Listener = object : ServerApiStellar.Listener {
            override fun onSuccess(request: StellarRequest.Base) {
                try {
                    if (!SubmitTransaction::class.java.isInstance(request)) throw Exception("Invalid request logic")
                    val submitTransactionRequest = request as SubmitTransaction
                    if (submitTransactionRequest.response.isSuccess) {
                        ctx.error = null
                        blockchainRequestsCallbacks.onComplete(true)
                    } else {
                        if (submitTransactionRequest.response.extras != null && submitTransactionRequest.response.extras.resultCodes != null) {
                            var trResult = submitTransactionRequest.response.extras.resultCodes.transactionResultCode
                            if (submitTransactionRequest.response.extras.resultCodes.operationsResultCodes != null && submitTransactionRequest.response.extras.resultCodes.operationsResultCodes.size > 0) {
                                trResult += "/" + submitTransactionRequest.response.extras.resultCodes.operationsResultCodes[0]
                            }
                            ctx.error = trResult
                        } else {
                            ctx.error = "transaction failed"
                        }
                        blockchainRequestsCallbacks.onComplete(false)
                    }
                } catch (e: Exception) {
                    if (e.message != null) {
                        ctx.error = e.message
                        blockchainRequestsCallbacks.onComplete(false)
                    } else {
                        ctx.error = e.javaClass.name
                        blockchainRequestsCallbacks.onComplete(false)
                    }
                }
            }

            override fun onFail(request: StellarRequest.Base) {
                ctx.error = request.error
                blockchainRequestsCallbacks.onComplete(false)
            }
        }
        serverApi.setListener(listener)
        val transaction = TransactionEx.fromEnvelopeXdr(String(txForSend))
        coinData!!.incSequenceNumber()
        serverApi.requestData(ctx, SubmitTransaction(transaction))
    }

    override fun needMultipleLinesForBalance(): Boolean {
        return true
    }

    override fun allowSelectFeeLevel(): Boolean {
        return false
    }

    override fun pendingTransactionTimeoutInSeconds(): Int {
        return 10
    }

    companion object {
        private val TAG = XlmTagEngine::class.java.simpleName
        private val decimals: Int
            private get() = 7

        private val multiplier = BigDecimal("10000000")

        const val OPERATIONS_LIMIT = 50
        private val TRUSTED_DESTINATION = "GAYPZMHFZERB42ONEJ4CY6ADDVTINEXMY6OZ5G6CLR4HHVKOSNJSZGMM"
        private val TRUSTED_SOURCE = "GAZY7H4BWWEVB6QGB4RV3LW7DH5NO5CD5O6JCEQXA7N2UCGZSAPJFYW2"
    }

}