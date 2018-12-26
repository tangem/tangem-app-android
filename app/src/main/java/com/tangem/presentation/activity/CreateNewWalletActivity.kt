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
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.tangem.App
import com.tangem.Constant
import com.tangem.domain.wallet.TangemContext
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.tangemcard.android.reader.NfcManager
import com.tangem.tangemcard.android.reader.NfcReader
import com.tangem.tangemcard.data.asBundle
import com.tangem.tangemcard.reader.CardProtocol
import com.tangem.tangemcard.tasks.CreateNewWalletTask
import com.tangem.tangemcard.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_create_new_wallet.*

class CreateNewWalletActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        fun callingIntent(context: Context, ctx: TangemContext): Intent {
            val intent = Intent(context, CreateNewWalletActivity::class.java)
            intent.putExtra("UID", ctx.card!!.uid)
            intent.putExtra("Card", ctx.card!!.asBundle)
            return intent
        }
    }

    private lateinit var ctx: TangemContext
    private lateinit var nfcManager: NfcManager

    private var createNewWalletTask: CreateNewWalletTask? = null
    private var lastReadSuccess = true

    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_wallet)

        nfcManager = NfcManager(this, this)

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        tvCardId.text = ctx.card!!.cidDescription

        progressBar = findViewById(R.id.progressBar)
        progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar!!.visibility = View.INVISIBLE
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
//            Log.v(TAG, "UID: " + sUID);

            if (sUID == ctx.card!!.uid) {
                if (lastReadSuccess) {
                    isoDep.timeout = ctx.card!!.pauseBeforePIN2 + 5000
                } else {
                    isoDep.timeout = ctx.card!!.pauseBeforePIN2 + 65000
                }
                createNewWalletTask = CreateNewWalletTask(ctx.card, NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, this)
                createNewWalletTask!!.start()
            } else {
//                Log.d(TAG, "Mismatch card UID (" + sUID + " instead of " + mCard.getUID() + ")");
                nfcManager.ignoreTag(isoDep.tag)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onResume() {
        super.onResume()
        nfcManager.onResume()
    }

    public override fun onPause() {
        nfcManager.onPause()
        if (createNewWalletTask != null)
            createNewWalletTask!!.cancel(true)
        super.onPause()
    }

    public override fun onStop() {
        // dismiss enable NFC dialog
        nfcManager.onStop()
        if (createNewWalletTask != null)
            createNewWalletTask!!.cancel(true)
        super.onStop()
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        progressBar!!.post {
            progressBar!!.visibility = View.VISIBLE
            progressBar!!.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        createNewWalletTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                progressBar!!.post {
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.GREEN)
                    val intent = Intent()
                    intent.putExtra("UID", cardProtocol.card.uid)
                    intent.putExtra("Card", cardProtocol.card.asBundle)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            } else {
                lastReadSuccess = false
                if (cardProtocol.error is CardProtocol.TangemException_InvalidPIN) {
                    progressBar!!.post {
                        progressBar!!.progress = 100
                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                    progressBar!!.postDelayed({
                        try {
                            progressBar!!.progress = 0
                            progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                            progressBar!!.visibility = View.INVISIBLE
                            val intent = Intent()
                            intent.putExtra("message", "Cannot create wallet. Make sure you enter correct PIN2!")
                            intent.putExtra("UID", cardProtocol.card.uid)
                            intent.putExtra("Card", cardProtocol.card!!.asBundle)
                            setResult(Constant.RESULT_INVALID_PIN, intent)
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                } else {
                    progressBar!!.post {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                                NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                            }
                        } else {
                            Toast.makeText(baseContext, R.string.try_to_scan_again, Toast.LENGTH_SHORT).show()
                        }
                        progressBar!!.progress = 100
                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
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
        createNewWalletTask = null

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
        WaitSecurityDelayDialog.onReadWait(this, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this)
    }

}