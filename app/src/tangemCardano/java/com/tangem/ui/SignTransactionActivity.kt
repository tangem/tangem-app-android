package com.tangem.ui

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tangem.App
import com.tangem.Constant
import com.tangem.card_android.android.nfc.NfcDeviceAntennaLocation
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.card_android.android.reader.NfcReader
import com.tangem.card_android.data.EXTRA_TANGEM_CARD
import com.tangem.card_android.data.EXTRA_TANGEM_CARD_UID
import com.tangem.card_android.data.asBundle
import com.tangem.card_common.data.TangemCard
import com.tangem.card_common.reader.CardCrypto
import com.tangem.card_common.reader.CardProtocol
import com.tangem.card_common.tasks.CustomReadCardTask
import com.tangem.card_common.tasks.OneTouchSignTask
import com.tangem.card_common.tasks.SignTask
import com.tangem.card_common.util.Log
import com.tangem.wallet.CoinEngine
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.TangemContext
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.activity.SendTransactionActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.util.LOG
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.layout_progress_horizontal.*
import kotlinx.android.synthetic.main.layout_touch_card.*
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SignTransactionActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = SignTransactionActivity::class.java.simpleName
    }

    private lateinit var nfcManager: NfcManager
    private lateinit var ctx: TangemContext
    private lateinit var mpFinishSignSound: MediaPlayer

    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation

    private var task: CustomReadCardTask? = null

    private lateinit var amount: CoinEngine.Amount
    private lateinit var fee: CoinEngine.Amount
    private var isIncludeFee = true
    private var outAddressStr: String? = null
    private var lastReadSuccess = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_transaction)

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        mpFinishSignSound = MediaPlayer.create(this, R.raw.scan_card_sound)

        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(this, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
        nfcDeviceAntenna.init()

        amount = CoinEngine.Amount(intent.getStringExtra(Constant.EXTRA_AMOUNT), intent.getStringExtra(Constant.EXTRA_AMOUNT_CURRENCY))
        fee = CoinEngine.Amount(intent.getStringExtra(Constant.EXTRA_FEE), intent.getStringExtra(Constant.EXTRA_FEE_CURRENCY))
        isIncludeFee = intent.getBooleanExtra(Constant.EXTRA_FEE_INCLUDED, true)
        outAddressStr = intent.getStringExtra(Constant.EXTRA_TARGET_ADDRESS)

        // tvCardID.text = ctx.card!!.cidDescription
        progressBar.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar.visibility = View.INVISIBLE
    }

    public override fun onPause() {
        task?.cancel(true)
        super.onPause()
    }

    public override fun onStop() {
        task?.cancel(true)
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTION_) {
            setResult(resultCode, data)
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
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
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)

            isoDep.timeout = 65000

            val coinEngine = CoinEngineFactory.createCardano(ctx!!)!!
            coinEngine.setOnNeedSendTransaction { tx ->
                if (tx != null) {
                    val intent = Intent(this, SendTransactionActivity::class.java)
                    ctx.saveToIntent(intent)
                    intent.putExtra(Constant.EXTRA_TX, tx)
                    startActivityForResult(intent, Constant.REQUEST_CODE_SEND_TRANSACTION_)
                }
            }

            val tx: OneTouchSignTask.TransactionToSign = object : OneTouchSignTask.TransactionToSign {
                var txToSign: SignTask.TransactionToSign? = null
                suspend fun requestBalanceAndUnspentTransactions(): Boolean = suspendCoroutine { cont ->
                    coinEngine!!.requestBalanceAndUnspentTransactions(object : CoinEngine.BlockchainRequestsCallbacks {
                        override fun onComplete(success: Boolean?) {
                            cont.resume(success!!)
                        }

                        override fun onProgress() {
                        }

                        override fun allowAdvance(): Boolean {
                            return true
                        }
                    })
                }

                fun initData(card: TangemCard) {
                    if (ctx.card == null) {
                        ctx.card = card
                    }
                    if (ctx.coinData == null) {
                        ctx.coinData = CoinEngineFactory.createCardanoData()
                    }
                    if (ctx.coinData.wallet.isNullOrEmpty()) {
                        coinEngine.defineWallet()
                        runBlocking { requestBalanceAndUnspentTransactions() }
                        Log.e(MainActivity.TAG, "requestBalanceAndUnspentTransactions completed")
                    }
                    if (txToSign == null) txToSign = coinEngine!!.constructTransaction(amount, fee, isIncludeFee, outAddressStr)
                }

                override fun isSigningOnCardSupported(card: TangemCard?): Boolean {
                    initData(card!!)
                    return CoinEngineFactory.isCardano(card?.blockchainID) and (txToSign!!.isSigningMethodSupported(card?.signingMethod))
                }

                override fun isIssuerCanSignData(card: TangemCard?): Boolean {
                    return try {
                        card?.issuer?.privateDataKey!=null
                    } catch (e: java.lang.Exception) {
                        false
                    }
                }

                override fun isIssuerCanSignTransaction(card: TangemCard?): Boolean {
                    return try {
                        card?.issuer?.privateTransactionKey!=null
                    } catch (e: java.lang.Exception) {
                        false
                    }
                }

                override fun getHashesToSign(card: TangemCard?): Array<ByteArray> {
                    initData(card!!)
                    return txToSign!!.hashesToSign
                }

                override fun getRawDataToSign(card: TangemCard?): ByteArray {
                    initData(card!!)
                    return txToSign!!.rawDataToSign
                }

                override fun getHashAlgToSign(card: TangemCard?): String {
                    initData(card!!)
                    return txToSign!!.hashAlgToSign
                }

                override fun getIssuerTransactionSignature(card: TangemCard?, dataToSignByIssuer: ByteArray?): ByteArray {
                    initData(card!!)
                    if( card.issuer!=null && card.issuer.privateTransactionKey!=null)
                    {
                        return CardCrypto.Signature(card.issuer.privateTransactionKey, dataToSignByIssuer)
                    }
                    return txToSign!!.getIssuerTransactionSignature(dataToSignByIssuer)
                }

                override fun onSignCompleted(card: TangemCard?, signature: ByteArray?) {
                    initData(card!!)
                    txToSign!!.onSignCompleted(signature)
                }

            }
            task = OneTouchSignTask(NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, this, tx)
            task!!.start()

        } catch (e: CardProtocol.TangemException_WrongAmount) {
            try {
                val intent = Intent()
                intent.putExtra(Constant.EXTRA_MESSAGE, getString(R.string.cannot_sign_transaction_wrong_amount))
                intent.putExtra(EXTRA_TANGEM_CARD_UID, ctx.card.uid)
                intent.putExtra(EXTRA_TANGEM_CARD, ctx.card.asBundle)
                setResult(Activity.RESULT_CANCELED, intent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar.post { rlProgressBar.visibility = View.VISIBLE }

        progressBar?.post {
            progressBar?.visibility = View.VISIBLE
            progressBar?.progress = 5
        }
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar?.post { progressBar!!.progress = progress }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        task = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar.post { rlProgressBar.visibility = View.GONE }

                progressBar?.post {
                    progressBar?.progress = 100
                    progressBar?.progressTintList = ColorStateList.valueOf(Color.GREEN)
                }

                mpFinishSignSound.start()
            } else {
                lastReadSuccess = false
                if (cardProtocol.error.javaClass == CardProtocol.TangemException_InvalidPIN::class.java) {
                    progressBar?.post {
                        progressBar?.progress = 100
                        progressBar?.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                    progressBar?.postDelayed({
                        try {
                            progressBar?.progress = 0
                            progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                            progressBar?.visibility = View.INVISIBLE
                            val intent = Intent()
                            intent.putExtra(Constant.EXTRA_MESSAGE, getString(R.string.cannot_sign_transaction_make_sure_you_enter_correct_pin_2))
                            intent.putExtra(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                            intent.putExtra(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                            setResult(Constant.RESULT_INVALID_PIN_, intent)
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                } else {
                    if (cardProtocol.error is CardProtocol.TangemException_WrongAmount) {
                        try {
                            val intent = Intent()
                            intent.putExtra(Constant.EXTRA_MESSAGE, getString(R.string.cannot_sign_transaction_wrong_amount))
                            intent.putExtra(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                            intent.putExtra(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                            setResult(Activity.RESULT_CANCELED, intent)
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    progressBar?.post {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                                NoExtendedLengthSupportDialog.message = getText(R.string.the_nfc_adapter_length_apdu).toString() + "\n" + getText(R.string.the_nfc_adapter_length_apdu_advice).toString()
                                NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                            }
                        } else {
                            Toast.makeText(baseContext, R.string.try_to_scan_again, Toast.LENGTH_LONG).show()
                        }
                        progressBar?.progress = 100
                        progressBar?.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                }
            }
        }

        rlProgressBar.postDelayed({
            try {
                rlProgressBar.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)

        progressBar?.postDelayed({
            try {
                progressBar?.progress = 0
                progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                progressBar?.visibility = View.INVISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadCancel() {
        task = null

        progressBar?.postDelayed({
            try {
                progressBar?.progress = 0
                progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                progressBar?.visibility = View.INVISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        LOG.i(TAG, "onReadBeforeRequest timeout $timeout")
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)
    }

    override fun onReadAfterRequest() {
        LOG.i(TAG, "onReadAfterRequest")
        WaitSecurityDelayDialog.onReadAfterRequest(this)
    }

    override fun onReadWait(msec: Int) {
        LOG.i(TAG, "onReadWait msec $msec")
        WaitSecurityDelayDialog.onReadWait(this, msec)
    }

}