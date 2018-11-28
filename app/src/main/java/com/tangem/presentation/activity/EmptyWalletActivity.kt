package com.tangem.presentation.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.View
import android.widget.Toast
import com.tangem.data.nfc.VerifyCardTask
import com.tangem.domain.cardReader.CardProtocol
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.TangemCard
import com.tangem.domain.wallet.TangemContext
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_empty_wallet.*

class EmptyWalletActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = EmptyWalletActivity::class.java.simpleName

        private const val REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY = 2
        private const val REQUEST_CODE_REQUEST_PIN2 = 3
        private const val REQUEST_CODE_VERIFY_CARD = 4

        fun callingIntent(context: Context) = Intent(context, EmptyWalletActivity::class.java)
    }

    private var nfcManager: NfcManager? = null
    private lateinit var ctx: TangemContext
    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty_wallet)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        tvIssuer.text = ctx.card!!.issuerDescription
        //tvBlockchain.text = ctx.card!!.blockchainName
        if (ctx.card!!.tokenSymbol.length > 1) {
            val html = Html.fromHtml(ctx.card!!.blockchainName)
            tvBlockchain.text = html
        } else
            tvBlockchain.text = ctx.card!!.blockchainName

        tvCardID.text = ctx.card!!.cidDescription
        imgBlockchain.setImageResource(ctx.card!!.blockchain.getImageResource(this, ctx.card!!.tokenSymbol))

        if (ctx.card!!.useDefaultPIN1()!!) {
            imgPIN.setImageResource(R.drawable.unlock_pin1)
            imgPIN.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.this_banknote_protected_default_PIN1_code, Toast.LENGTH_LONG).show() }
        } else {
            imgPIN.setImageResource(R.drawable.lock_pin1)
            imgPIN.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.this_banknote_protected_user_PIN1_code, Toast.LENGTH_LONG).show() }
        }

        if (ctx.card!!.pauseBeforePIN2 > 0 && (ctx.card!!.useDefaultPIN2()!! || !ctx.card!!.useSmartSecurityDelay())) {
            imgPIN2orSecurityDelay.setImageResource(R.drawable.timer)
            imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, String.format(getString(R.string.this_banknote_will_enforce), ctx.card!!.pauseBeforePIN2 / 1000.0), Toast.LENGTH_LONG).show() }

        } else if (ctx.card!!.useDefaultPIN2()!!) {
            imgPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2)
            imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.this_banknote_protected_default_PIN2_code, Toast.LENGTH_LONG).show() }
        } else {
            imgPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2)
            imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.this_banknote_protected_user_PIN2_code, Toast.LENGTH_LONG).show() }
        }

        if (ctx.card!!.useDevelopersFirmware()!!) {
            imgDeveloperVersion.setImageResource(R.drawable.ic_developer_version)
            imgDeveloperVersion.visibility = View.VISIBLE
            imgDeveloperVersion.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.unlocked_banknote_only_development_use, Toast.LENGTH_LONG).show() }
        } else
            imgDeveloperVersion.visibility = View.INVISIBLE

        // set listeners
        btnNewWallet.setOnClickListener {
            requestPIN2Count = 0
            val intent = Intent(baseContext, PinRequestActivity::class.java)
            intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
            intent.putExtra("UID", ctx.card!!.uid)
            intent.putExtra("Card", ctx.card!!.asBundle)
            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2)
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
        if (requestCode == REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {

                if (data != null) {
                    data.putExtra("modification", "updateAndViewCard")
                    data.putExtra("updateDelay", 0)
                    setResult(Activity.RESULT_OK, data)
                }
                finish()
            } else {
                if (data != null && data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                    val updatedCard = TangemCard(data.getStringExtra("UID"))
                    updatedCard.loadFromBundle(data.getBundleExtra("Card"))
                    ctx.card = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(baseContext, PinRequestActivity::class.java)
                    intent.putExtra("mode", PinRequestActivity.Mode.RequestPIN2.toString())
                    intent.putExtra("UID", ctx.card!!.uid)
                    intent.putExtra("Card", ctx.card!!.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2)
                    return
                }
            }
            setResult(resultCode, data)
            finish()
        } else if (requestCode == REQUEST_CODE_REQUEST_PIN2) {
            if (resultCode == Activity.RESULT_OK) {
                doCreateNewWallet()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            val isoDep = IsoDep.get(tag)
                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
            if (ctx.card!!.uid != sUID) {
//                Log.d(TAG, "Invalid UID: " + sUID);
                nfcManager!!.ignoreTag(isoDep.tag)
                return
            } else {
//                Log.v(TAG, "UID: " + sUID);
            }

            if (lastReadSuccess) {
                isoDep.timeout = 1000
            } else {
                isoDep.timeout = 65000
            }
            //lastTag = tag;
            verifyCardTask = VerifyCardTask(this, ctx.card, nfcManager, isoDep, this)
            verifyCardTask!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        progressBar!!.post {
            progressBar!!.visibility = View.VISIBLE
            progressBar!!.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        verifyCardTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                progressBar!!.post {
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.GREEN)
//                    val intent = Intent(this@EmptyWalletActivity, VerifyCardActivity::class.java)
//                    intent.putExtra("UID", cardProtocol.card.uid)
//                    intent.putExtra("Card", cardProtocol.card.asBundle)
//                    startActivityForResult(intent, REQUEST_CODE_VERIFY_CARD)
                    //addCard(cardProtocol.getCard());
                }
            } else {
                // remove last UIDs because of error and no card read
                progressBar!!.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                        if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                            NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                        }
                    } else {
                        Toast.makeText(this@EmptyWalletActivity, R.string.try_to_scan_again, Toast.LENGTH_LONG).show()
                    }
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
                }
            }
        }

        progressBar!!.postDelayed({
            try {
                progressBar!!.progress = 0
                progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                progressBar!!.visibility = View.INVISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar!!.post { progressBar!!.progress = progress }
    }

    override fun onReadCancel() {
        verifyCardTask = null
        progressBar!!.postDelayed({
            try {
                progressBar!!.progress = 0
                progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                progressBar!!.visibility = View.INVISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.OnReadWait(this, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this)
    }

    private fun doCreateNewWallet() {
        val intent = Intent(this, CreateNewWalletActivity::class.java)
        intent.putExtra("UID", ctx.card!!.uid)
        intent.putExtra("Card", ctx.card!!.asBundle)
        startActivityForResult(intent, REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY)
    }

}