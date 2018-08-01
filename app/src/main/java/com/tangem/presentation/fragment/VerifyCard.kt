package com.tangem.presentation.fragment

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.PINStorage
import com.tangem.domain.wallet.TangemCard
import com.tangem.presentation.activity.CreateNewWalletActivity
import com.tangem.presentation.activity.PurgeActivity
import com.tangem.presentation.activity.RequestPINActivity
import com.tangem.presentation.activity.SwapPINActivity
import com.tangem.presentation.dialog.PINSwapWarningDialog
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fr_verify_card.*
import java.io.IOException
import java.util.*

class VerifyCard : Fragment(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = VerifyCard::class.java.simpleName

        private const val REQUEST_CODE_SEND_PAYMENT = 1
        private const val REQUEST_CODE_PURGE = 2
        private const val REQUEST_CODE_REQUEST_PIN2_FOR_PURGE = 3
        private const val REQUEST_CODE_VERIFY_CARD = 4
        private const val REQUEST_CODE_ENTER_NEW_PIN = 5
        private const val REQUEST_CODE_ENTER_NEW_PIN2 = 6
        private const val REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN = 7
        private const val REQUEST_CODE_SWAP_PIN = 8
    }

    private var nfcManager: NfcManager? = null
    private var mCard: TangemCard? = null

    private var requestPIN2Count = 0
    private var timerHideErrorAndMessage: Timer? = null
    private var newPIN = ""
    private var newPIN2 = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcManager = NfcManager(activity, this)

        mCard = TangemCard(activity!!.intent.getStringExtra("UID"))
        mCard!!.LoadFromBundle(activity!!.intent.extras!!.getBundle("Card"))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fr_verify_card, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateViews()

        swipe_container.setOnRefreshListener { swipe_container.isRefreshing = false }

        fabMenu.setOnClickListener { showMenu(fabMenu) }

        btnOk.setOnClickListener {
            val data = prepareResultIntent()
            data.putExtra("modification", "update")
            if (activity != null)
                activity!!.finish()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcManager!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        nfcManager!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        nfcManager!!.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var data = data
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ENTER_NEW_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras!!.containsKey("confirmPIN")) {
                        val intent = Intent(context, RequestPINActivity::class.java)
                        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                        intent.putExtra("UID", mCard!!.uid)
                        intent.putExtra("Card", mCard!!.asBundle)
                        newPIN = data.getStringExtra("newPIN")
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(context, RequestPINActivity::class.java)
                        intent.putExtra("newPIN", data.getStringExtra("newPIN"))
                        intent.putExtra("mode", RequestPINActivity.Mode.ConfirmNewPIN.toString())
                        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN)
                    }
                }
            }
            REQUEST_CODE_ENTER_NEW_PIN2 -> if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    if (data.extras!!.containsKey("confirmPIN2")) {
                        val intent = Intent(context, RequestPINActivity::class.java)
                        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                        intent.putExtra("UID", mCard!!.uid)
                        intent.putExtra("Card", mCard!!.asBundle)
                        newPIN2 = data.getStringExtra("newPIN2")
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    } else {
                        val intent = Intent(context, RequestPINActivity::class.java)
                        intent.putExtra("newPIN2", data.getStringExtra("newPIN2"))
                        intent.putExtra("mode", RequestPINActivity.Mode.ConfirmNewPIN2.toString())
                        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN2)
                    }
                }
            }
            REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (newPIN == "") newPIN = mCard!!.pin

                if (newPIN2 == "") newPIN2 = PINStorage.getPIN2()

                val pinSwapWarningDialog = PINSwapWarningDialog()
                pinSwapWarningDialog.setOnRefreshPage { startSwapPINActivity() }
                val bundle = Bundle()
                if (!PINStorage.isDefaultPIN(newPIN) || !PINStorage.isDefaultPIN2(newPIN2))
                    bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_forget))
                else
                    bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_use_default))
                pinSwapWarningDialog.arguments = bundle
                pinSwapWarningDialog.show(activity!!.fragmentManager, PINSwapWarningDialog.TAG)
            }

            REQUEST_CODE_SWAP_PIN -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    data = Intent()

                    data.putExtra("UID", mCard!!.uid)
                    data.putExtra("Card", mCard!!.asBundle)
                    data.putExtra("modification", "delete")
                } else {
                    data.putExtra("modification", "update")
                }
                activity!!.setResult(Activity.RESULT_OK, data)
                activity!!.finish()
            } else {
                if (data != null && data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                    val updatedCard = TangemCard(data.getStringExtra("UID"))
                    updatedCard.LoadFromBundle(data.getBundleExtra("Card"))
                    mCard = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(context, RequestPINActivity::class.java)
                    intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                    intent.putExtra("UID", mCard!!.uid)
                    intent.putExtra("Card", mCard!!.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey("message")) {
                        mCard!!.error = data.getStringExtra("message")
                    }
                }
            }
            REQUEST_CODE_REQUEST_PIN2_FOR_PURGE -> if (resultCode == Activity.RESULT_OK) {
                val intent = Intent(context, PurgeActivity::class.java)
                intent.putExtra("UID", mCard!!.uid)
                intent.putExtra("Card", mCard!!.asBundle)
                startActivityForResult(intent, REQUEST_CODE_PURGE)
            }
            REQUEST_CODE_PURGE -> if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    data = Intent()

                    data.putExtra("UID", mCard!!.uid)
                    data.putExtra("Card", mCard!!.asBundle)
                    data.putExtra("modification", "delete")
                } else {
                    data.putExtra("modification", "update")
                }
                activity!!.setResult(Activity.RESULT_OK, data)
                activity!!.finish()
            } else {
                if (data != null && data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                    val updatedCard = TangemCard(data.getStringExtra("UID"))
                    updatedCard.LoadFromBundle(data.getBundleExtra("Card"))
                    mCard = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(context, RequestPINActivity::class.java)
                    intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                    intent.putExtra("UID", mCard!!.uid)
                    intent.putExtra("Card", mCard!!.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_PURGE)
                    return
                } else {
                    if (data != null && data.extras!!.containsKey("message")) {
                        mCard!!.error = data.getStringExtra("message")
                    }
                }
                updateViews()
            }
            REQUEST_CODE_SEND_PAYMENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    swipe_container.isRefreshing = true
                    mCard!!.clearInfo()
                    updateViews()
                }

                if (data != null) {
                    if (data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                        val updatedCard = TangemCard(data.getStringExtra("UID"))
                        updatedCard.LoadFromBundle(data.getBundleExtra("Card"))
                        mCard = updatedCard
                    }
                    if (data.extras!!.containsKey("message")) {
                        if (resultCode == Activity.RESULT_OK) {
                            mCard!!.message = data.getStringExtra("message")
                        } else {
                            mCard!!.error = data.getStringExtra("message")
                        }
                    }
                    updateViews()
                }
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
//            Log.w(getClass().getName(), "Ignore discovered tag!");
            nfcManager!!.ignoreTag(tag)
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
            tvCardID.text = mCard!!.cidDescription

            if (mCard!!.error == null || mCard!!.error.isEmpty()) {
                tvError.visibility = View.GONE
                tvError.text = ""
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = mCard!!.error
            }
            if (mCard!!.message == null || mCard!!.message.isEmpty()) {
                tvMessage.visibility = View.GONE
                tvMessage.text = ""
            } else {
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = mCard!!.message
            }

            tvManufacturerInfo.text = mCard!!.manufacturer.officialName

            if (mCard!!.isManufacturerConfirmed && mCard!!.isCardPublicKeyValid) {
                tvCardIdentity.setText(R.string.attested)
                tvCardIdentity.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))

            } else {
                tvCardIdentity.setText(R.string.not_confirmed)
                tvCardIdentity.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
            }

            tvIssuer.text = mCard!!.issuerDescription
            tvIssuerData.text = mCard!!.issuerDataDescription

            tvCardRegistredDate.text = mCard!!.personalizationDateTimeDescription

            tvBlockchain.text = mCard!!.blockchainName

            ivBlockchain.setImageResource(mCard!!.blockchain.getLogoImageResource(mCard!!.blockchainID, mCard!!.tokenSymbol))

            if (mCard!!.isReusable!!)
                tvReusable.setText(R.string.reusable)
            else
                tvReusable.setText(R.string.one_off_banknote)

            tvSigningMethod.text = mCard!!.signingMethod.description

            if (mCard!!.status == TangemCard.Status.Loaded || mCard!!.status == TangemCard.Status.Purged) {

                tvLastSigned.text = mCard!!.lastSignedDescription
                when {
                    mCard!!.remainingSignatures == 0 -> {
                        tvRemainingSignatures.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
                        tvRemainingSignatures.setText(R.string.none)
                    }
                    mCard!!.remainingSignatures == 1 -> {
                        tvRemainingSignatures.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
                        tvRemainingSignatures.setText(R.string.last_one)
                    }
                    mCard!!.remainingSignatures > 1000 -> {
                        tvRemainingSignatures.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))
                        tvRemainingSignatures.setText(R.string.unlimited)
                    }
                    else -> {
                        tvRemainingSignatures.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))
                        tvRemainingSignatures.text = mCard!!.remainingSignatures.toString()
                    }
                }
                tvSignedTx.text = (mCard!!.maxSignatures - mCard!!.remainingSignatures).toString()
            } else {
                tvLastSigned.text = ""
                tvRemainingSignatures.text = ""
                tvSignedTx.text = ""
            }

            tvFirmware.text = mCard!!.firmwareVersion

            var features = ""

            features += if (mCard!!.allowSwapPIN()!! && mCard!!.allowSwapPIN2()!!) {
                "Allows change PIN1 and PIN2\n"
            } else if (mCard!!.allowSwapPIN()!!) {
                "Allows change PIN1\n"
            } else if (mCard!!.allowSwapPIN2()!!) {
                "Allows change PIN2\n"
            } else {
                "Fixed PIN1 and PIN2\n"
            }

            if (mCard!!.needCVC()!!)
                features += "Requires CVC\n"


            if (mCard!!.supportDynamicNDEF()!!) {
                features += "Dynamic NDEF for iOS\n"
            } else if (mCard!!.supportNDEF()!!)
                features += "NDEF\n"

            if (mCard!!.supportBlock()!!)
                features += "Blockable\n"


            if (mCard!!.supportOnlyOneCommandAtTime()!!)
                features += "Atomic command mode"

            if (features.endsWith("\n"))
                features = features.substring(0, features.length - 1)

            tvFeatures.text = features

            if (mCard!!.useDefaultPIN1()!!) {
                imgPIN.setImageResource(R.drawable.unlock_pin1)
                imgPIN.setOnClickListener { Toast.makeText(context, R.string.this_banknote_protected_default_PIN1_code, Toast.LENGTH_LONG).show() }
            } else {
                imgPIN.setImageResource(R.drawable.lock_pin1)
                imgPIN.setOnClickListener { Toast.makeText(context, R.string.this_banknote_protected_user_PIN1_code, Toast.LENGTH_LONG).show() }
            }

            if (mCard!!.pauseBeforePIN2 > 0 && (mCard!!.useDefaultPIN2()!! || !mCard!!.useSmartSecurityDelay())) {
                imgPIN2orSecurityDelay.setImageResource(R.drawable.timer)
                imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, String.format(getString(R.string.this_banknote_will_enforce), mCard!!.pauseBeforePIN2 / 1000.0), Toast.LENGTH_LONG).show() }
            } else if (mCard!!.useDefaultPIN2()!!) {
                imgPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2)
                imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, R.string.this_banknote_protected_default_PIN2_code, Toast.LENGTH_LONG).show() }
            } else {
                imgPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2)
                imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(context, R.string.this_banknote_protected_user_PIN2_code, Toast.LENGTH_LONG).show() }
            }

            if (mCard!!.useDevelopersFirmware()!!) {
                imgDeveloperVersion.setImageResource(R.drawable.ic_developer_version)
                imgDeveloperVersion.visibility = View.VISIBLE
                imgDeveloperVersion.setOnClickListener { v -> Toast.makeText(context, R.string.unlocked_banknote_only_development_use, Toast.LENGTH_LONG).show() }
            } else
                imgDeveloperVersion.visibility = View.INVISIBLE

            if (mCard!!.status == TangemCard.Status.Loaded) {
                tvWallet.text = mCard!!.shortWalletString
                if (mCard!!.isWalletPublicKeyValid) {
                    tvWalletIdentity.setText(R.string.possession_proved)
                    tvWalletIdentity.setTextColor(ContextCompat.getColor(context!!, R.color.confirmed))
                } else {
                    tvWalletIdentity.setText(R.string.possession_not_proved)
                    tvWalletIdentity.setTextColor(ContextCompat.getColor(context!!, R.color.not_confirmed))
                }
            } else {
                tvWallet!!.setText(R.string.not_available)
                tvWalletIdentity.setText(R.string.no_data_string)
            }

