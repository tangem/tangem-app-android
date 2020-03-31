package com.tangem.ui

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
import com.tangem.tangem_card.tasks.SignTask
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
import com.tangem.wallet.CoinEngine
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.layout_progress_horizontal.*
import kotlinx.android.synthetic.main.layout_touch_card.*
import kotlinx.android.synthetic.tangemAccess.fragment_sign_transaction.*


class SignTransactionFragment : BaseFragment(), NavigationResultListener,
        NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = SignTransactionFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_sign_transaction

    private lateinit var ctx: TangemContext
    private lateinit var mpFinishSignSound: MediaPlayer

    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation

    private var signTransactionTask: SignTask? = null

    private lateinit var amount: CoinEngine.Amount
    private lateinit var fee: CoinEngine.Amount
    private var isIncludeFee = true
    private var outAddressStr: String? = null
    private var lastReadSuccess = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(context, arguments)

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

        amount = CoinEngine.Amount(arguments?.getString(Constant.EXTRA_AMOUNT), arguments?.getString(Constant.EXTRA_AMOUNT_CURRENCY))
        fee = CoinEngine.Amount(arguments?.getString(Constant.EXTRA_FEE), arguments?.getString(Constant.EXTRA_FEE_CURRENCY))
        isIncludeFee = arguments?.getBoolean(Constant.EXTRA_FEE_INCLUDED, true) ?: true
        outAddressStr = arguments?.getString(Constant.EXTRA_TARGET_ADDRESS)

        tvCardID.text = ctx.card!!.cidDescription
        progressBar.progressTintList = ColorStateList.valueOf(Color.DKGRAY)
        progressBar.visibility = View.INVISIBLE
    }

    override fun onPause() {
        signTransactionTask?.cancel(true)
        super.onPause()
    }

    override fun onStop() {
        signTransactionTask?.cancel(true)
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

            if (sUID == ctx.card.uid) {
                if (lastReadSuccess)
                    isoDep.timeout = ctx.card.pauseBeforePIN2 + 5000
                else
                    isoDep.timeout = ctx.card.pauseBeforePIN2 + 65000

                val coinEngine = CoinEngineFactory.create(ctx)
                coinEngine?.setOnNeedSendTransaction { tx ->
                    if (tx != null) {
                        val data = Bundle()
                        ctx.saveToBundle(data)
                        data.putByteArray(Constant.EXTRA_TX, tx)
                        navigateForResult(
                                Constant.REQUEST_CODE_SEND_TRANSACTION_,
                                R.id.action_signTransactionFragment_to_sendTransactionFragment,
                                data)
                    }
                }
                val transactionToSign = coinEngine?.constructTransaction(amount, fee, isIncludeFee, outAddressStr)

                signTransactionTask = SignTask(ctx.card, NfcReader((activity as MainActivity).nfcManager, isoDep),
                        App.localStorage, App.pinStorage, this, transactionToSign)
                signTransactionTask?.start()
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
        } catch(e: IllegalArgumentException) {
            val data = Bundle()
            data.putString(Constant.EXTRA_MESSAGE, e.message)
            navigateBackWithResult(Activity.RESULT_CANCELED, data, R.id.loadedWalletFragment)
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
        signTransactionTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar?.post { rlProgressBar?.visibility = View.GONE }

                progressBar?.post {
                    progressBar?.progress = 100
                    progressBar?.progressTintList = ColorStateList.valueOf(Color.GREEN)
                }

                mpFinishSignSound.start()
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
                            Toast.makeText(context, R.string.general_notification_scan_again, Toast.LENGTH_LONG).show()
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
        signTransactionTask = null

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

//    private val waitSecurityDelayDialogNew = WaitSecurityDelayDialogNew()

    override fun onReadBeforeRequest(timeout: Int) {
        LOG.i(TAG, "onReadBeforeRequest timeout $timeout")
        WaitSecurityDelayDialog.onReadBeforeRequest(activity!!, timeout)

//        if (!waitSecurityDelayDialogNew.isAdded)
//            waitSecurityDelayDialogNew.show(supportFragmentManager, WaitSecurityDelayDialogNew.TAG)


//        val readBeforeRequest = ReadBeforeRequest()
//        readBeforeRequest.timeout = timeout
//        EventBus.getDefault().post(readBeforeRequest)
    }

    override fun onReadAfterRequest() {
        LOG.i(TAG, "onReadAfterRequest")
        WaitSecurityDelayDialog.onReadAfterRequest(activity!!)

//        val readAfterRequest = ReadAfterRequest()
//        EventBus.getDefault().post(readAfterRequest)
    }

    override fun onReadWait(msec: Int) {
        LOG.i(TAG, "onReadWait msec $msec")
        WaitSecurityDelayDialog.onReadWait(activity!!, msec)

//        val readWait = ReadWait()
//        readWait.msec = msec
//        EventBus.getDefault().post(readWait)
    }

}