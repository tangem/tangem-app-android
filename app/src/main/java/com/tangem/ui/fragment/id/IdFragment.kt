package com.tangem.ui.fragment.id

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.App
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.data.network.ServerApiCommon
import com.tangem.server_android.ServerApiTangem
import com.tangem.server_android.model.CardVerifyAndGetInfo
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_card.tasks.VerifyCardTask
import com.tangem.tangem_card.util.Util
import com.tangem.tangem_sdk.android.reader.NfcReader
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.ui.fragment.BaseFragment
import com.tangem.util.LOG
import com.tangem.util.UtilHelper
import com.tangem.wallet.*
import kotlinx.android.synthetic.main.fragment_id.*
import kotlinx.android.synthetic.main.layout_btn_details.*
import kotlinx.android.synthetic.main.layout_id.*
import kotlinx.android.synthetic.main.layout_tangem_card.*
import kotlinx.android.synthetic.main.layout_tangem_card.rlProgressBar
import net.i2p.crypto.eddsa.Utils
import java.util.*
import kotlin.concurrent.timerTask

class IdFragment : BaseFragment(), NfcAdapter.ReaderCallback,
        CardProtocol.Notifications, SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val TAG: String = IdFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_id

    private lateinit var ctx: TangemContext
    private lateinit var mpSecondScanSound: MediaPlayer
    private var serverApiCommon: ServerApiCommon = ServerApiCommon()
    private var serverApiTangem: ServerApiTangem = ServerApiTangem()
    private var lastTag: Tag? = null
    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""
    private var photo: ByteArray? = null
    private var cardProtocol: CardProtocol? = null
    private var refreshAction: Runnable? = null
    private var hasIdInfo: Boolean? = null
    private var toast: Toast? = null

    private var requestCounter: Int = 0
        set(value) {
            field = value
            LOG.i(TAG, "requestCounter, set $field")
            if (field <= 0) {
                LOG.e(TAG, "+++++++++++ FINISH REFRESH")
                if (srl != null && srl.isRefreshing)
                    srl.isRefreshing = false
            } else if (srl != null && !srl.isRefreshing)
                srl.isRefreshing = true
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(context, arguments)

        lastTag = activity?.intent?.getParcelableExtra(Constant.EXTRA_LAST_DISCOVERED_TAG)

        if (arguments?.containsKey(NfcAdapter.EXTRA_TAG) == true) {
            val tag = arguments!!.getParcelable<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) onTagDiscovered(tag)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mpSecondScanSound = MediaPlayer.create(activity, R.raw.scan_card_sound)

        tvIdNumber?.text = "ID # ${Utils.bytesToHex(ctx.card.cid)}"

        if (hasIdInfo == false) rlToolButtons?.visibility = View.VISIBLE

        // set listeners
        srl.setOnRefreshListener { refresh() }

        btnDetails.setOnClickListener {
            if (cardProtocol != null) {
                val bundle = Bundle().apply { ctx.saveToBundle(this) }
                navigateForResult(Constant.REQUEST_CODE_VERIFY_CARD,
                        R.id.action_idFragment_to_verifyCard, bundle)
            } else {
                (activity as MainActivity).toastHelper
                        .showSingleToast(context, getString(R.string.general_notification_scan_again_to_verify))
            }
        }

        btnIssueNewId?.setOnClickListener {
            val bundle = Bundle().apply { ctx.saveToBundle(this) }
            navigateToDestination(R.id.action_idFragment_to_issueNewIdFragment, bundle)
        }

        if (hasIdInfo == null) tvBalanceLine1?.text = "Reading... Hold the card firmly"

        btnNewScan.setOnClickListener {
            navigateUp()
        }

        // request card verify and get info listener
        val cardVerifyAndGetInfoListener: ServerApiTangem.CardVerifyAndGetInfoListener = object : ServerApiTangem.CardVerifyAndGetInfoListener {
            override fun onSuccess(cardVerifyAndGetArtworkResponse: CardVerifyAndGetInfo.Response?) {
                LOG.i(TAG, "cardVerifyAndGetInfoListener onSuccess")
                if (activity == null || !UtilHelper.isOnline(activity!!)) return

                val result = cardVerifyAndGetArtworkResponse?.results!![0]
                if (result.error != null) {
                    ctx.card!!.isOnlineVerified = false
                    return
                }
                ctx.card!!.isOnlineVerified = result.passed

                requestCounter--
                updateViews()

                if (!result.passed) return

                if (App.localStorage.checkBatchInfoChanged(ctx.card!!, result)) {
                    LOG.w(TAG, "Batch ${result.batch} info  changed to '$result'")
                    ivTangemCard.setImageBitmap(App.localStorage.getCardArtworkBitmap(ctx.card!!))
                    App.localStorage.applySubstitution(ctx.card!!)
                    refresh()
                }
            }

            override fun onFail(message: String?) {
                LOG.i(TAG, "cardVerifyAndGetInfoListener onFail")
                if (activity == null || !UtilHelper.isOnline(activity!!)) return
                requestCounter--
                updateViews()
            }
        }
        serverApiTangem.setCardVerifyAndGetInfoListener(cardVerifyAndGetInfoListener)
        refresh()
        startVerify(lastTag)
    }

    override fun onPause() {
        super.onPause()
        if (timerHideErrorAndMessage != null) {
            timerHideErrorAndMessage!!.cancel()
            timerHideErrorAndMessage = null
        }
    }

    override fun onStop() {
        srl?.removeCallbacks(refreshAction)
        toast?.cancel()
        super.onStop()
    }


    override fun onTagDiscovered(tag: Tag) {
        startVerify(tag)
    }

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar?.post { rlProgressBar?.visibility = View.VISIBLE }
    }

    override fun onReadProgress(protocol: CardProtocol, progress: Int) {

    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        verifyCardTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar?.post {
                    rlProgressBar?.visibility = View.GONE
                    this.cardProtocol = cardProtocol
                    if (!cardProtocol.card.isWalletPublicKeyValid)
                        refresh()
                    else
                        updateViews()

                    if (cardProtocol.card.isIDCard) {
                        if (cardProtocol.card.hasIDCardData()) {

                            ctx.card = cardProtocol.card

                            tvBalanceLine1?.visibility = View.VISIBLE

                            val idCardData = ctx.card.idCardData
                            tvFullName.text = idCardData.fullName
                            tvBirthDate.text = idCardData.birthday
                            tvGender.text = "Sex: ${idCardData.gender}"
                            ivPhoto.setImageBitmap(BitmapFactory.decodeByteArray(idCardData.photo, 0, idCardData.photo.size))

                            val inputs = "${idCardData.fullName};${idCardData.birthday}${idCardData.gender}"
                            val info = inputs.toByteArray() + idCardData.photo
                            ctx.card.idHash = Util.calculateSHA256(info)

                            refresh()

                        } else {
                            rlToolButtons?.visibility = View.VISIBLE
                            tvBalanceLine1?.text = resources.getString(R.string.id_empty).toUpperCase(Locale.ENGLISH)
                            tvBalanceLine1?.setTextSize(18F)
                            tvBalanceLine1?.visibility = View.VISIBLE
                            hasIdInfo = false
                        }
                    }

                    mpSecondScanSound.start()
                }
            } else {
                // remove last UIDs because of error and no card read
                FirebaseCrashlytics.getInstance().recordException(cardProtocol.error)
                rlProgressBar?.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported)
                        if (!NoExtendedLengthSupportDialog.allReadyShowed)
                            activity?.supportFragmentManager?.let { NoExtendedLengthSupportDialog().show(it, NoExtendedLengthSupportDialog.TAG) }
                        else {
                            toast = Toast.makeText(activity, R.string.general_notification_scan_again, Toast.LENGTH_SHORT)
                            toast?.show()
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
    }

    override fun onReadCancel() {
        verifyCardTask = null
        rlProgressBar?.postDelayed({
            try {
                rlProgressBar?.visibility = View.GONE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 500)
    }

    override fun onReadWait(msec: Int) {
        WaitSecurityDelayDialog.onReadWait(activity as AppCompatActivity?, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(activity as AppCompatActivity?, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(activity)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

    }

    fun updateViews() {
        if (activity == null || this.view == null) return

        if (timerHideErrorAndMessage != null) {
            timerHideErrorAndMessage!!.cancel()
            timerHideErrorAndMessage = null
        }

        if (ctx.card?.idCardData?.photo != null && (photo == null)) {
            photo = ctx.card.idCardData.photo
            ivPhoto.setImageBitmap(BitmapFactory.decodeByteArray(photo, 0, photo!!.size))

        }

        if (ctx.hasError()) {
            tvError?.visibility = View.VISIBLE
            tvError?.text = ctx.error
        } else {
            tvError?.visibility = View.GONE
            tvError?.text = ""
        }

        if (ctx.message == null || ctx.message.isEmpty()) {
            tvMessage?.text = ""
            tvMessage?.visibility = View.GONE
        } else {
            tvMessage?.text = ctx.message
            tvMessage?.visibility = View.VISIBLE
        }

        if (tvError?.visibility == View.VISIBLE || tvMessage?.visibility == View.VISIBLE) {
            timerHideErrorAndMessage = Timer()
            timerHideErrorAndMessage?.schedule(
                    timerTask {
                        activity?.runOnUiThread {
                            tvMessage?.visibility = View.GONE
                            tvError?.visibility = View.GONE
                            // clear only already viewed messages
                            if (tvMessage?.text == ctx.message) ctx.message = null
                            if (tvError?.text == ctx.error) ctx.error = null
                        }
                    },
                    5000)
        }

        if (srl.isRefreshing && ctx.card?.hasIDCardData() != false) {
            tvBalanceLine1.setTextColor(resources.getColor(R.color.primary))
            tvBalanceLine1.text = getString(R.string.loaded_wallet_verifying_in_blockchain)
            tvBalanceLine2.text = ""
        } else if (ctx.card?.hasIDCardData() != false) {

            val validator = BalanceValidator()
            // TODO why attest=false?
            validator.check(ctx, false)
            context?.let { ContextCompat.getColor(it, validator.color) }?.let { tvBalanceLine1?.setTextColor(it) }
            tvBalanceLine1?.text = getString(validator.firstLine)
        }

        if (ctx.card.hasIDCardData()) {
            val idCardData = ctx.card.idCardData
            tvFullName.text = idCardData.fullName
            tvBirthDate.text = idCardData.birthday
            tvGender.text = "Sex: ${idCardData.gender}"
            ivPhoto.setImageBitmap(BitmapFactory.decodeByteArray(idCardData.photo, 0, idCardData.photo.size))
            rlToolButtons?.visibility = View.GONE
        }
    }

    private fun refresh() {
        if (ctx.card == null) return

        // clear all card data and request again
        ctx.coinData.clearInfo()
        ctx.error = null
        ctx.message = null

        LOG.w(TAG, "============= START REFRESH")
        requestCounter = 0
        srl?.isRefreshing = true

        updateViews()

        // Bitcoin, Litecoin, BitcoinCash, Stellar
        if (ctx.blockchain == Blockchain.Bitcoin || ctx.blockchain == Blockchain.BitcoinTestNet || ctx.blockchain == Blockchain.BitcoinDual ||
                ctx.blockchain == Blockchain.Litecoin || ctx.blockchain == Blockchain.BitcoinCash ||
                ctx.blockchain == Blockchain.Stellar || ctx.blockchain == Blockchain.StellarTestNet || ctx.blockchain == Blockchain.StellarAsset) {
            ctx.coinData.setIsBalanceEqual(true)
        }

        requestVerifyAndGetInfo()

        if (ctx.card.hasIDCardData()) requestBalanceAndUnspentTransactions()

        if (requestCounter == 0) {
            // if no connection and no requests posted
            srl?.isRefreshing = false
            updateViews()
        }
    }

    private fun requestBalanceAndUnspentTransactions() {
        if (UtilHelper.isOnline(context as Activity)) {
            val coinEngine = CoinEngineFactory.create(ctx)
            coinEngine?.defineWallet()
            requestCounter++
            coinEngine!!.requestBalanceAndUnspentTransactions(
                    object : CoinEngine.BlockchainRequestsCallbacks {


                        override fun onComplete(success: Boolean) {
                            LOG.i(TAG, "requestBalanceAndUnspentTransactions onComplete: $success, request counter $requestCounter")
                            if (activity == null) return
                            requestCounter--
                            if (!success) {
                                LOG.e(TAG, "requestBalanceAndUnspentTransactions ctx.error: " + ctx.error)
                            }
                            updateViews()

                        }

                        override fun onProgress() {
                            if (activity == null) return
                            LOG.i(TAG, "requestBalanceAndUnspentTransactions onProgress")
                            updateViews()
                        }

                        override fun allowAdvance(): Boolean {
                            return try {
                                context?.let { UtilHelper.isOnline(it) }!!
                            } catch (e: KotlinNullPointerException) {
                                e.printStackTrace()
                                false
                            }
                        }
                    }
            )
        } else {
            ctx.error = getString(R.string.general_error_no_connection)
            updateViews()
        }
    }

    private fun requestVerifyAndGetInfo() {
        if (UtilHelper.isOnline(context as Activity)) {
            if ((ctx.card!!.isOnlineVerified == null || !ctx.card!!.isOnlineVerified)) {
                LOG.i(TAG, "requestVerifyAndGetInfo")
                requestCounter++
                serverApiTangem.cardVerifyAndGetInfo(ctx.card)
            }
        } else {
            ctx.error = getString(R.string.general_error_no_connection)
            updateViews()
        }
    }

    private fun startVerify(tag: Tag?) {
        try {
            val isoDep = IsoDep.get(tag)
            val uid = tag?.id
            val sUID = Util.byteArrayToHexString(uid)
            if (ctx.card.uid != sUID || cardProtocol != null) {
                (activity as MainActivity).nfcManager.ignoreTag(isoDep.tag)
                return
            }

            if (lastReadSuccess)
                isoDep.timeout = 1000
            else
                isoDep.timeout = 65000

            verifyCardTask = VerifyCardTask(ctx.card, NfcReader((activity as MainActivity).nfcManager, isoDep),
                    App.localStorage, App.pinStorage, App.firmwaresStorage, this)
            verifyCardTask?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}