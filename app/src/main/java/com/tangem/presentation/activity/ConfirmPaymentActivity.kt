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
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.domain.wallet.CoinEngine
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemContext
import com.tangem.presentation.event.TransactionFinishWithError
import com.tangem.tangemcard.android.reader.NfcManager
import com.tangem.tangemcard.data.TangemCard
import com.tangem.tangemcard.data.loadFromBundle
import com.tangem.util.LOG
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_confirm_payment.*
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.util.*

class ConfirmPaymentActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private var nfcManager: NfcManager? = null

    private lateinit var ctx: TangemContext
    private lateinit var amount: CoinEngine.Amount

    private var isIncludeFee: Boolean = true
    private var requestPIN2Count = 0
    private var nodeCheck = true
    private var dtVerified: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_payment)

        nfcManager = NfcManager(this, this)

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        val engine = CoinEngineFactory.create(ctx)

        val html = Html.fromHtml(engine!!.balanceHTML)
        tvBalance.text = html

        isIncludeFee = intent.getBooleanExtra(Constant.EXTRA_FEE_INCLUDED, true)

        if (isIncludeFee)
            tvIncFee.setText(R.string.including_fee)
        else
            tvIncFee.setText(R.string.not_including_fee)

        amount = CoinEngine.Amount(intent.getStringExtra(Constant.EXTRA_AMOUNT), intent.getStringExtra(Constant.EXTRA_AMOUNT_CURRENCY))

        if (ctx.blockchain == Blockchain.Token && amount.currency != "ETH")
            tvIncFee.visibility = View.INVISIBLE
        else
            tvIncFee.visibility = View.VISIBLE

        etAmount.setText(amount.toValueString())
        tvCurrency.text = engine.balanceCurrency
        tvCurrency2.text = engine.feeCurrency
        tvCardID.text = ctx.card!!.cidDescription
        etWallet.setText(intent.getStringExtra(Constant.EXTRA_TARGET_ADDRESS))
        etFee.setText("")

        btnSend.visibility = View.INVISIBLE

        rgFee.isEnabled = !(ctx.blockchain == Blockchain.Ethereum || ctx.blockchain == Blockchain.EthereumTestNet || ctx.blockchain == Blockchain.Token || ctx.blockchain == Blockchain.BitcoinCash || ctx.blockchain == Blockchain.Litecoin)

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
            intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
            ctx.saveToIntent(intent)
            intent.putExtra(Constant.EXTRA_FEE_INCLUDED, isIncludeFee)
            startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_)
        }

        val coinEngine = CoinEngineFactory.create(ctx)

        progressBar.visibility = View.VISIBLE

        coinEngine!!.requestFee(
                object : CoinEngine.BlockchainRequestsCallbacks {
                    override fun onComplete(success: Boolean) {
                        if (success) {
                            onProgress()
                            progressBar.visibility = View.INVISIBLE
                            dtVerified = Date()
                        } else {
                            finishWithError(Activity.RESULT_CANCELED, ctx.error)
                        }
                    }

                    override fun onProgress() {
                        doSetFee(rgFee.checkedRadioButtonId)
                    }

                    override fun allowAdvance(): Boolean {
                        return UtilHelper.isOnline(this@ConfirmPaymentActivity)
                    }
                },
                etWallet.text.toString(),
                amount)
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
        if (requestCode == Constant.REQUEST_CODE_SIGN_PAYMENT) {
            if (data != null && data.extras != null) {
                if (data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                    val updatedCard = TangemCard(data.getStringExtra("UID"))
                    updatedCard.loadFromBundle(data.getBundleExtra("Card"))
                    ctx.card = updatedCard
                }
            }
            if (resultCode == Constant.RESULT_INVALID_PIN_ && requestPIN2Count < 2) {
                requestPIN2Count++

                val intent = Intent(baseContext, PinRequestActivity::class.java)
                intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
                ctx.saveToIntent(intent)
                intent.putExtra(Constant.EXTRA_FEE_INCLUDED, isIncludeFee)
                startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_)

                return
            }
            setResult(resultCode, data)
            finish()
        } else if (requestCode == Constant.REQUEST_CODE_REQUEST_PIN2_) {
            if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(baseContext, SignPaymentActivity::class.java)
                ctx.saveToIntent(intent)
                intent.putExtra(Constant.EXTRA_TARGET_ADDRESS, etWallet!!.text.toString())
                intent.putExtra(Constant.EXTRA_AMOUNT, etAmount.text.toString())
                intent.putExtra(Constant.EXTRA_AMOUNT_CURRENCY, tvCurrency.text.toString())
                intent.putExtra(Constant.EXTRA_FEE, etFee.text.toString())
                intent.putExtra(Constant.EXTRA_FEE_CURRENCY, tvCurrency2.text.toString())
                intent.putExtra(Constant.EXTRA_FEE_INCLUDED, isIncludeFee)
                startActivityForResult(intent, Constant.REQUEST_CODE_SIGN_PAYMENT)
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

    private fun doSetFee(checkedRadioButtonId: Int) {
        var txtFee = ""
        when (checkedRadioButtonId) {
            R.id.rbMinimalFee ->
                if (ctx.coinData.minFee != null) {
                    txtFee = ctx.coinData.minFee!!.toValueString()
                    btnSend.visibility = View.VISIBLE
                } else
                    btnSend.visibility = View.INVISIBLE

            R.id.rbNormalFee ->
                if (ctx.coinData.normalFee != null) {
                    txtFee = ctx.coinData.normalFee!!.toValueString()
                    btnSend.visibility = View.VISIBLE
                } else
                    btnSend.visibility = View.INVISIBLE

            R.id.rbMaximumFee ->
                if (ctx.coinData.maxFee != null) {
                    txtFee = ctx.coinData.maxFee!!.toValueString()
                    btnSend.visibility = View.VISIBLE
                } else
                    btnSend.visibility = View.INVISIBLE
        }
        etFee.setText(txtFee.replace(',', '.'))
    }

    private fun finishWithError(errorCode: Int, message: String) {
        val transactionFinishWithError = TransactionFinishWithError()
        transactionFinishWithError.message = message
        EventBus.getDefault().post(transactionFinishWithError)

        val intent = Intent()
        intent.putExtra("message", message)
        setResult(errorCode, intent)
        finish()
    }

}