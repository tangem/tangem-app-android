package com.tangem.ui.activity

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
import androidx.appcompat.app.AppCompatActivity
import android.text.Html
import android.view.View
import android.widget.Toast
import com.tangem.App
import com.tangem.Constant
import com.tangem.di.Navigator
import com.tangem.wallet.TangemContext
import com.tangem.ui.dialog.NoExtendedLengthSupportDialog
import com.tangem.ui.dialog.WaitSecurityDelayDialog
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.card_android.android.reader.NfcReader
import com.tangem.card_android.data.asBundle
import com.tangem.card_android.data.loadFromBundle
import com.tangem.card_common.data.TangemCard
import com.tangem.card_common.reader.CardProtocol
import com.tangem.card_common.tasks.VerifyCardTask
import com.tangem.card_common.util.Util
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_empty_wallet.*
import kotlinx.android.synthetic.main.layout_tangem_card.*
import javax.inject.Inject
import com.tangem.card_android.data.EXTRA_TANGEM_CARD
import com.tangem.card_android.data.EXTRA_TANGEM_CARD_UID
import com.tangem.di.ToastHelper

class EmptyWalletActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {
    companion object {
        val TAG: String = EmptyWalletActivity::class.java.simpleName
        fun callingIntent(context: Context, ctx: TangemContext): Intent {
            val intent = Intent(context, EmptyWalletActivity::class.java)
            ctx.saveToIntent(intent)
            return intent
        }
    }

    @Inject
    internal lateinit var navigator: Navigator
    @Inject
    internal lateinit var toastHelper: ToastHelper

    private lateinit var nfcManager: NfcManager
    private lateinit var ctx: TangemContext

    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0

    private var cardProtocol: CardProtocol? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty_wallet)

        App.navigatorComponent.inject(this)
        App.toastHelperComponent.inject(this)

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        ctx = TangemContext.loadFromBundle(this, intent.extras)

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
            val intent = Intent(baseContext, PinRequestActivity::class.java)
            intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
            intent.putExtra(EXTRA_TANGEM_CARD_UID, ctx.card!!.uid)
            intent.putExtra(EXTRA_TANGEM_CARD, ctx.card!!.asBundle)
            startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2)
        }

        btnDetails.setOnClickListener {
            if (cardProtocol != null)
                navigator.showVerifyCard(this, ctx)
            else
                toastHelper.showSingleToast(this, getString(R.string.need_attach_card_again))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constant.REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    data.putExtra(Constant.EXTRA_MODIFICATION, "updateAndViewCard")
                    data.putExtra("updateDelay", 0)
                    setResult(Activity.RESULT_OK, data)
                }
                finish()
            } else {
                if (data != null && data.extras!!.containsKey(EXTRA_TANGEM_CARD_UID) && data.extras!!.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getStringExtra(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundleExtra(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == Constant.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(baseContext, PinRequestActivity::class.java)
                    intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
                    intent.putExtra(EXTRA_TANGEM_CARD_UID, ctx.card!!.uid)
                    intent.putExtra(EXTRA_TANGEM_CARD, ctx.card!!.asBundle)
                    startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2)
                    return
                }
            }
            setResult(resultCode, data)
            finish()
        } else if (requestCode == Constant.REQUEST_CODE_REQUEST_PIN2) {
            if (resultCode == Activity.RESULT_OK) {
                navigator.showCreateNewWallet(this, ctx)
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            val isoDep = IsoDep.get(tag)
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
            if (ctx.card.uid != sUID) {
                nfcManager.ignoreTag(isoDep.tag)
                return
            }

            if (lastReadSuccess)
                isoDep.timeout = 1000
            else
                isoDep.timeout = 65000

            verifyCardTask = VerifyCardTask(ctx.card, NfcReader(nfcManager, isoDep), App.localStorage, App.pinStorage, App.firmwaresStorage, this)
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
                // remove last UIDs because of error and no card read
                progressBar?.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                        if (!NoExtendedLengthSupportDialog.allReadyShowed)
                            NoExtendedLengthSupportDialog().show(supportFragmentManager, NoExtendedLengthSupportDialog.TAG)
                    } else
                        Toast.makeText(this@EmptyWalletActivity, R.string.try_to_scan_again, Toast.LENGTH_LONG).show()

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
        progressBar?.post { progressBar!!.progress = progress }
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
        WaitSecurityDelayDialog.onReadWait(this, msec)
    }

    override fun onReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)
    }

    override fun onReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this)
    }

}