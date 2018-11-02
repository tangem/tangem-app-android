package com.tangem.presentation.activity

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.tangem.data.network.ElectrumRequest
import com.tangem.data.network.ServerApiHelper
import com.tangem.data.network.ServerApiHelperElectrum
import com.tangem.data.network.model.InfuraResponse
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.*
import com.tangem.util.*
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_confirm_payment.*
import org.json.JSONException
import java.io.IOException
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.*

class ConfirmPaymentActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    companion object {
        private const val REQUEST_CODE_SIGN_PAYMENT = 1
        private const val REQUEST_CODE_REQUEST_PIN2 = 2
    }

    private var nfcManager: NfcManager? = null

    private var serverApiHelper: ServerApiHelper = ServerApiHelper()
    private var serverApiHelperElectrum: ServerApiHelperElectrum = ServerApiHelperElectrum()

    private lateinit var ctx: TangemContext
    private lateinit var amount: CoinEngine.Amount

    private var feeRequestSuccess = false
    private var balanceRequestSuccess = false
    private var minFee: CoinEngine.Amount? = null
    private var maxFee: CoinEngine.Amount? = null
    private var normalFee: CoinEngine.Amount? = null
    private var isIncludeFee: Boolean = true
    private var requestPIN2Count = 0
    private var nodeCheck = false
    private var dtVerified: Date? = null
    private var calcSize: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_payment)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        val engine = CoinEngineFactory.create(ctx)

        val html = Html.fromHtml(engine!!.balanceHTML)
        tvBalance.text = html

        isIncludeFee = intent.getBooleanExtra(SignPaymentActivity.EXTRA_FEE_INCLUDED, true)

        if (isIncludeFee)
            tvIncFee.setText(R.string.including_fee)
        else
            tvIncFee.setText(R.string.not_including_fee)

        amount = CoinEngine.Amount(intent.getStringExtra(SignPaymentActivity.EXTRA_AMOUNT), intent.getStringExtra(SignPaymentActivity.EXTRA_AMOUNT_CURRENCY))

        if (ctx.blockchain == Blockchain.Token && amount.currency!="ETH")
            tvIncFee.visibility = View.INVISIBLE
        else
            tvIncFee.visibility = View.VISIBLE

        etAmount.setText(amount.toValueString())
        tvCurrency.text = engine.balanceCurrency
        tvCurrency2.text = engine.feeCurrency
        tvCardID.text = ctx.card!!.cidDescription
        etWallet.setText(intent.getStringExtra(SignPaymentActivity.EXTRA_TARGET_ADDRESS))
        etFee.setText("")

        btnSend.visibility = View.INVISIBLE
        feeRequestSuccess = false
        balanceRequestSuccess = false

        if (ctx.blockchain == Blockchain.Ethereum || ctx.blockchain == Blockchain.EthereumTestNet || ctx.blockchain == Blockchain.Token) {
            rgFee.isEnabled = false

            requestInfura(ServerApiHelper.INFURA_ETH_GAS_PRICE)

        } else {
            rgFee.isEnabled = true

            requestElectrum(ctx.card, ElectrumRequest.checkBalance(ctx.card!!.wallet))

            calcSize = 256
            try {
                calcSize = buildSize(etWallet!!.text.toString(), "0.00", etAmount.text.toString())
            } catch (ex: Exception) {
                Log.e("Build Fee error", ex.message)
            }

            ctx.coinData!!.resetFailedBalanceRequestCounter()

            progressBar.visibility = View.VISIBLE

            requestEstimateFee()
        }

        // set listeners
        rgFee.setOnCheckedChangeListener { _, checkedId -> doSetFee(checkedId) }
        etFee.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    val engine = CoinEngineFactory.create(ctx)
                    val eqFee = engine!!.evaluateFeeEquivalent(etFee!!.text.toString())
                    tvFeeEquivalent.text = eqFee

                    if (!ctx.coinData!!.amountEquivalentDescriptionAvailable) {
                        tvFeeEquivalent.error = "Service unavailable"
                        tvCurrency2.visibility = View.GONE
                        tvFeeEquivalent.visibility = View.GONE
                    } else
                        tvFeeEquivalent.error = null

                } catch (e: Exception) {
                    e.printStackTrace()
                    tvFeeEquivalent.text = ""
                }
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
        btnSend.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, -1)

            if (dtVerified == null || dtVerified!!.before(calendar.time)) {
                finishWithError(Activity.RESULT_CANCELED, getString(R.string.the_obtained_data_is_outdated_try_again))
                return@setOnClickListener
            }

            val engineCoin = CoinEngineFactory.create(ctx)

            if (engineCoin!!.isNeedCheckNode && !nodeCheck) {
                Toast.makeText(baseContext, getString(R.string.cannot_reach_current_active_blockchain_node_try_again), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val txFee = engineCoin.convertToAmount(etFee.text.toString(), tvCurrency2.text.toString())
            val txAmount = engineCoin.convertToAmount(etAmount.text.toString(), tvCurrency.text.toString())

            if (!engineCoin.hasBalanceInfo()) {
                finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_check_balance_no_connection_with_blockchain_nodes))
                return@setOnClickListener

            } else if (!engineCoin.isBalanceNotZero) {
                finishWithError(Activity.RESULT_CANCELED, getString(R.string.the_wallet_is_empty))
                return@setOnClickListener

            } else if (!engineCoin.isExtractPossible) {
                finishWithError(Activity.RESULT_CANCELED, getString(R.string.please_wait_for_confirmation_of_incoming_transaction))
                return@setOnClickListener
            }

            if (!engineCoin.checkNewTransactionAmountAndFee(txAmount, txFee, isIncludeFee)) {
                finishWithError(Activity.RESULT_CANCELED, getString(R.string.not_enough_funds_on_your_card))
                return@setOnClickListener
            }

            requestPIN2Count = 0
            val intent = Intent(baseContext, PinRequestActivity::class.java)
            intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
            ctx.saveToIntent(intent)
            intent.putExtra(SignPaymentActivity.EXTRA_FEE_INCLUDED, isIncludeFee)
            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2)
        }

        // request electrum listener
        val electrumBodyListener: ServerApiHelperElectrum.ElectrumRequestDataListener = object : ServerApiHelperElectrum.ElectrumRequestDataListener {
            override fun onSuccess(electrumRequest: ElectrumRequest?) {
                if (electrumRequest!!.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                    try {
                        if (etFee.text.toString().isEmpty()) etFee.setText(getString(R.string.empty))
                        val engine = CoinEngineFactory.create(ctx)
                        val balance = engine.convertToAmount(CoinEngine.InternalAmount(electrumRequest.result.getLong("confirmed") + electrumRequest.result.getLong("unconfirmed"), "Satoshi"))
                        val amount = CoinEngine.Amount(etAmount.text.toString(), ctx.blockchain.currency)
                        if (balance < amount) {
                            etFee.error = getString(R.string.not_enough_funds)
                        } else {
                            etFee.error = null
                            balanceRequestSuccess = true
                            if (feeRequestSuccess && balanceRequestSuccess) {
                                btnSend.visibility = View.VISIBLE
                            }
                            dtVerified = Date()
                            nodeCheck = true
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        requestElectrum(ctx.card!!, ElectrumRequest.checkBalance(ctx.card!!.wallet))
                    }
                }
            }

            override fun onFail(message: String?) {
                finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_check_balance_no_connection_with_blockchain_nodes))
            }

        }
        serverApiHelperElectrum.setElectrumRequestData(electrumBodyListener)

        // request infura eth gasPrice listener
        val infuraBodyListener: ServerApiHelper.InfuraBodyListener = object : ServerApiHelper.InfuraBodyListener {
            override fun onSuccess(method: String, infuraResponse: InfuraResponse) {
                when (method) {
                    ServerApiHelper.INFURA_ETH_GAS_PRICE -> {
                        var gasPrice = infuraResponse.result
                        gasPrice = gasPrice.substring(2)
                        //TODO - remove Gwei
                        // rounding gas price to integer gwei
                        val l = BigInteger(gasPrice, 16).divide(BigInteger.valueOf(1000000000L)).multiply(BigInteger.valueOf(1000000000L))

                        //val m = if (ctx.blockchain==Blockchain.Token) BigInteger.valueOf(60000) else BigInteger.valueOf(21000)
                        val m = if (amount.currency != "ETH") BigInteger.valueOf(60000) else BigInteger.valueOf(21000)
                        val weiMinFee = CoinEngine.InternalAmount(l.multiply(m), "wei")
                        val weiNormalFee = CoinEngine.InternalAmount(weiMinFee.multiply(BigDecimal.valueOf(12)).divide(BigDecimal.valueOf(10)), "wei")
                        val weiMaxFee = CoinEngine.InternalAmount(weiMinFee.multiply(BigDecimal.valueOf(15)).divide(BigDecimal.valueOf(10)), "wei")

                        minFee = engine.convertToAmount(weiMinFee)
                        normalFee = engine.convertToAmount(weiNormalFee)
                        maxFee = engine.convertToAmount(weiMaxFee)
                        doSetFee(rgFee.checkedRadioButtonId)
                        //etFee.setText(weiNormalFee.toValueString())
                        etFee.error = null
                        btnSend.visibility = View.VISIBLE
                        feeRequestSuccess = true
                        balanceRequestSuccess = true
                        dtVerified = Date()
                    }
                }
            }

            override fun onFail(method: String, message: String) {
                when (method) {
                    ServerApiHelper.INFURA_ETH_GAS_PRICE -> {
                        finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_obtain_data_from_blockchain))
                    }
                }
            }
        }
        serverApiHelper.setInfuraResponse(infuraBodyListener)

        // request estimate fee listener
        val estimateFeeListener: ServerApiHelper.EstimateFeeListener = object : ServerApiHelper.EstimateFeeListener {
            override fun onSuccess(blockCount: Int, estimateFeeResponse: String?) {
                var fee: BigDecimal?
                fee = BigDecimal(estimateFeeResponse) // BTC per 1 kb

                if (fee == BigDecimal.ZERO) {
                    progressBar.visibility = View.INVISIBLE
                    requestEstimateFee()
                }

                if (calcSize.toLong() != 0L) {
                    fee = fee.multiply(BigDecimal(calcSize.toLong())).divide(BigDecimal(1024)) // per Kb -> per byte
                } else {
                    requestEstimateFee()
                }

                progressBar.visibility = View.INVISIBLE

                fee = fee!!.setScale(8, RoundingMode.DOWN)

                when (blockCount) {
                    ServerApiHelper.ESTIMATE_FEE_MINIMAL -> {
                        minFee = CoinEngine.Amount(fee, engine.feeCurrency)
                        if (rgFee.checkedRadioButtonId == R.id.rbMinimalFee) doSetFee(rgFee.checkedRadioButtonId)
                    }

                    ServerApiHelper.ESTIMATE_FEE_NORMAL -> {
                        normalFee = CoinEngine.Amount(fee, engine.feeCurrency)
                        if (rgFee.checkedRadioButtonId == R.id.rbNormalFee) doSetFee(rgFee.checkedRadioButtonId)
                    }

                    ServerApiHelper.ESTIMATE_FEE_PRIORITY -> {
                        maxFee = CoinEngine.Amount(fee, engine.feeCurrency)
                        if (rgFee.checkedRadioButtonId == R.id.rbMaximumFee) doSetFee(rgFee.checkedRadioButtonId)
                    }
                }

                etFee.error = null
                feeRequestSuccess = true
                if (feeRequestSuccess && balanceRequestSuccess)
                    btnSend.visibility = View.VISIBLE
                dtVerified = Date()
            }

            override fun onFail(message: String?) {
                finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_calculate_fee_wrong_data_received_from_node))
            }
        }
        serverApiHelper.setEstimateFee(estimateFeeListener)
    }

    public override fun onResume() {
        super.onResume()
        nfcManager!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        nfcManager!!.onPause()
    }

    public override fun onStop() {
        super.onStop()
        nfcManager!!.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SIGN_PAYMENT) {
            if (data != null && data.extras != null) {
                if (data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                    val updatedCard = TangemCard(data.getStringExtra("UID"))
                    updatedCard.loadFromBundle(data.getBundleExtra("Card"))
                    ctx.card = updatedCard
                }
            }
            if (resultCode == SignPaymentActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                requestPIN2Count++
                val intent = Intent(baseContext, PinRequestActivity::class.java)
                intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                ctx.saveToIntent(intent)
                intent.putExtra(SignPaymentActivity.EXTRA_FEE_INCLUDED, isIncludeFee)
                startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2)
                return
            }
            setResult(resultCode, data)
            finish()
        } else if (requestCode == REQUEST_CODE_REQUEST_PIN2) {
            if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(baseContext, SignPaymentActivity::class.java)
                ctx.saveToIntent(intent)
                intent.putExtra(SignPaymentActivity.EXTRA_TARGET_ADDRESS, etWallet!!.text.toString())
                intent.putExtra(SignPaymentActivity.EXTRA_AMOUNT, etAmount.text.toString())
                intent.putExtra(SignPaymentActivity.EXTRA_AMOUNT_CURRENCY, tvCurrency.text.toString())
                intent.putExtra(SignPaymentActivity.EXTRA_FEE, etFee.text.toString())
                intent.putExtra(SignPaymentActivity.EXTRA_FEE_CURRENCY, tvCurrency2.text.toString())
                intent.putExtra(SignPaymentActivity.EXTRA_FEE_INCLUDED, isIncludeFee)
                startActivityForResult(intent, REQUEST_CODE_SIGN_PAYMENT)
            } else
                Toast.makeText(baseContext, R.string.pin_2_is_required_to_sign_the_payment, Toast.LENGTH_LONG).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                val intent = Intent()
                setResult(Activity.RESULT_CANCELED, intent)
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            nfcManager!!.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // TODO - move to BtcEngine
    @Throws(Exception::class)
    internal fun buildSize(outputAddress: String, outFee: String, outAmount: String): Int {
        val myAddress = ctx.card!!.wallet
        val pbKey = ctx.card!!.walletPublicKey
        val pbComprKey = ctx.card!!.walletPublicKeyRar

        // build script for our address
        val rawTxList = (ctx.coinData!! as BtcData).unspentTransactions
        val outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes

        // collect unspent
        val unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend)

        var fullAmount: Long = 0
        for (i in unspentOutputs.indices) {
            fullAmount += unspentOutputs[i].value
        }

        // get first unspent
        val outPut = unspentOutputs[0]
        val outPutIndex = outPut.outputIndex

        // get prev TX id;
        val prevTXID = rawTxList[0].txID//"f67b838d6e2c0c587f476f583843e93ff20368eaf96a798bdc25e01f53f8f5d2";

        val fees = FormatUtil.ConvertStringToLong(outFee)
        var amount = FormatUtil.ConvertStringToLong(outAmount)
        amount -= fees

        val change = fullAmount - fees - amount

        if (amount + fees > fullAmount) {
            throw Exception(String.format("Balance (%d) < amount (%d) + (%d)", fullAmount, change, amount))
        }

        val hashesForSign = arrayOfNulls<ByteArray>(unspentOutputs.size)

        for (i in unspentOutputs.indices) {
            val newTX = BTCUtils.buildTXForSign(myAddress, outputAddress, myAddress, unspentOutputs, i, amount, change)
            val hashData = Util.calculateSHA256(newTX)
            val doubleHashData = Util.calculateSHA256(hashData)
//            Log.e("TX_BODY_1", BTCUtils.toHex(newTX))
//            Log.e("TX_HASH_1", BTCUtils.toHex(hashData))
//            Log.e("TX_HASH_2", BTCUtils.toHex(doubleHashData))

            unspentOutputs[i].bodyDoubleHash = doubleHashData
            unspentOutputs[i].bodyHash = hashData
            hashesForSign[i] = doubleHashData
        }

        val signFromCard = ByteArray(64 * unspentOutputs.size)

        for (i in unspentOutputs.indices) {
            val r = BigInteger(1, Arrays.copyOfRange(signFromCard, 0 + i * 64, 32 + i * 64))
            val s = BigInteger(1, Arrays.copyOfRange(signFromCard, 32 + i * 64, 64 + i * 64))
            val encodingSign = DerEncodingUtil.packSignDer(r, s, pbKey)
            unspentOutputs[i].scriptForBuild = encodingSign
        }

        val realTX = BTCUtils.buildTXForSend(outputAddress, myAddress, unspentOutputs, amount, change)

        return realTX.size
    }

    private fun requestElectrum(card: TangemCard, electrumRequest: ElectrumRequest) {
        if (UtilHelper.isOnline(this)) {
            serverApiHelperElectrum.electrumRequestData(card, electrumRequest)
        } else
            finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_obtain_data_from_blockchain))
    }

    private fun requestInfura(method: String) {
        if (UtilHelper.isOnline(this)) {
            serverApiHelper.infura(method, 67, ctx.card!!.wallet, "", "")
        } else
            finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_obtain_data_from_blockchain))
    }

    private fun requestEstimateFee() {
        serverApiHelper.estimateFee(ServerApiHelper.ESTIMATE_FEE_PRIORITY)
        serverApiHelper.estimateFee(ServerApiHelper.ESTIMATE_FEE_NORMAL)
        serverApiHelper.estimateFee(ServerApiHelper.ESTIMATE_FEE_MINIMAL)
    }

    private fun doSetFee(checkedRadioButtonId: Int) {
        var txtFee = ""
        when (checkedRadioButtonId) {
            R.id.rbMinimalFee ->
                if (minFee != null)
                    txtFee = minFee!!.toValueString()
                else
                    finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_obtain_data_from_blockchain))
            R.id.rbNormalFee ->
                if (normalFee != null)
                    txtFee = normalFee!!.toValueString()
                else
                    finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_obtain_data_from_blockchain))
            R.id.rbMaximumFee ->
                if (maxFee != null)
                    txtFee = maxFee!!.toValueString()
                else
                    finishWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_obtain_data_from_blockchain))
        }
        etFee.setText(txtFee.replace(',', '.'))
    }

    private fun finishWithError(errorCode: Int, message: String) {
        val intent = Intent()
        intent.putExtra("message", message)
        setResult(errorCode, intent)
        finish()
    }

}