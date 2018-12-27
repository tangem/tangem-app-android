package com.tangem.presentation.activityimport android.app.Activityimport android.content.Intentimport android.content.res.ColorStateListimport android.graphics.Colorimport android.nfc.NfcAdapterimport android.nfc.Tagimport android.nfc.tech.IsoDepimport android.os.Bundleimport android.support.v7.app.AppCompatActivityimport android.view.KeyEventimport android.view.Viewimport android.widget.ProgressBarimport android.widget.Toastimport com.tangem.Appimport com.tangem.Constantimport com.tangem.domain.wallet.CoinEngineimport com.tangem.domain.wallet.CoinEngineFactoryimport com.tangem.domain.wallet.TangemContextimport com.tangem.presentation.dialog.NoExtendedLengthSupportDialogimport com.tangem.presentation.dialog.WaitSecurityDelayDialogimport com.tangem.presentation.dialog.WaitSecurityDelayDialogNewimport com.tangem.presentation.event.ReadAfterRequestimport com.tangem.presentation.event.ReadBeforeRequestimport com.tangem.presentation.event.ReadWaitimport com.tangem.presentation.event.TransactionFinishWithErrorimport com.tangem.tangemcard.reader.CardProtocolimport com.tangem.tangemcard.android.reader.NfcManagerimport com.tangem.tangemcard.android.reader.NfcReaderimport com.tangem.tangemcard.data.asBundleimport com.tangem.tangemcard.tasks.SignTaskimport com.tangem.tangemcard.util.Utilimport com.tangem.util.LOGimport com.tangem.wallet.Rimport kotlinx.android.synthetic.main.activity_sign_payment.*import org.greenrobot.eventbus.EventBusclass SignPaymentActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {    companion object {        val TAG: String = SignPaymentActivity::class.java.simpleName    }    private lateinit var nfcManager: NfcManager    private lateinit var ctx: TangemContext    private var signPaymentTask: SignTask? = null    private lateinit var amount: CoinEngine.Amount    private lateinit var fee: CoinEngine.Amount    private var isIncludeFee = true    private var outAddressStr: String? = null    private var lastReadSuccess = true    private var progressBar: ProgressBar? = null    override fun onCreate(savedInstanceState: Bundle?) {        super.onCreate(savedInstanceState)        setContentView(R.layout.activity_sign_payment)        nfcManager = NfcManager(this, this)        ctx = TangemContext.loadFromBundle(this, intent.extras)        amount = CoinEngine.Amount(intent.getStringExtra(Constant.EXTRA_AMOUNT), intent.getStringExtra(Constant.EXTRA_AMOUNT_CURRENCY))        fee = CoinEngine.Amount(intent.getStringExtra(Constant.EXTRA_FEE), intent.getStringExtra(Constant.EXTRA_FEE_CURRENCY))        isIncludeFee = intent.getBooleanExtra(Constant.EXTRA_FEE_INCLUDED, true)        outAddressStr = intent.getStringExtra(Constant.EXTRA_TARGET_ADDRESS)        tvCardID.text = ctx.card!!.cidDescription        progressBar = findViewById(R.id.progressBar)        progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)        progressBar!!.visibility = View.INVISIBLE    }    public override fun onResume() {        super.onResume()        nfcManager.onResume()    }    public override fun onPause() {        nfcManager.onPause()        if (signPaymentTask != null)            signPaymentTask!!.cancel(true)        super.onPause()    }    public override fun onStop() {        // dismiss enable NFC dialog        nfcManager.onStop()        if (signPaymentTask != null)            signPaymentTask!!.cancel(true)        super.onStop()    }    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {        if (requestCode == Constant.REQUEST_CODE_SEND_PAYMENT_) {            setResult(resultCode, data)            finish()            return        }        super.onActivityResult(requestCode, resultCode, data)    }    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {        when (keyCode) {            KeyEvent.KEYCODE_BACK -> {                val intent = Intent()                setResult(Activity.RESULT_CANCELED, intent)                finish()                return true            }        }        return super.onKeyDown(keyCode, event)    }    override fun onTagDiscovered(tag: Tag) {        try {            // get IsoDep handle and run cardReader thread            val isoDep = IsoDep.get(tag)                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))            val uid = tag.id            val sUID = Util.byteArrayToHexString(uid)            if (sUID == ctx.card!!.uid) {                if (lastReadSuccess) {                    isoDep.timeout = ctx.card!!.pauseBeforePIN2 + 5000                } else {                    isoDep.timeout = ctx.card!!.pauseBeforePIN2 + 65000                }                val coinEngine = CoinEngineFactory.create(ctx)                        ?: throw CardProtocol.TangemException("Can't create CoinEngine!")                coinEngine.setOnNeedSendPayment { tx ->                    if (tx != null) {                        val intent = Intent(this, SendTransactionActivity::class.java)                        ctx.saveToIntent(intent)                        intent.putExtra(Constant.EXTRA_TX, tx)                        startActivityForResult(intent, Constant.REQUEST_CODE_SEND_PAYMENT_)                    }                }                val paymentToSign = coinEngine.constructPayment(amount, fee, isIncludeFee, outAddressStr)                signPaymentTask = SignTask(ctx.card, NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, this, paymentToSign)                signPaymentTask!!.start()            } else                nfcManager.ignoreTag(isoDep.tag)        } catch (e: CardProtocol.TangemException_WrongAmount) {            try {                val intent = Intent()                intent.putExtra("message", getString(R.string.cannot_sign_transaction_wrong_amount))                intent.putExtra("UID", ctx.card.uid)                intent.putExtra("Card", ctx.card.asBundle)                setResult(Activity.RESULT_CANCELED, intent)                finish()            } catch (e: Exception) {                e.printStackTrace()            }        } catch (e: Exception) {            e.printStackTrace()        }    }    override fun onReadStart(cardProtocol: CardProtocol) {        progressBar!!.post {            progressBar!!.visibility = View.VISIBLE            progressBar!!.progress = 5        }    }    override fun onReadProgress(protocol: CardProtocol, progress: Int) {        progressBar!!.post { progressBar!!.progress = progress }    }    override fun onReadFinish(cardProtocol: CardProtocol?) {        signPaymentTask = null        if (cardProtocol != null) {            if (cardProtocol.error == null) {                progressBar!!.post {                    progressBar!!.progress = 100                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.GREEN)                }            } else {                lastReadSuccess = false                if (cardProtocol.error.javaClass == CardProtocol.TangemException_InvalidPIN::class.java) {                    progressBar!!.post {                        progressBar!!.progress = 100                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)                    }                    progressBar!!.postDelayed({                        try {                            progressBar!!.progress = 0                            progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)                            progressBar!!.visibility = View.INVISIBLE                            val intent = Intent()                            intent.putExtra("message", getString(R.string.cannot_sign_transaction_make_sure_you_enter_correct_pin_2))                            intent.putExtra("UID", cardProtocol.card.uid)                            intent.putExtra("Card", cardProtocol.card.asBundle)                            setResult(Constant.RESULT_INVALID_PIN_, intent)                            finish()                        } catch (e: Exception) {                            e.printStackTrace()                        }                    }, 500)                } else {                    if (cardProtocol.error is CardProtocol.TangemException_WrongAmount) {                        try {                            val intent = Intent()                            intent.putExtra("message", getString(R.string.cannot_sign_transaction_wrong_amount))                            intent.putExtra("UID", cardProtocol.card.uid)                            intent.putExtra("Card", cardProtocol.card.asBundle)                            setResult(Activity.RESULT_CANCELED, intent)                            finish()                        } catch (e: Exception) {                            e.printStackTrace()                        }                    }                    progressBar!!.post {                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {                            if (!NoExtendedLengthSupportDialog.allReadyShowed) {                                NoExtendedLengthSupportDialog.message = getText(R.string.the_nfc_adapter_length_apdu).toString() + "\n" + getText(R.string.the_nfc_adapter_length_apdu_advice).toString()                                NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)                            }                        } else {                            Toast.makeText(baseContext, R.string.try_to_scan_again, Toast.LENGTH_LONG).show()                        }                        progressBar!!.progress = 100                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)                    }                }            }        }        progressBar!!.postDelayed({            try {                progressBar!!.progress = 0                progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)                progressBar!!.visibility = View.INVISIBLE            } catch (e: Exception) {                e.printStackTrace()            }        }, 500)    }    override fun onReadCancel() {        signPaymentTask = null        progressBar!!.postDelayed({            try {                progressBar!!.progress = 0                progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)                progressBar!!.visibility = View.INVISIBLE            } catch (e: Exception) {                e.printStackTrace()            }        }, 500)    }    private val waitSecurityDelayDialogNew = WaitSecurityDelayDialogNew()    override fun onReadBeforeRequest(timeout: Int) {//        LOG.i(TAG, "onReadBeforeRequest timeout $timeout")//        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)        if (!waitSecurityDelayDialogNew.isAdded)            waitSecurityDelayDialogNew.show(supportFragmentManager, WaitSecurityDelayDialogNew.TAG)        val readBeforeRequest = ReadBeforeRequest()        readBeforeRequest.timeout = timeout        EventBus.getDefault().post(readBeforeRequest)    }    override fun onReadAfterRequest() {//        LOG.i(TAG, "onReadAfterRequest")//        WaitSecurityDelayDialog.onReadAfterRequest(this)        val readAfterRequest = ReadAfterRequest()        EventBus.getDefault().post(readAfterRequest)    }    override fun onReadWait(msec: Int) {//        LOG.i(TAG, "onReadWait msec $msec")//        WaitSecurityDelayDialog.onReadWait(this, msec)        val readWait = ReadWait()        readWait.msec = msec        EventBus.getDefault().post(readWait)    }}