//            timerHideErrorAndMessage = Timer()
//            timerHideErrorAndMessage!!.schedule(object : TimerTask() {
//                override fun run() {
//                    tvError.post {
//                        tvMessage!!.visibility = View.GONE
//                        tvError!!.visibility = View.GONE
//                        mCard!!.error = null
//                        mCard!!.message = null
//                    }
//                }
//            }, 5000)


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun doSetPin() {
        requestPIN2Count = 0
        val intent = Intent(context, RequestPINActivity::class.java)
        intent.putExtra("mode", RequestPINActivity.Mode.RequestNewPIN.toString())
        newPIN = ""
        newPIN2 = ""
        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN)
    }

    private fun doResetPin() {
        requestPIN2Count = 0
        val intent = Intent(context, RequestPINActivity::class.java)
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
        intent.putExtra("UID", mCard!!.uid)
        intent.putExtra("Card", mCard!!.asBundle)
        newPIN = PINStorage.getDefaultPIN()
        newPIN2 = ""
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    private fun doResetPin2() {
        requestPIN2Count = 0
        val intent = Intent(context, RequestPINActivity::class.java)
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
        intent.putExtra("UID", mCard!!.uid)
        intent.putExtra("Card", mCard!!.asBundle)
        newPIN = ""
        newPIN2 = PINStorage.getDefaultPIN2()
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    private fun doResetPins() {
        requestPIN2Count = 0
        val intent = Intent(context, RequestPINActivity::class.java)
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
        intent.putExtra("UID", mCard!!.uid)
        intent.putExtra("Card", mCard!!.asBundle)
        newPIN = PINStorage.getDefaultPIN()
        newPIN2 = PINStorage.getDefaultPIN2()
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN)
    }

    private fun doSetPin2() {
        requestPIN2Count = 0
        val intent = Intent(context, RequestPINActivity::class.java)
        intent.putExtra("mode", RequestPINActivity.Mode.RequestNewPIN2.toString())
        newPIN = ""
        newPIN2 = ""
        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN2)
    }

    private fun doPurge() {
        requestPIN2Count = 0
        val engine = CoinEngineFactory.Create(mCard!!.blockchain)
        if (!mCard!!.hasBalanceInfo()) {
            return
        } else if (engine != null && engine.IsBalanceNotZero(mCard)) {
            Toast.makeText(context, R.string.cannot_erase_wallet_with_non_zero_balance, Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(context, RequestPINActivity::class.java)
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
        intent.putExtra("UID", mCard!!.uid)
        intent.putExtra("Card", mCard!!.asBundle)
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_PURGE)
    }

    private fun startSwapPINActivity() {
        val intent = Intent(context, SwapPINActivity::class.java)
        intent.putExtra("UID", mCard!!.uid)
        intent.putExtra("Card", mCard!!.asBundle)
        intent.putExtra("newPIN", newPIN)
        intent.putExtra("newPIN2", newPIN2)
        startActivityForResult(intent, REQUEST_CODE_SWAP_PIN)
    }

    fun prepareResultIntent(): Intent {
        val data = Intent()
        data.putExtra("UID", mCard!!.uid)
        data.putExtra("Card", mCard!!.asBundle)
        return data
    }

    private fun showMenu(v: View) {
        val popup = PopupMenu(activity, v)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.menu_loaded_wallet, popup.menu)

        popup.menu.findItem(R.id.action_set_PIN1).isVisible = mCard!!.allowSwapPIN()!!
        popup.menu.findItem(R.id.action_reset_PIN1).isVisible = mCard!!.allowSwapPIN()!! && !mCard!!.useDefaultPIN1()
        popup.menu.findItem(R.id.action_set_PIN2).isVisible = mCard!!.allowSwapPIN2()!!
        popup.menu.findItem(R.id.action_reset_PIN2).isVisible = mCard!!.allowSwapPIN2()!! && !mCard!!.useDefaultPIN2()
        popup.menu.findItem(R.id.action_reset_PINs).isVisible = mCard!!.allowSwapPIN()!! && mCard!!.allowSwapPIN2()!! && !mCard!!.useDefaultPIN1() && !mCard!!.useDefaultPIN2()
        if (!mCard!!.isReusable || mCard!!.status != TangemCard.Status.Loaded)
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