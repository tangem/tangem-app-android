package com.tangem.wallet.flowDemo

import android.net.Uri
import android.text.InputFilter
import com.google.protobuf.ByteString
import com.tangem.tangem_card.reader.CardProtocol.TangemException
import com.tangem.tangem_card.tasks.SignTask
import com.tangem.tangem_card.util.Util
import com.tangem.wallet.*
import com.tangem.wallet.binance.client.domain.Account
import com.tangem.wallet.flowDemo.proto.Observation
import com.tangem.wallet.flowDemo.proto.ObserveServiceGrpc
import io.grpc.ManagedChannelBuilder
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class FlowDemoEngine : CoinEngine {
    constructor()

    constructor(context: TangemContext) : super(context) {
        if (context.coinData == null) {
            coinData = FlowDemoData()
            context.coinData = coinData
        } else if (context.coinData is FlowDemoData) {
            coinData = context.coinData as FlowDemoData
        } else {
            throw Exception("Invalid type of Blockchain data for XlmEngine")
        }
    }

    private val TAG = FlowDemoEngine::class.java.simpleName

    var coinData: FlowDemoData? = null

    override fun checkNewTransactionAmount(amount: Amount?): Boolean {
        return true
    }

    override fun requestBalanceAndUnspentTransactions(blockchainRequestsCallbacks: BlockchainRequestsCallbacks?) {
        val channel = ManagedChannelBuilder.forAddress("ec2-3-134-137-165.us-east-2.compute.amazonaws.com", 3569).usePlaintext().build();
        val flowApi = ObserveServiceGrpc.newBlockingStub(channel)

        val script = ByteString.copyFrom(
                """import TangemTest from 0x02

            pub fun main(): String? {
                let contract = getAccount(0x02)
	            let collectionRef = contract.published[&TangemTest.TagCollection] ?? panic("missing reference!")
                let desc = collectionRef.tags["%s"]?.desc
                return desc
            }""".format(coinData!!.wallet).toByteArray()
        )
        val scriptRequest = Observation.ExecuteScriptRequest.newBuilder().setScript(script).build()

        val scriptResponseObserver = object : DisposableSingleObserver<Observation.ExecuteScriptResponse>() {
            override fun onSuccess(response: Observation.ExecuteScriptResponse) {
                coinData!!.isBalanceReceived = true
                val byteResult = response.value.toByteArray()
                if (byteResult.size > 8) {
                    coinData!!.description = String(byteResult.copyOfRange(8, byteResult.size)).toUpperCase()
                }
                blockchainRequestsCallbacks!!.onComplete(true)
            }

            override fun onError(e: Throwable) {
                ctx.error = e.message
                blockchainRequestsCallbacks!!.onComplete(false)
            }
        }

        try {
            Single.just(Observation.ExecuteScriptResponse.getDefaultInstance())
                    .map { flowApi.executeScript(scriptRequest) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .timeout(10, TimeUnit.SECONDS)
                    .subscribe(scriptResponseObserver)
        } catch (e: Exception) {
            ctx.error = e.message
            blockchainRequestsCallbacks!!.onComplete(false)
        }
    }

    override fun getBalance(): Amount {
        return Amount()
    }

    override fun isNeedCheckNode(): Boolean {
        return false
    }

    override fun getAmountInputFilters(): Array<InputFilter> {
        TODO("Not yet implemented")
    }

    override fun getWalletExplorerUri(): Uri {
        return Uri.parse("https://www.onflow.org/")
    }

    override fun getBalanceHTML(): String {
        val balanceString = if(hasBalanceInfo()) {
            if(coinData!!.description != null) {
                coinData!!.description!! + " TANGEM CARD"
            } else {
                "UNKNOWN CARD"
            }
        } else {
            ""
        }

        if (coinData!!.description == "COMMON") {
            balanceString == "$balanceString<br><small><small>your usual tangem card</small></small>"
        } else if (coinData!!.description == "RARE") {
            balanceString == "$balanceString<br><small><small>wow, this one is RARE</small></small>"
        }

        return balanceString
    }

    override fun awaitingConfirmation(): Boolean {
        return false
    }

    override fun getFeeCurrency(): String {
        return  ""
    }

    override fun convertToByteArray(internalAmount: InternalAmount?): ByteArray {
        TODO("Not yet implemented")
    }

    override fun requestSendTransaction(blockchainRequestsCallbacks: BlockchainRequestsCallbacks?, txForSend: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun hasBalanceInfo(): Boolean {
        return coinData!!.isBalanceReceived
    }

    override fun getBalanceEquivalent(): String {
        return ""
    }

    //    public String getOfflineBalanceHTML() {
    //        InternalAmount offlineInternalAmount = convertToInternalAmount(ctx.getCard().getOfflineBalance());
    //        Amount offlineAmount = convertToAmount(offlineInternalAmount);
    //        return offlineAmount.toDescriptionString(getDecimals());
    //    }
    @Throws(TangemException::class)
    override fun defineWallet() {
        try {
            val wallet = calculateAddress(ctx.card.walletPublicKeyRar)
            ctx.coinData.wallet = wallet
        } catch (e: java.lang.Exception) {
            ctx.coinData.wallet = "ERROR"
            throw TangemException("Can't define wallet address")
        }
    }

    override fun calculateAddress(pkUncompressed: ByteArray?): String {
        return Util.byteArrayToHexString(pkUncompressed)
    }

    override fun getShareWalletUri(): Uri {
        return Uri.parse(ctx.coinData.wallet)
    }

    override fun getUnspentInputsDescription(): String {
        return ""
    }

    override fun checkNewTransactionAmountAndFee(amount: Amount?, fee: Amount?, isFeeIncluded: Boolean?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isBalanceNotZero(): Boolean {
        return true
    }

    override fun getBalanceCurrency(): String {
        return ""
    }

    override fun evaluateFeeEquivalent(fee: String?): String {
        return ""
    }

    override fun convertToAmount(internalAmount: InternalAmount?): Amount {
        TODO("Not yet implemented")
    }

    override fun convertToAmount(strAmount: String?, currency: String?): Amount {
        TODO("Not yet implemented")
    }

    override fun isExtractPossible(): Boolean {
        return false
    }

    override fun convertToInternalAmount(amount: Amount?): InternalAmount {
        TODO("Not yet implemented")
    }

    override fun convertToInternalAmount(bytes: ByteArray?): InternalAmount {
        TODO("Not yet implemented")
    }

    override fun requestFee(blockchainRequestsCallbacks: BlockchainRequestsCallbacks?, targetAddress: String?, amount: Amount?) {
        TODO("Not yet implemented")
    }

    override fun validateAddress(address: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun constructTransaction(amountValue: Amount?, feeValue: Amount?, IncFee: Boolean, targetAddress: String?): SignTask.TransactionToSign {
        TODO("Not yet implemented")
    }

    override fun validateBalance(balanceValidator: BalanceValidator?): Boolean {

        if (hasBalanceInfo()) {
            if (coinData!!.description != null) {
                balanceValidator!!.setScore(100)
                balanceValidator.firstLine = R.string.balance_validator_first_line_verified_in_blockchain
                balanceValidator.setSecondLine(R.string.empty_string)
            } else {
                balanceValidator!!.setScore(0)
                balanceValidator.firstLine = R.string.balance_validator_first_line_authenticity
                balanceValidator.setSecondLine(R.string.empty_string)
            }
        } else {
            balanceValidator!!.setScore(0)
            balanceValidator.firstLine = R.string.balance_validator_first_line_no_connection
            balanceValidator.setSecondLine(R.string.balance_validator_second_line_authenticity_not_verified)
            return false
        }

        return true
    }

    override fun createCoinData(): CoinData {
        return FlowDemoData()
    }
}