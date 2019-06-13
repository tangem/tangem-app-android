package com.tangem.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tangem.App
import com.tangem.Constant
import com.tangem.wallet.TangemContext
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.card_android.android.nfc.NfcDeviceAntennaLocation
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.card_android.android.reader.NfcReader
import com.tangem.card_android.data.asBundle
import com.tangem.card_common.reader.CardProtocol
import com.tangem.card_common.tasks.CreateNewWalletTask
import com.tangem.card_common.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_create_new_wallet.*
import kotlinx.android.synthetic.main.layout_progress_horizontal.*
import kotlinx.android.synthetic.main.layout_touch_card.*
import com.tangem.card_android.data.EXTRA_TANGEM_CARD
import com.tangem.card_android.data.EXTRA_TANGEM_CARD_UID

class CreateNewWalletActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        fun callingIntent(context: Context, ctx: TangemContext): Intent {
            val intent = Intent(context, CreateNewWalletActivity::class.java)
            intent.putExtra(EXTRA_TANGEM_CARD_UID, ctx.card!!.uid)
            intent.putExtra(EXTRA_TANGEM_CARD, ctx.card!!.asBundle)
            return intent
        }
    }

    private lateinit var nfcManager: NfcManager
    private lateinit var ctx: TangemContext

    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation

    private var createNewWalletTask: CreateNewWalletTask? = null
    private var lastReadSuccess = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_wallet)

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(this, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
        nfcDeviceAntenna.init()

        tvCardId.text = ctx.card!!.cidDescription
        progressBar.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar.visibility = View.INVISIBLE
    }

    override fun onTagDiscovered(tag: Tag) {
        // get IsoDep handle and run cardReader thread
        val isoDep = IsoDep.get(tag)
        val uid = tag.id
        val sUID = Util.byteArrayToHexString(uid)

        if (sUID == ctx.card.uid) {
            if (lastReadSuccess)
                isoDep.timeout = ctx.card.pauseBeforePIN2 + 5000
            else
                isoDep.timeout = ctx.card.pauseBeforePIN2 + 65000

            createNewWalletTask = CreateNewWalletTask(ctx.card, NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, this)
            createNewWalletTask?.start()
        } else
            nfcManager.ignoreTag(isoDep.tag)
    }

    public override fun onPause() {
        createNewWalletTask?.cancel(true)
        super.onPause()
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar?.post { rlProgressBar.visibility = View.VISIBLE }

        progressBar?.post {
            progressBar?.visibility = View.VISIBLE
            progressBar?.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        createNewWalletTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar.post { rlProgressBar.visibility = View.GONE }

                progressBar?.post {
                    progressBar?.progress = 100
                    progressBar?.progressTintList = ColorStateList.valueOf(Color.GREEN)
                    val intent = Intent()
                    intent.putExtra(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                    intent.putExtra(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            } else {
                lastReadSuccess = false
                if (cardProtocol.error is CardProtocol.TangemException_InvalidPIN) {
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
                            intent.putExtra(Constant.EXTRA_MESSAGE, getString(R.string.cannot_create_wallet))
                            intent.putExtra(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                            intent.putExtra(EXTRA_TANGEM_CARD, cardProtocol.card!!.asBundle)
                            setResult(Constant.RESULT_INVALID_PIN, intent)
                            finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                } else {
                    progressBar?.post {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                                NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                            }
                        } else
                            Toast.makeText(this, R.string.try_to_scan_again, Toast.LENGTH_SHORT).show()

                        progressBar?.progress = 100
                        progressBar?.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                }
            }
        }

        rlProgressBar.postDelayed({ rlProgressBar?.visibility = View.GONE }, 500)

        progressBar?.postDelayed({
            progressBar?.progress = 0
            progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
            progressBar?.visibility = View.INVISIBLE
        }, 500)
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar?.post { progressBar?.progress = progress }
    }

    override fun onReadCancel() {
        createNewWalletTask = null

        progressBar?.postDelayed({
            progressBar?.progress = 0
            progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
            progressBar?.visibility = View.INVISIBLE
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