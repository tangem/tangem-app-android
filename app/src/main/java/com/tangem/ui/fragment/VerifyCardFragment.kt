package com.tangem.ui.fragment

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.format.DateUtils
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.tangem.App
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_card.reader.CardProtocol
import com.tangem.tangem_sdk.android.data.PINStorage
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangem_sdk.data.loadFromBundle
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.dialog.PINSwapWarningDialog
import com.tangem.ui.event.DeletingWalletFinish
import com.tangem.ui.fragment.pin.PinRequestFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fr_verify_card.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.IOException
import java.util.*

class VerifyCardFragment : BaseFragment(), NavigationResultListener, NfcAdapter.ReaderCallback {
    companion object {
        val TAG: String = VerifyCardFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fr_verify_card

    private lateinit var ctx: TangemContext

    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(context, arguments)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val data = prepareResultIntent()
                data.putExtra(Constant.EXTRA_MODIFICATION, getString(R.string.main_screen_btn_update))
                navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateViews()

        val card = ctx.card
        if (!card.allowSwapPIN() && !card.allowSwapPIN2() && card.forbidPurgeWallet()) {
            fabMenu.hide()
        }

        // set listeners
        fabMenu.setOnClickListener { showMenu(fabMenu) }

        btnOk.setOnClickListener {
            val data = prepareResultIntent()
            data.putExtra(Constant.EXTRA_MODIFICATION, getString(R.string.main_screen_btn_update))
            navigateUp()
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

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        when (requestCode) {
            Constant.REQUEST_CODE_ENTER_NEW_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.containsKey(Constant.EXTRA_CONFIRM_PIN)) {
                        val bundle = Bundle()
                        bundle.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
                        ctx.saveToBundle(bundle)
                        newPIN = data.getString(Constant.EXTRA_NEW_PIN)
                        navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                                R.id.action_verifyCard_to_pinRequestFragment, bundle)
                    } else {
                        val bundle = Bundle()
                        bundle.putString(Constant.EXTRA_NEW_PIN, data.getString(Constant.EXTRA_NEW_PIN))
                        bundle.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.ConfirmNewPIN.toString())
                        navigateForResult(Constant.REQUEST_CODE_ENTER_NEW_PIN,
                                R.id.action_verifyCard_to_pinRequestFragment, bundle)

                    }
                }
            }

            Constant.REQUEST_CODE_ENTER_NEW_PIN2 -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.containsKey(Constant.EXTRA_CONFIRM_PIN_2)) {
                        val bundle = Bundle()
                        bundle.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
                        ctx.saveToBundle(bundle)
                        newPIN2 = data.getString(Constant.EXTRA_NEW_PIN_2)
                        navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                                R.id.action_verifyCard_to_pinRequestFragment, bundle)

                    } else {
                        val bundle = Bundle()
                        bundle.putString(Constant.EXTRA_NEW_PIN_2, data.getString(Constant.EXTRA_NEW_PIN_2))
                        bundle.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.ConfirmNewPIN2.toString())
                        navigateForResult(Constant.REQUEST_CODE_ENTER_NEW_PIN2,
                                R.id.action_verifyCard_to_pinRequestFragment, bundle)
                    }
                }
            }

            Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (newPIN == "") newPIN = ctx.card!!.pin

                if (newPIN2 == "") newPIN2 = App.pinStorage.piN2

                val pinSwapWarningDialog = PINSwapWarningDialog()
                pinSwapWarningDialog.setOnRefreshPage {
                    val bundle = Bundle()
                    bundle.putString(Constant.EXTRA_NEW_PIN, newPIN)
                    bundle.putString(Constant.EXTRA_NEW_PIN_2, newPIN2)
                    navigateForResult(Constant.REQUEST_CODE_SWAP_PIN,
                            R.id.action_verifyCard_to_pinSwapFragment, bundle)
                }
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
                    val newData = Bundle()
                    ctx.saveToBundle(newData)
                    newData.putString(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_DELETE)
                    navigateBackWithResult(Activity.RESULT_OK, newData)
                } else {
                    data.putString(Constant.EXTRA_MODIFICATION, Constant.EXTRA_MODIFICATION_UPDATE)
                    navigateBackWithResult(Activity.RESULT_OK, data)
                }
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
                    ctx.saveToBundle(bundle)
                    navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                            R.id.action_verifyCard_to_pinRequestFragment, bundle)
                    return
                } else {
                    if (data != null && data.containsKey(Constant.EXTRA_MESSAGE)) {
                        ctx.error = data.getString(Constant.EXTRA_MESSAGE)
                    }
                }
            }

            Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE -> if (resultCode == Activity.RESULT_OK) {
                val bundle = Bundle().apply { ctx.saveToBundle(this) }
                navigateForResult(Constant.REQUEST_CODE_PURGE,
                        R.id.action_verifyCard_to_purgeFragment, bundle)
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
                    ctx.saveToBundle(bundle)
                    navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE,
                            R.id.action_verifyCard_to_pinRequestFragment, data)
                    return
                } else {
                    if (data != null && data.containsKey(Constant.EXTRA_MESSAGE)) {
                        ctx.error = data.getString(Constant.EXTRA_MESSAGE)
                    }
                }
                updateViews()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        try {
            (activity as MainActivity).nfcManager.ignoreTag(tag!!)
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

            ivBlockchain.setImageResource(ctx.blockchain.getLogoImageResource(ctx.card!!.tokenSymbol))

            var s = ""
            for (signingM in ctx.card!!.allowedSigningMethod) {
                s += if (signingM == ctx.card!!.signingMethod) {
                    ", <b>${signingM.description}</b>"
                } else {
                    ", <small>${signingM.description}</small>"
                }
            }
            s = s.substring(2, s.length)
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

            val textIsLinked = if (ctx.card.terminalIsLinked) {
                getString(R.string.details_linked_card_to_phone)
            } else {
                getString(R.string.general_no)
            }
            tvIsLinked.text = textIsLinked

            if (ctx.card!!.useDevelopersFirmware()!!) {
                imgDeveloperVersion.setImageResource(R.drawable.ic_developer_version)
                imgDeveloperVersion.visibility = View.VISIBLE
                imgDeveloperVersion.setOnClickListener { Toast.makeText(context, R.string.details_unlocked_card, Toast.LENGTH_LONG).show() }
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
        val data = Bundle()
        data.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestNewPIN.toString())
        newPIN = ""
        newPIN2 = ""
        navigateForResult(Constant.REQUEST_CODE_ENTER_NEW_PIN,
                R.id.action_verifyCard_to_pinRequestFragment, data)
    }

    private fun doResetPin() {
        requestPIN2Count = 0
        val data = Bundle()
        data.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
        ctx.saveToBundle(data)
        newPIN = PINStorage.getDefaultPIN()
        newPIN2 = ""
        navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                R.id.action_verifyCard_to_pinRequestFragment, data)
    }

    private fun doResetPin2() {
        requestPIN2Count = 0
        val data = Bundle()
        data.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
        ctx.saveToBundle(data)
        newPIN = ""
        newPIN2 = PINStorage.getDefaultPIN2()
        navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                R.id.action_verifyCard_to_pinRequestFragment, data)
    }

    private fun doResetPins() {
        requestPIN2Count = 0
        val data = Bundle()
        data.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
        ctx.saveToBundle(data)
        newPIN = PINStorage.getDefaultPIN()
        newPIN2 = PINStorage.getDefaultPIN2()
        navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN,
                R.id.action_verifyCard_to_pinRequestFragment, data)
    }

    private fun doSetPin2() {
        requestPIN2Count = 0
        val data = Bundle()
        data.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestNewPIN2.toString())
        newPIN = ""
        newPIN2 = ""
        navigateForResult(Constant.REQUEST_CODE_ENTER_NEW_PIN2,
                R.id.action_verifyCard_to_pinRequestFragment, data)
    }

    private fun doPurge() {
        requestPIN2Count = 0
        val engine = CoinEngineFactory.create(ctx) ?: return
        if (!engine.hasBalanceInfo()) {
            return
        }
        if (engine.isBalanceNotZero) {
            Toast.makeText(context, R.string.general_error_cannot_erase_wallet_with_non_zero_balance, Toast.LENGTH_LONG).show()
            return
        } else if (engine.awaitingConfirmation()) {
            Toast.makeText(context, R.string.general_error_cannot_erase_wallet_with_non_zero_balance, Toast.LENGTH_LONG).show()
            return
        }

        val data = Bundle()
        data.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
        ctx.saveToBundle(data)
        navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_FOR_PURGE,
                R.id.action_verifyCard_to_pinRequestFragment, data)
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