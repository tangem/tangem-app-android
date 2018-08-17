package com.tangem.presentation.activity

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.*
import com.tangem.data.network.request.ElectrumRequest
import com.tangem.data.network.request.FeeRequest
import com.tangem.data.network.request.InfuraRequest
import com.tangem.data.network.task.confirm_payment.ConnectFeeTask
import com.tangem.data.network.task.confirm_payment.ConnectTask
import com.tangem.data.network.task.confirm_payment.ETHRequestTask
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.*
import com.tangem.util.BTCUtils
import com.tangem.util.DerEncodingUtil
import com.tangem.util.FormatUtil
import com.tangem.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_confirm_payment.*
import java.io.IOException
import java.math.BigInteger
import java.util.*

class ConfirmPaymentActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    companion object {
        private const val REQUEST_CODE_SIGN_PAYMENT = 1
        private const val REQUEST_CODE_REQUEST_PIN2 = 2
    }

    private var nfcManager: NfcManager? = null
    var card: TangemCard? = null

    var feeRequestSuccess = false
    var balanceRequestSuccess = false

    private var tvFeeEquivalent: TextView? = null
    var etAmount: EditText? = null
    var etFee: EditText? = null
    var rgFee: RadioGroup? = null
    var progressBar: ProgressBar? = null
    var btnSend: Button? = null

    var minFee: String? = null
    var maxFee: String? = null
    var normalFee: String? = null
    var minFeeInInternalUnits: Long? = 0L
    private var requestPIN2Count = 0
    var nodeCheck = false
    var dtVerified: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_payment)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        card = TangemCard(intent.getStringExtra("UID"))
        card!!.loadFromBundle(intent.extras!!.getBundle("Card"))

        progressBar = findViewById(R.id.progressBar)
        btnSend = findViewById(R.id.btnSend)
        etAmount = findViewById(R.id.etAmount)
        etFee = findViewById(R.id.etFee)
        tvFeeEquivalent = findViewById(R.id.tvFeeEquivalent)
        rgFee = findViewById(R.id.rgFee)

        val engine = CoinEngineFactory.create(card!!.blockchain)
        if (card!!.blockchain == Blockchain.Token) {
            val html = Html.fromHtml(engine!!.getBalanceWithAlter(card))
            tvBalance.text = html
        } else
            tvBalance.text = engine!!.getBalanceWithAlter(card)

        etAmount!!.setText(intent.getStringExtra(SignPaymentActivity.EXTRA_AMOUNT))
        tvCurrency.text = engine.getBalanceCurrency(card)
        tvCurrency2.text = engine.getFeeCurrency()

        tvCardID.text = card!!.cidDescription

        etWallet!!.setText(intent.getStringExtra("Wallet"))

        etFee!!.setText("?")

        btnSend!!.visibility = View.INVISIBLE
        feeRequestSuccess = false
        balanceRequestSuccess = false

        // set listeners
        rgFee!!.setOnCheckedChangeListener { _, checkedId -> doSetFee(checkedId) }
        etFee!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    val engine = CoinEngineFactory.create(card!!.blockchain)
                    val eqFee = engine!!.evaluateFeeEquivalent(card, etFee!!.text.toString())
                    tvFeeEquivalent!!.text = eqFee

                    if (!card!!.amountEquivalentDescriptionAvailable) {
                        tvFeeEquivalent!!.error = "Service unavailable"
                    } else {
                        tvFeeEquivalent!!.error = null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    tvFeeEquivalent!!.text = ""
                }
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
        btnSend!!.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, -1)

            if (dtVerified == null || dtVerified!!.before(calendar.time)) {
                finishActivityWithError(Activity.RESULT_CANCELED, getString(R.string.the_obtained_data_is_outdated_try_again))
                return@setOnClickListener
            }

            val engineCoin = CoinEngineFactory.create(card!!.blockchain)

            if (engineCoin!!.isNeedCheckNode() && !nodeCheck) {
                Toast.makeText(baseContext, getString(R.string.cannot_reach_current_active_blockchain_node_try_again), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val txFee = etFee!!.text.toString()
            val txAmount = etAmount!!.text.toString()

            if (!engineCoin.hasBalanceInfo(card)) {
                finishActivityWithError(Activity.RESULT_CANCELED, getString(R.string.cannot_check_balance_no_connection_with_blockchain_nodes))
                return@setOnClickListener

            } else if (!engineCoin.isBalanceNotZero(card)) {
                finishActivityWithError(Activity.RESULT_CANCELED, getString(R.string.the_wallet_is_empty))
                return@setOnClickListener

            } else if (!engineCoin.checkUnspentTransaction(card)) {
                finishActivityWithError(Activity.RESULT_CANCELED, getString(R.string.please_wait_for_confirmation_of_incoming_transaction))
                return@setOnClickListener
            }

            if (!engineCoin.checkAmountValue(card, txAmount, txFee, minFeeInInternalUnits)) {
                finishActivityWithError(Activity.RESULT_CANCELED, getString(R.string.fee_exceeds_payment_amount_enter_correct_value_and_repeat_sending))
                return@setOnClickListener
            }

            requestPIN2Count = 0
            val intent = Intent(baseContext, PinRequestActivity::class.java)
            intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
            intent.putExtra("UID", card!!.uid)
            intent.putExtra("Card", card!!.asBundle)
            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2)
        }

        if (card!!.blockchain == Blockchain.Ethereum || card!!.blockchain == Blockchain.EthereumTestNet || card!!.blockchain == Blockchain.Token) {
            val task = ETHRequestTask(this@ConfirmPaymentActivity, card!!.blockchain)
            val req = InfuraRequest.GetGasPrise(card!!.wallet)
            req.id = 67
            req.setBlockchain(card!!.blockchain)
            rgFee!!.isEnabled = false
            task.execute(req)

        } else {
            rgFee!!.isEnabled = true
            val data = SharedData(SharedData.COUNT_REQUEST)
            val engineCoin = CoinEngineFactory.create(card!!.blockchain)

            for (i in 0 until data.allRequest) {
                val nodeAddress = engineCoin!!.getNextNode(card)
                val nodePort = engineCoin.getNextNodePort(card)
                val connectTaskEx = ConnectTask(this@ConfirmPaymentActivity, nodeAddress, nodePort, data)
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.checkBalance(card!!.wallet))
            }

            var calcSize = 256
            try {
                calcSize = buildSize(etWallet!!.text.toString(), "0.00", etAmount!!.text.toString())
            } catch (ex: Exception) {
                Log.e("Build Fee error", ex.message)
            }

            card!!.resetFailedBalanceRequestCounter()
            val sharedFee = SharedData(SharedData.COUNT_REQUEST)

            progressBar!!.visibility = View.VISIBLE
            for (i in 0 until SharedData.COUNT_REQUEST) {
                val feeTask = ConnectFeeTask(this@ConfirmPaymentActivity, sharedFee)
                feeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        FeeRequest.GetFee(card!!.wallet, calcSize.toLong(), FeeRequest.NORMAL),
                        FeeRequest.GetFee(card!!.wallet, calcSize.toLong(), FeeRequest.MINIMAL),
                        FeeRequest.GetFee(card!!.wallet, calcSize.toLong(), FeeRequest.PRIORITY))
            }
        }
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
                    card = updatedCard
                }
            }
            if (resultCode == SignPaymentActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                requestPIN2Count++
                val intent = Intent(baseContext, PinRequestActivity::class.java)
                intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                intent.putExtra("UID", card!!.uid)
                intent.putExtra("Card", card!!.asBundle)
                startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2)
                return
            }
            setResult(resultCode, data)
            finish()
        } else if (requestCode == REQUEST_CODE_REQUEST_PIN2) {
            if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(baseContext, SignPaymentActivity::class.java)
                intent.putExtra("UID", card!!.uid)
                intent.putExtra("Card", card!!.asBundle)
                intent.putExtra("Wallet", etWallet!!.text.toString())
                intent.putExtra(SignPaymentActivity.EXTRA_AMOUNT, etAmount!!.text.toString())
                intent.putExtra("Fee", etFee!!.text.toString())
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

    fun finishActivityWithError(errorCode: Int, message: String) {
        val intent = Intent()
        intent.putExtra("message", message)
        setResult(errorCode, intent)
        finish()
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
//            Log.w(getClass().getName(), "Ignore discovered tag!");
            nfcManager!!.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    internal fun buildSize(outputAddress: String, outFee: String, outAmount: String): Int {
        val myAddress = card!!.wallet
        val pbKey = card!!.walletPublicKey
        val pbComprKey = card!!.walletPublicKeyRar

        // build script for our address
        val rawTxList = card!!.unspentTransactions
        val outputScriptWeAreAbleToSpend = Transaction.Script.buildOutput(myAddress).bytes

        // collect unspent
        val unspentOutputs = BTCUtils.getOutputs(rawTxList, outputScriptWeAreAbleToSpend)

        var fullAmount: Long = 0
        for (i in unspentOutputs.indices) {
            fullAmount += unspentOutputs[i].value
        }

        // Get first unspent
        val outPut = unspentOutputs[0]
        val outPutIndex = outPut.outputIndex

        // get prev TX id;
        val prevTXID = rawTxList[0].txID//"f67b838d6e2c0c587f476f583843e93ff20368eaf96a798bdc25e01f53f8f5d2";

        val fees = FormatUtil.ConvertStringToLong(outFee)
        var amount = FormatUtil.ConvertStringToLong(outAmount)
        amount = amount - fees

        val change = fullAmount - fees - amount

        if (amount + fees > fullAmount) {
            throw Exception(String.format("Balance (%d) < amount (%d) + (%d)", fullAmount, change, amount))
        }

        val hashesForSign = arrayOfNulls<ByteArray>(unspentOutputs.size)

        for (i in unspentOutputs.indices) {
            val newTX = BTCUtils.buildTXForSign(myAddress, outputAddress, myAddress, unspentOutputs, i, amount, change)

            val hashData = Util.calculateSHA256(newTX)
            val doubleHashData = Util.calculateSHA256(hashData)

            //            Log.e("TX_BODY_1", BTCUtils.toHex(newTX));
            //            Log.e("TX_HASH_1", BTCUtils.toHex(hashData));
            //            Log.e("TX_HASH_2", BTCUtils.toHex(doubleHashData));

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

    fun doSetFee(checkedRadioButtonId: Int) {
        when (checkedRadioButtonId) {
            R.id.rbMinimalFee ->
                if (minFee != null)
                    etFee!!.setText(minFee)
                else
                    etFee!!.setText("?")
            R.id.rbNormalFee ->
                if (normalFee != null)
                    etFee!!.setText(normalFee)
                else
                    etFee!!.setText("?")
            R.id.rbMaximumFee ->
                if (maxFee != null)
                    etFee!!.setText(maxFee)
                else
                    etFee!!.setText("?")
        }
    }

}