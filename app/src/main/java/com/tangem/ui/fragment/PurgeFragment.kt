package com.tangem.ui.fragment

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
import androidx.core.os.bundleOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.App
import com.tangem.Constant
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_card.tasks.PurgeTask
import com.tangem.tangem_card.util.Util
import com.tangem.tangem_sdk.android.nfc.NfcDeviceAntennaLocation
import com.tangem.tangem_sdk.android.reader.NfcReader
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangem_sdk.data.asBundle
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialogNew
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fragment_purge.*
import kotlinx.android.synthetic.main.layout_touch_card.*
import javax.inject.Inject

class PurgeFragment : BaseFragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {
    companion object {
        val TAG: String = PurgeFragment::class.java.simpleName

        fun callingIntent(context: Context, ctx: TangemContext): Intent {
            val intent = Intent(context, PurgeFragment::class.java)
            ctx.saveToIntent(intent)
            return intent
        }

        const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER
    }

    @Inject
    internal lateinit var waitSecurityDelayDialogNew: WaitSecurityDelayDialogNew

    override val layoutId = R.layout.fragment_purge

    private lateinit var ctx: TangemContext

    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation

    private var purgeTask: PurgeTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(context, arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(context!!, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
        nfcDeviceAntenna.init()

        tvCardID.text = ctx.card.cidDescription
        progressBar.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar.visibility = View.INVISIBLE
    }

    override fun onStop() {
        purgeTask?.cancel(true)
        super.onStop()
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
            if (sUID == ctx.card.uid) {
                isoDep.timeout = ctx.card.pauseBeforePIN2 + 65000
                purgeTask = PurgeTask(ctx.card, NfcReader((activity as MainActivity).nfcManager, isoDep),
                        App.localStorage, App.pinStorage, this)
                purgeTask?.start()
            } else {
                (activity as MainActivity).nfcManager.ignoreTag(isoDep.tag)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.onReadWait(activity, msec)

//        val readWait = ReadWait()
//        readWait.msec = msec
//        EventBus.getDefault().post(readWait)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(activity, timeout)

//        if (!waitSecurityDelayDialogNew.isAdded)
//            waitSecurityDelayDialogNew.show(supportFragmentManager, WaitSecurityDelayDialogNew.TAG)
//
//
//        val readBeforeRequest = ReadBeforeRequest()
//        readBeforeRequest.timeout = timeout
//        EventBus.getDefault().post(readBeforeRequest)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(activity)

//        val readAfterRequest = ReadAfterRequest()
//        EventBus.getDefault().post(readAfterRequest)
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar.post { rlProgressBar.visibility = View.VISIBLE }

        progressBar.post {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        purgeTask = null

        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar?.post { rlProgressBar?.visibility = View.GONE }

                progressBar?.post {
                    progressBar?.progress = 100
                    progressBar?.progressTintList = ColorStateList.valueOf(Color.GREEN)

                    val data = Bundle()
                    data.putString(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                    data.putBundle(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)

                    navigateToDestination(R.id.action_purgeFragment_to_mainFragment)
                }
            } else {
                FirebaseCrashlytics.getInstance().recordException(cardProtocol.error)
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

                            val data = bundleOf(
                                    EXTRA_TANGEM_CARD_UID to cardProtocol.card.uid,
                                    EXTRA_TANGEM_CARD to cardProtocol.card.asBundle,
                                    Constant.EXTRA_MESSAGE to getString(R.string.nfc_error_cannot_erase_wallet)
                            )
                            navigateBackWithResult(RESULT_INVALID_PIN, data)
                            return@postDelayed
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                } else {
                    progressBar?.post {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allReadyShowed)
                                NoExtendedLengthSupportDialog().show(activity!!.supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                        } else
                            Toast.makeText(context, R.string.general_notification_scan_again_to_verify, Toast.LENGTH_LONG).show()

                        progressBar?.progress = 100
                        progressBar?.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                }
            }
        }

        rlProgressBar?.postDelayed({
            try {
                rlProgressBar?.visibility = View.GONE
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

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar?.post { progressBar?.progress = progress }
    }

    override fun onReadCancel() {
        purgeTask = null
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

}