package com.tangem.ui.fragment.id

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.App
import com.tangem.Constant
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_card.tasks.WriteIDCardTask
import com.tangem.tangem_card.util.Util
import com.tangem.tangem_sdk.android.nfc.NfcDeviceAntennaLocation
import com.tangem.tangem_sdk.android.reader.NfcReader
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangem_sdk.data.asBundle
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.util.LOG
import com.tangem.util.UtilHelper
import com.tangem.wallet.CoinEngine
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.layout_progress_horizontal.*
import kotlinx.android.synthetic.main.layout_touch_card.*
import kotlinx.android.synthetic.tangemAccess.fragment_sign_transaction.*

class WriteIdFragment : BaseFragment(), NavigationResultListener,
        NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = WriteIdFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_write_id

    private lateinit var ctx: TangemContext
    private lateinit var tx: ByteArray
    private lateinit var mpFinishSignSound: MediaPlayer
    private var toast: Toast? = null

    private var idWasWritten = false

    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation

    private var writeIDCardTask: WriteIDCardTask? = null

    private var lastReadSuccess = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(context, arguments)
        tx = arguments?.getByteArray(Constant.EXTRA_TX)!!

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackWithResult(Activity.RESULT_CANCELED)
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mpFinishSignSound = MediaPlayer.create(context, R.raw.scan_card_sound)

        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(context!!, ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
        nfcDeviceAntenna.init()

        tvCardID.text = ctx.card!!.cidDescription
        progressBar.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar.visibility = View.INVISIBLE
        tvProgressBar?.text = "Writing..."
    }

    override fun onPause() {
        writeIDCardTask?.cancel(true)
        super.onPause()
    }

    override fun onStop() {
        writeIDCardTask?.cancel(true)
        toast?.cancel()
        super.onStop()
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTION_) {
            navigateBackWithResult(resultCode, data)
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            // get IsoDep handle and run cardReader thread
            val isoDep = IsoDep.get(tag)
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)

            if (sUID == ctx.card.uid || !idWasWritten) {
                if (lastReadSuccess)
                    isoDep.timeout = ctx.card.pauseBeforePIN2 + 5000
                else
                    isoDep.timeout = ctx.card.pauseBeforePIN2 + 65000

                writeIDCardTask = WriteIDCardTask(ctx.card, NfcReader((activity as MainActivity).nfcManager, isoDep),
                        App.localStorage, App.pinStorage, ctx.card.idCardData, ctx.card.issuer.privateDataKey, this)
                writeIDCardTask?.start()
            } else
                (activity as MainActivity).nfcManager.ignoreTag(isoDep.tag)

        } catch (e: CardProtocol.TangemException_WrongAmount) {
            try {
                val data = Bundle()
                data.putString(Constant.EXTRA_MESSAGE, getString(R.string.send_transaction_error_wrong_amount))
                data.putString(EXTRA_TANGEM_CARD_UID, ctx.card.uid)
                data.putBundle(EXTRA_TANGEM_CARD, ctx.card.asBundle)
                navigateBackWithResult(Activity.RESULT_CANCELED, data)
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
        progressBar?.post { progressBar?.progress = progress }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        writeIDCardTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar?.post { rlProgressBar?.visibility = View.GONE }

                progressBar?.post {
                    progressBar?.progress = 100
                    progressBar?.progressTintList = ColorStateList.valueOf(Color.GREEN)
                }

                mpFinishSignSound.start()

                idWasWritten = true

                val coinEngine = CoinEngineFactory.create(ctx)

                coinEngine!!.requestSendTransaction(
                        object : CoinEngine.BlockchainRequestsCallbacks {
                            override fun onComplete(success: Boolean) {
                                if (success) {
                                    navigateUp(R.id.main)
                                } else {
                                    toast?.cancel()
                                    navigateUp(R.id.issueNewIdFragment)
                                }
                            }

                            override fun onProgress() {
                            }

                            override fun allowAdvance(): Boolean {
                                return UtilHelper.isOnline(requireContext())
                            }
                        },
                        tx
                )

            } else {
                lastReadSuccess = false
                FirebaseCrashlytics.getInstance().recordException(cardProtocol.error)
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
                            val data = Bundle()
                            data.putString(Constant.EXTRA_MESSAGE, getString(R.string.send_transaction_error_cannot_sign))
                            data.putString(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                            data.putBundle(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                            navigateBackWithResult(Constant.RESULT_INVALID_PIN_, data)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                } else {
                    if (cardProtocol.error is CardProtocol.TangemException_WrongAmount) {
                        try {
                            val data = Bundle()
                            data.putString(Constant.EXTRA_MESSAGE, getString(R.string.send_transaction_error_wrong_amount))
                            data.putString(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                            data.putBundle(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                            navigateBackWithResult(Activity.RESULT_CANCELED, data)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    progressBar?.post {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                                NoExtendedLengthSupportDialog.message = getText(R.string.dialog_the_nfc_adapter_length_apdu).toString() + "\n" + getText(R.string.dialog_the_nfc_adapter_length_apdu_advice).toString()
                                NoExtendedLengthSupportDialog().show(requireFragmentManager(), NoExtendedLengthSupportDialog.TAG)
                            }
                        } else {
                           if (!idWasWritten) {
                               toast = Toast.makeText(context, R.string.general_notification_scan_again, Toast.LENGTH_SHORT)
                               toast?.show()
                           }
                        }
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

    override fun onReadCancel() {
        writeIDCardTask = null

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
        WaitSecurityDelayDialog.onReadBeforeRequest(activity!!, timeout)
    }

    override fun onReadAfterRequest() {
        LOG.i(TAG, "onReadAfterRequest")
        WaitSecurityDelayDialog.onReadAfterRequest(activity!!)
    }

    override fun onReadWait(msec: Int) {
        LOG.i(TAG, "onReadWait msec $msec")
        WaitSecurityDelayDialog.onReadWait(activity!!, msec)
    }

}