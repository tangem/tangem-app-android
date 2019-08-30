package com.tangem.ui.fragment

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tangem.App
import com.tangem.Constant
import com.tangem.card_android.android.data.PINStorage
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.card_android.data.EXTRA_TANGEM_CARD
import com.tangem.card_android.data.EXTRA_TANGEM_CARD_UID
import com.tangem.card_android.data.loadFromBundle
import com.tangem.card_common.data.TangemCard
import com.tangem.card_common.reader.CardProtocol
import com.tangem.data.Blockchain
import com.tangem.ui.activity.PinRequestActivity
import com.tangem.ui.activity.VerifyCardActivity
import com.tangem.ui.dialog.PINSwapWarningDialog
import com.tangem.ui.event.DeletingWalletFinish
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fr_verify_card.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.IOException
import java.util.*

class VerifyCard : androidx.fragment.app.Fragment(), NfcAdapter.ReaderCallback {
    companion object {
        val TAG: String = VerifyCard::class.java.simpleName
    }

    private lateinit var nfcManager: NfcManager
    private lateinit var ctx: TangemContext

    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(activity, activity?.intent?.extras)

        nfcManager = NfcManager(activity!!, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fr_verify_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateViews()

        srlVerifyCard.setOnRefreshListener { srlVerifyCard.isRefreshing = false }

        // set listeners
        fabMenu.setOnClickListener { showMenu(fabMenu) }

        btnOk.setOnClickListener {
            val data = prepareResultIntent()
            data.putExtra(Constant.EXTRA_MODIFICATION, getString(R.string.main_screen_btn_update))
            activity?.finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun onDeleteWalletFinish(deletingWalletFinish: DeletingWalletFinish) {
        activity?.finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Constant.REQUEST_CODE_ENTER_NEW_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras!!.containsKey(Constant.EXTRA_CONFIRM_PIN)) {
                        val intent = Intent(context, PinRequestActivity::class.java)
                        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
                        ctx.saveToIntent(intent)
                        newPIN = data.getStringExtra(Constant.EXTRA_NEW_PIN)
                        startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(context, PinRequestActivity::class.java)
                        intent.putExtra(Constant.EXTRA_NEW_PIN, data.getStringExtra(Constant.EXTRA_NEW_PIN))
                        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.ConfirmNewPIN.toString())
                        startActivityForResult(intent, Constant.REQUEST_CODE_ENTER_NEW_PIN)
                    }
                }
            }

            Constant.REQUEST_CODE_ENTER_NEW_PIN2 -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras!!.containsKey(Constant.EXTRA_CONFIRM_PIN_2)) {
                        val intent = Intent(context, PinRequestActivity::class.java)
                        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
                        ctx.saveToIntent(intent)
                        newPIN2 = data.getStringExtra(Constant.EXTRA_NEW_PIN_2)
                        startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(context, PinRequestActivity::class.java)
                        intent.putExtra(Constant.EXTRA_NEW_PIN_2, data.getStringExtra(Constant.EXTRA_NEW_PIN_2))
                        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.ConfirmNewPIN2.toString())
                        startActivityForResult(intent, Constant.REQUEST_CODE_ENTER_NEW_PIN2)
                    }
                }
            }

            Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (newPIN == "") newPIN = ctx.card!!.pin

                if (newPIN2 == "") newPIN2 = App.pinStorage.piN2

                val pinSwapWarningDialog = PINSwapWarningDialog()
                pinSwapWarningDialog.setOnRefreshPage { (activity as VerifyCardActivity).navigator.showPinSwap(context as Activity, newPIN, newPIN2) }
                val bundle = Bundle()
                if (!CardProtocol.isDefaultPIN(newPIN) || !CardProtocol.isDefaultPIN2(newPIN2))
                    bundle.putString(Constant.EXTRA_MESSAGE, getString(R.string.loaded_wallet_warning_dont_forget_pin))
                else
                    bundle.putString(Constant.EXTRA_MESSAGE, getString(R.string.loaded_wallet_warning_default_pin))
                pinSwapWarningDialog.arguments = bundle
                activity?.supportFragmentManager?.let { pinSwapWarningDialog.show(it, PINSwapWarningDialog.TAG) }
            }

            Constant.REQUEST_CODE_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    ctx.saveToIntent(data)
                    data?.putExtra(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_DELETE)
                } else
                    data.putExtra(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_UPDATE)
                activity?.setResult(Activity.RESULT_OK, data)
                activity?.finish()
            } else {
                if (data != null && data.extras!!.containsKey(EXTRA_TANGEM_CARD_UID) && data.extras!!.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getStringExtra(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundleExtra(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == Constant.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(context, PinRequestActivity::class.java)
                    intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
                    ctx.saveToIntent(intent)
                    startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey(Constant.EXTRA_MESSAGE)) {
                        ctx.error = data.getStringExtra(Constant.EXTRA_MESSAGE)
                    }
                }
            }

            Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE -> if (resultCode == Activity.RESULT_OK)
                (activity as VerifyCardActivity).navigator.showPurge(context as Activity, ctx)

            else {
                if (data != null && data.extras!!.containsKey(EXTRA_TANGEM_CARD_UID) && data.extras!!.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getStringExtra(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundleExtra(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
                if (resultCode == Constant.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(context, PinRequestActivity::class.java)
                    intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
                    ctx.saveToIntent(intent)
                    startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey(Constant.EXTRA_MESSAGE)) {
                        ctx.error = data.getStringExtra(Constant.EXTRA_MESSAGE)
                    }
                }
                updateViews()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        try {
            nfcManager.ignoreTag(tag!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun updateViews() {
        try {
            if (timerHideErrorAndMessage != null) {
                timerHideErrorAndMessage!!.cancel()
                timerHideErrorAndMessage = null
            }
            tvCardID.text = ctx.card!!.cidDescription

            if (ctx.error == null || ctx.error.isEmpty()) {
                tvError.visibility = View.GONE
                tvError.text = ""
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = ctx.error
            }
            if (ctx.message == null || ctx.message.isEmpty()) {
                tvMessage.visibility = View.GONE
                tvMessage.text = ""
            } else {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = ctx.message
            }

            tvManufacturerInfo.text = ctx.card!!.manufacturer.officialName

            if (ctx.card!!.isManufacturerConfirmed && ctx.card!!.isCardPublicKeyValid) {
                tvCardIdentity.setText(R.string.details_attested)
                tvCardIdentity.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))

            } else {
                tvCardIdentity.setText(R.string.details_not_confirmed)
                tvCardIdentity.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
            }

            tvIssuer.text = ctx.card!!.issuerDescription

            tvCardRegistredDate.text = DateUtils.formatDateTime(null, ctx.card!!.personalizationDateTime.time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NUMERIC_DATE or DateUtils.FORMAT_SHOW_YEAR)

            @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(ctx.blockchainName, Html.FROM_HTML_MODE_LEGACY)
            else
                Html.fromHtml(ctx.blockchainName)
            tvBlockchain.text = html

            tvValidationNode.text = ctx.coinData!!.validationNodeDescription

            val engine = CoinEngineFactory.create(ctx)

            tvInputs.text = engine!!.unspentInputsDescription

            ivBlockchain.setImageResource(Blockchain.getLogoImageResource(ctx.card!!.blockchainID, ctx.card!!.tokenSymbol))

            if (ctx.card!!.isReusable!!)
                tvReusable.setText(R.string.details_reusable)
            else
                tvReusable.setText(R.string.details_one_off_banknote)

            var s=""
            for(signingM in ctx.card!!.allowedSigningMethod) {
                s += if(signingM==ctx.card!!.signingMethod) {
                    ", <b>${signingM.description}</b>"
                } else {
                    ", <small>${signingM.description}</small>"
                }
            }
            s=s.substring(2,s.length)
            @Suppress("DEPRECATION")
            tvSigningMethod.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY)
            else
                Html.fromHtml(s)


            if (ctx.card!!.status == TangemCard.Status.Loaded || ctx.card!!.status == TangemCard.Status.Purged) {

                when {
                    ctx.card!!.remainingSignatures == 0 -> {
                        tvRemainingSignatures.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
                        tvRemainingSignatures.setText(R.string.details_none)
                    }
                    ctx.card!!.remainingSignatures == 1 -> {
                        tvRemainingSignatures.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
                        tvRemainingSignatures.setText(R.string.details_last_one)
                    }
                    ctx.card!!.remainingSignatures > 1000 -> {
                        tvRemainingSignatures.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))
                        tvRemainingSignatures.setText(R.string.details_unlimited)
                    }
                    else -> {
                        tvRemainingSignatures.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))
                        tvRemainingSignatures.text = ctx.card!!.remainingSignatures.toString()
                    }
                }
                tvSignedTx.text = (ctx.card!!.maxSignatures - ctx.card!!.remainingSignatures).toString()
            } else {
                tvLastSigned.text = ""
                tvRemainingSignatures.text = ""
                tvSignedTx.text = ""
            }

            tvFirmware.text = ctx.card!!.firmwareVersion

            var features = ""

            features += if (ctx.card!!.allowSwapPIN()!! && ctx.card!!.allowSwapPIN2()!!) {
                "Allows change PIN1 and PIN2\n"
            } else if (ctx.card!!.allowSwapPIN()!!) {
                "Allows change PIN1\n"
            } else if (ctx.card!!.allowSwapPIN2()!!) {
                "Allows change PIN2\n"
            } else {
                "Fixed PIN1 and PIN2\n"
            }

            if (ctx.card!!.needCVC()!!)
                features += "Requires CVC\n"


            if (ctx.card!!.supportDynamicNDEF()!!) {
                features += "Dynamic NDEF for iOS\n"
            } else if (ctx.card!!.supportNDEF()!!)
                features += "NDEF\n"

            if (ctx.card!!.supportBlock()!!)
                features += "Blockable\n"


            if (ctx.card!!.supportOnlyOneCommandAtTime()!!)
                features += "Atomic command mode"

            if (features.endsWith("\n"))
                features = features.substring(0, features.length - 1)

            tvFeatures.text = features

            if (ctx.card!!.useDefaultPIN1()) {
                imgPIN.setImageResource(R.drawable.unlock_pin1)
                imgPIN.setOnClickListener { Toast.makeText(context, R.string.details_protected_by_default_pin_1, Toast.LENGTH_LONG).show() }
            } else {
                imgPIN.setImageResource(R.drawable.lock_pin1)
                imgPIN.setOnClickListener { Toast.makeText(context, R.string.details_protected_by_user_pin_1, Toast.LENGTH_LONG).show() }
            }

            if (ctx.card!!.pauseBeforePIN2 > 0 && (ctx.card!!.useDefaultPIN2()!! || !ctx.card!!.useSmartSecurityDelay())) {
                imgPIN2orSecurityDelay.setImageResource(R.drawable.timer)
                imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, String.format(getString(R.string.details_security_delay), ctx.card!!.pauseBeforePIN2 / 1000.0), Toast.LENGTH_LONG).show() }
            } else if (ctx.card!!.useDefaultPIN2()!!) {
                imgPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2)
                imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, R.string.details_protected_by_default_pin_2, Toast.LENGTH_LONG).show() }
            } else {
                imgPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2)
                imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, R.string.details_protected_by_user_pin_2, Toast.LENGTH_LONG).show() }
            }

            if (ctx.card!!.useDevelopersFirmware()!!) {
                imgDeveloperVersion.setImageResource(R.drawable.ic_developer_version)
                imgDeveloperVersion.visibility = View.VISIBLE
                imgDeveloperVersion.setOnClickListener { Toast.makeText(context, R.string.details_unlocked_banknote, Toast.LENGTH_LONG).show() }
            } else
                imgDeveloperVersion.visibility = View.INVISIBLE

            if (ctx.card!!.status == TangemCard.Status.Loaded) {
                tvWallet.text = ctx.coinData!!.shortWalletString
                if (ctx.card!!.isWalletPublicKeyValid) {
                    tvWalletIdentity.setText(R.string.details_possession_proved)
                    tvWalletIdentity.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))
                } else {
                    tvWalletIdentity.setText(R.string.details_possession_not_proved)
                    tvWalletIdentity.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
                }
            } else {
                tvWallet!!.setText(R.string.details_not_available)
                tvWalletIdentity.setText(R.string.no_data_string)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doSetPin() {
        requestPIN2Count = 0
        val intent = Intent(context, PinRequestActivity::class.java)
        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestNewPIN.toString())
        newPIN = ""
        newPIN2 = ""
        startActivityForResult(intent, Constant.REQUEST_CODE_ENTER_NEW_PIN)
    }

    private fun doResetPin() {
        requestPIN2Count = 0
        val intent = Intent(context, PinRequestActivity::class.java)
        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
        ctx.saveToIntent(intent)
        newPIN = PINStorage.getDefaultPIN()
        newPIN2 = ""
        startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    private fun doResetPin2() {
        requestPIN2Count = 0
        val intent = Intent(context, PinRequestActivity::class.java)
        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
        ctx.saveToIntent(intent)
        newPIN = ""
        newPIN2 = PINStorage.getDefaultPIN2()
        startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    private fun doResetPins() {
        requestPIN2Count = 0
        val intent = Intent(context, PinRequestActivity::class.java)
        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
        ctx.saveToIntent(intent)
        newPIN = PINStorage.getDefaultPIN()
        newPIN2 = PINStorage.getDefaultPIN2()
        startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    private fun doSetPin2() {
        requestPIN2Count = 0
        val intent = Intent(context, PinRequestActivity::class.java)
        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestNewPIN2.toString())
        newPIN = ""
        newPIN2 = ""
        startActivityForResult(intent, Constant.REQUEST_CODE_ENTER_NEW_PIN2)
    }

    private fun doPurge() {
        requestPIN2Count = 0
        val engine = CoinEngineFactory.create(ctx)
        if (!engine!!.hasBalanceInfo()) {
            return
        } else if (engine.isBalanceNotZero) {
            Toast.makeText(context, R.string.general_error_cannot_erase_wallet_with_non_zero_balance, Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(context, PinRequestActivity::class.java)
        intent.putExtra(Constant.EXTRA_MODE, PinRequestActivity.Mode.RequestPIN2.toString())
        ctx.saveToIntent(intent)
        startActivityForResult(intent, Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE)
    }

    fun prepareResultIntent(): Intent {
        val data = Intent()
        ctx.saveToIntent(data)
        return data
    }

    private fun showMenu(v: View) {
        val popup = PopupMenu(activity, v)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_loaded_wallet, popup.menu)

        popup.menu.findItem(R.id.action_set_PIN1).isVisible = ctx.card!!.allowSwapPIN()!!
        popup.menu.findItem(R.id.action_reset_PIN1).isVisible = ctx.card!!.allowSwapPIN()!! && !ctx.card!!.useDefaultPIN1()
        popup.menu.findItem(R.id.action_set_PIN2).isVisible = ctx.card!!.allowSwapPIN2()!!
        popup.menu.findItem(R.id.action_reset_PIN2).isVisible = ctx.card!!.allowSwapPIN2()!! && !ctx.card!!.useDefaultPIN2()
        popup.menu.findItem(R.id.action_reset_PINs).isVisible = ctx.card!!.allowSwapPIN()!! && ctx.card!!.allowSwapPIN2()!! && !ctx.card!!.useDefaultPIN1() && !ctx.card!!.useDefaultPIN2()
        if (!ctx.card!!.isReusable || ctx.card!!.status != TangemCard.Status.Loaded)
            popup.menu.findItem(R.id.action_purge).isVisible = false

        popup.setOnMenuItemClickListener { item ->
            val id = item.itemId
            when (id) {
                R.id.action_set_PIN1 -> doSetPin()
                R.id.action_reset_PIN1 -> doResetPin()
                R.id.action_set_PIN2 -> doSetPin2()
                R.id.action_reset_PIN2 -> doResetPin2()
                R.id.action_reset_PINs -> doResetPins()
                R.id.action_purge -> doPurge()
            }
            false
        }

        if (BuildConfig.DEBUG) {
            popup.menu.findItem(R.id.action_set_PIN2).isEnabled = true
            popup.menu.findItem(R.id.action_reset_PIN2).isEnabled = true
        }

        popup.show()
    }

}