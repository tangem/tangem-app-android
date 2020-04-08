package com.tangem.ui.fragment.pin

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.App
import com.tangem.Constant
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_card.tasks.SwapPINTask
import com.tangem.tangem_card.util.Util
import com.tangem.tangem_sdk.android.reader.NfcReader
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangem_sdk.data.asBundle
import com.tangem.tangem_sdk.data.loadFromBundle
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.ui.fragment.BaseFragment
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_pin_swap.*

class PinSwapFragment : BaseFragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {
    companion object {
        val TAG: String = PinSwapFragment::class.java.simpleName

        fun callingIntent(newPIN: String, newPIN2: String): Bundle {
            val bundle = Bundle()
            bundle.putString(Constant.EXTRA_NEW_PIN, newPIN)
            bundle.putString(Constant.EXTRA_NEW_PIN_2, newPIN2)
            return bundle
        }

        const val RESULT_INVALID_PIN = Activity.RESULT_FIRST_USER
    }

    override val layoutId = R.layout.fragment_pin_swap

    private lateinit var card: TangemCard;
    private var newPIN: String? = null
    private var newPIN2: String? = null

    private var progressBar: ProgressBar? = null

    private var swapPinTask: SwapPINTask? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        card = TangemCard(arguments?.getString(EXTRA_TANGEM_CARD_UID))
        card.loadFromBundle(arguments?.getBundle(EXTRA_TANGEM_CARD) ?: Bundle())

        newPIN = arguments?.getString(Constant.EXTRA_NEW_PIN)
        newPIN2 = arguments?.getString(Constant.EXTRA_NEW_PIN_2)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvCardID.text = card.cidDescription

        progressBar?.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar?.visibility = View.INVISIBLE
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
            if (sUID == card.uid) {
                isoDep.timeout = card.pauseBeforePIN2 + 65000
                swapPinTask = SwapPINTask(card, NfcReader((activity as MainActivity).nfcManager, isoDep),
                        App.localStorage, App.pinStorage, this, newPIN, newPIN2)
                swapPinTask?.start()
            } else {
                (activity as MainActivity).nfcManager.ignoreTag(isoDep.tag)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    public override fun onPause() {
        swapPinTask?.cancel(true)
        super.onPause()
    }

    public override fun onStop() {
        swapPinTask?.cancel(true)
        super.onStop()
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        progressBar?.post {
            progressBar?.visibility = View.VISIBLE
            progressBar?.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        swapPinTask = null

        if (cardProtocol != null) {
            cardProtocol.error?.let { FirebaseCrashlytics.getInstance().recordException(it) }
            when {
                cardProtocol.error == null -> progressBar!!.post {
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.GREEN)
                    val data = Bundle()
                    data.putString(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                    data.putBundle(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                    navigateBackWithResult(Activity.RESULT_OK, data)
                }
                cardProtocol.error is CardProtocol.TangemException_InvalidPIN -> {
                    progressBar!!.post {
                        progressBar!!.progress = 100
                        progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
                    }
                    progressBar!!.postDelayed({
                        try {
                            progressBar!!.progress = 0
                            progressBar!!.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
                            progressBar!!.visibility = View.INVISIBLE
                            val data = Bundle()
                            data.putString(Constant.EXTRA_MESSAGE, "Cannot change PIN(s). Make sure you enter correct PIN2!")
                            data.putString(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                            data.putBundle(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                            navigateBackWithResult(RESULT_INVALID_PIN, data)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                }
                else -> progressBar!!.post {
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                        if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                            NoExtendedLengthSupportDialog().show(activity!!.supportFragmentManager,
                                    NoExtendedLengthSupportDialog.TAG)
                        }
                    } else {
                        Toast.makeText(context, R.string.general_notification_scan_again, Toast.LENGTH_SHORT).show()
                    }
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
                }
            }

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

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar!!.post { progressBar!!.progress = progress }
    }

    override fun onReadCancel() {
        swapPinTask = null

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
        WaitSecurityDelayDialog.onReadWait(activity, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(activity, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(activity)
    }

}