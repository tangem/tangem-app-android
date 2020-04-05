package com.tangem.ui.fragment.wallet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.App
import com.tangem.Constant
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_card.tasks.VerifyCardTask
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
import com.tangem.ui.fragment.pin.PinRequestFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fragment_empty_wallet.*
import kotlinx.android.synthetic.main.layout_tangem_card.*

class EmptyWalletFragment : BaseFragment(), NavigationResultListener,
        NfcAdapter.ReaderCallback, CardProtocol.Notifications {
    companion object {
        val TAG: String = EmptyWalletFragment::class.java.simpleName
        fun callingIntent(context: Context, ctx: TangemContext): Intent {
            val intent = Intent(context, EmptyWalletFragment::class.java)
            ctx.saveToIntent(intent)
            return intent
        }
    }

    override val layoutId = R.layout.fragment_empty_wallet

    private val ctx: TangemContext by lazy { TangemContext.loadFromBundle(context, arguments) }

    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0

    private var cardProtocol: CardProtocol? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvIssuer.text = ctx.card!!.issuerDescription

        if (ctx.card!!.tokenSymbol.length > 1) {
            @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(ctx.blockchainName, Html.FROM_HTML_MODE_LEGACY)
            else
                Html.fromHtml(ctx.blockchainName)
            tvBlockchain.text = html
        } else
            tvBlockchain.text = ctx.blockchainName

        tvCardID.text = ctx.card!!.cidDescription
        ivTangemCard.setImageBitmap(App.localStorage.getCardArtworkBitmap(ctx.card))

        // set listeners
        btnNewWallet.setOnClickListener {
            requestPIN2Count = 0

            val data = Bundle()
            data.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
            data.putString(EXTRA_TANGEM_CARD_UID, ctx.card!!.uid)
            data.putBundle(EXTRA_TANGEM_CARD, ctx.card!!.asBundle)
            navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2,
                    R.id.action_emptyWalletFragment_to_pinRequestFragment, data)
        }

        btnDetails.setOnClickListener {
            if (cardProtocol != null) {
                val data = Bundle()
                ctx.saveToBundle(data)
                navigateForResult(Constant.REQUEST_CODE_VERIFY_CARD,
                        R.id.action_emptyWalletFragment_to_verifyCard, data)
            } else {
                (activity as MainActivity).toastHelper
                        .showSingleToast(context, getString(R.string.general_notification_scan_again_to_verify))
            }
        }
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        if (requestCode == Constant.REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    data.putString(Constant.EXTRA_MODIFICATION, "updateAndViewCard")
                    data.putInt("updateDelay", 0)
                    navigateBackWithResult(Activity.RESULT_OK, data)
                }
                return
            } else {
                if (data != null && data.containsKey(EXTRA_TANGEM_CARD_UID) && data.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getString(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundle(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == Constant.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val bundle = Bundle()
                    bundle.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
                    bundle.putString(EXTRA_TANGEM_CARD_UID, ctx.card!!.uid)
                    bundle.putBundle(EXTRA_TANGEM_CARD, ctx.card!!.asBundle)
                    navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2,
                            R.id.action_emptyWalletFragment_to_pinRequestFragment, bundle)
                    return
                }
            }
            navigateBackWithResult(resultCode, data)
            return
        } else if (requestCode == Constant.REQUEST_CODE_REQUEST_PIN2) {
            if (resultCode == Activity.RESULT_OK) {
                val bundle = Bundle()
                bundle.putString(EXTRA_TANGEM_CARD_UID, ctx.card!!.uid)
                bundle.putBundle(EXTRA_TANGEM_CARD, ctx.card!!.asBundle)
                navigateForResult(Constant.REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY,
                        R.id.action_emptyWalletFragment_to_createNewWalletFragment, bundle)
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            val isoDep = IsoDep.get(tag)
            val uid = tag.id
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

    override fun onReadStart(cardProtocol: CardProtocol) {
        rlProgressBar?.post { rlProgressBar.visibility = View.VISIBLE }

        progressBar?.post {
            progressBar?.visibility = View.VISIBLE
            progressBar?.progress = 5
        }
    }

    override fun onReadFinish(cardProtocol: CardProtocol?) {
        verifyCardTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                rlProgressBar?.post { rlProgressBar.visibility = View.GONE }

                progressBar?.post {
                    progressBar?.progress = 100
                    progressBar?.progressTintList = ColorStateList.valueOf(Color.GREEN)
                    this.cardProtocol = cardProtocol
                }
            } else {
                FirebaseCrashlytics.getInstance().recordException(cardProtocol.error)
                // remove last UIDs because of error and no card read
                progressBar?.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                        if (!NoExtendedLengthSupportDialog.allReadyShowed)
                            NoExtendedLengthSupportDialog().show(activity!!.supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                    } else
                        Toast.makeText(context, R.string.general_notification_scan_again, Toast.LENGTH_LONG).show()

                    progressBar?.progress = 100
                    progressBar?.progressTintList = ColorStateList.valueOf(Color.RED)
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
        verifyCardTask = null

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