package com.tangem.ui.fragment.wallet

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.App
import com.tangem.Constant
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_card.tasks.CreateNewWalletTask
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
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fragment_create_new_wallet.*
import kotlinx.android.synthetic.main.layout_progress_horizontal.*
import kotlinx.android.synthetic.main.layout_touch_card.*

class CreateNewWalletFragment : BaseFragment(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    override val layoutId = R.layout.fragment_create_new_wallet

    private val ctx: TangemContext by lazy { TangemContext.loadFromBundle(context, arguments) }

    private lateinit var nfcDeviceAntenna: NfcDeviceAntennaLocation

    private var createNewWalletTask: CreateNewWalletTask? = null
    private var lastReadSuccess = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // init NFC Antenna
        nfcDeviceAntenna = NfcDeviceAntennaLocation(requireContext(), ivHandCardHorizontal, ivHandCardVertical, llHand, llNfc)
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

            createNewWalletTask = CreateNewWalletTask(ctx.card, NfcReader((activity as MainActivity).nfcManager, isoDep),
                    App.localStorage, App.pinStorage, this)
            createNewWalletTask?.start()
        } else
            (activity as MainActivity).nfcManager.ignoreTag(isoDep.tag)
    }

    override fun onPause() {
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
                    val data = Bundle()
                    data.putString(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                    data.putBundle(EXTRA_TANGEM_CARD, cardProtocol.card.asBundle)
                    navigateBackWithResult(Activity.RESULT_OK, data)
                    return@post
                }
            } else {
                FirebaseCrashlytics.getInstance().recordException(cardProtocol.error)
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
                            val data = Bundle()
                            data.putString(Constant.EXTRA_MESSAGE, getString(R.string.nfc_error_cannot_create_wallet))
                            data.putString(EXTRA_TANGEM_CARD_UID, cardProtocol.card.uid)
                            data.putBundle(EXTRA_TANGEM_CARD, cardProtocol.card!!.asBundle)
                            navigateBackWithResult(Constant.RESULT_INVALID_PIN, data)
                            return@postDelayed
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 500)
                } else {
                    progressBar?.post {
                        if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                                NoExtendedLengthSupportDialog().show(activity!!.supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                            }
                        } else
                            Toast.makeText(context, R.string.general_notification_scan_again, Toast.LENGTH_SHORT).show()

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
        WaitSecurityDelayDialog.onReadWait(activity, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(activity, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(activity)
    }

}