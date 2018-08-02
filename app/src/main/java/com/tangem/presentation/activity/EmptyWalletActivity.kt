package com.tangem.presentation.activity

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.tangem.data.nfc.VerifyCardTask
import com.tangem.domain.cardReader.CardProtocol
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.TangemCard
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog
import com.tangem.presentation.dialog.WaitSecurityDelayDialog
import com.tangem.util.Util
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_empty_wallet.*

class EmptyWalletActivity : AppCompatActivity(), NfcAdapter.ReaderCallback, CardProtocol.Notifications {

    companion object {
        val TAG: String = EmptyWalletActivity::class.java.simpleName

        private const val REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY = 2
        private const val REQUEST_CODE_REQUEST_PIN2 = 3
        private const val REQUEST_CODE_VERIFY_CARD = 4
    }

    private var nfcManager: NfcManager? = null
    private var card: TangemCard? = null
    private var lastReadSuccess = true
    private var verifyCardTask: VerifyCardTask? = null
    private var requestPIN2Count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty_wallet)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        card = TangemCard(intent.getStringExtra("UID"))
        card!!.LoadFromBundle(intent.extras!!.getBundle("Card"))

        tvIssuer.text = card!!.issuerDescription
        tvBlockchain.text = card!!.blockchainName
        tvCardID.text = card!!.cidDescription
        imgBlockchain.setImageResource(card!!.blockchain.getImageResource(this, card!!.tokenSymbol))

        if (card!!.useDefaultPIN1()!!) {
            imgPIN.setImageResource(R.drawable.unlock_pin1)
            imgPIN.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.this_banknote_protected_default_PIN1_code, Toast.LENGTH_LONG).show() }
        } else {
            imgPIN.setImageResource(R.drawable.lock_pin1)
            imgPIN.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.this_banknote_protected_user_PIN1_code, Toast.LENGTH_LONG).show() }
        }

        if (card!!.pauseBeforePIN2 > 0 && (card!!.useDefaultPIN2()!! || !card!!.useSmartSecurityDelay())) {
            imgPIN2orSecurityDelay.setImageResource(R.drawable.timer)
            imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, String.format(getString(R.string.this_banknote_will_enforce), card!!.pauseBeforePIN2 / 1000.0), Toast.LENGTH_LONG).show() }

        } else if (card!!.useDefaultPIN2()!!) {
            imgPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2)
            imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.this_banknote_protected_default_PIN2_code, Toast.LENGTH_LONG).show() }
        } else {
            imgPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2)
            imgPIN2orSecurityDelay.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.this_banknote_protected_user_PIN2_code, Toast.LENGTH_LONG).show() }
        }

        if (card!!.useDevelopersFirmware()!!) {
            imgDeveloperVersion.setImageResource(R.drawable.ic_developer_version)
            imgDeveloperVersion.visibility = View.VISIBLE
            imgDeveloperVersion.setOnClickListener { Toast.makeText(this@EmptyWalletActivity, R.string.unlocked_banknote_only_development_use, Toast.LENGTH_LONG).show() }
        } else
            imgDeveloperVersion.visibility = View.INVISIBLE

        btnNewWallet.setOnClickListener {
            requestPIN2Count = 0
            val intent = Intent(baseContext, RequestPINActivity::class.java)
            intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
            intent.putExtra("UID", card!!.uid)
            intent.putExtra("Card", card!!.asBundle)
            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2)
        }
    }

    public override fun onResume() {
        super.onResume()
        nfcManager!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        nfcManager!!.onPause()
    }

    public override fun onStop() {
        super.onStop()
        nfcManager!!.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY) {
            if (resultCode == Activity.RESULT_OK) {

                if (data != null) {
                    data.putExtra("modification", "updateAndViewCard")
                    data.putExtra("updateDelay", 0)
                    setResult(Activity.RESULT_OK, data)
                }
                finish()
            } else {
                if (data != null && data.extras!!.containsKey("UID") && data.extras!!.containsKey("Card")) {
                    val updatedCard = TangemCard(data.getStringExtra("UID"))
                    updatedCard.LoadFromBundle(data.getBundleExtra("Card"))
                    card = updatedCard
                }
                if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                    requestPIN2Count++
                    val intent = Intent(baseContext, RequestPINActivity::class.java)
                    intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString())
                    intent.putExtra("UID", card!!.uid)
                    intent.putExtra("Card", card!!.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2)
                    return
                }
            }
            setResult(resultCode, data)
            finish()
        } else if (requestCode == REQUEST_CODE_REQUEST_PIN2) {
            if (resultCode == Activity.RESULT_OK) {
                doCreateNewWallet()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            val isoDep = IsoDep.get(tag)
                    ?: throw CardProtocol.TangemException(getString(R.string.wrong_tag_err))
            val uid = tag.id
            val sUID = Util.byteArrayToHexString(uid)
            if (card!!.uid != sUID) {
//                Log.d(TAG, "Invalid UID: " + sUID);
                nfcManager!!.ignoreTag(isoDep.tag)
                return
            } else {
//                Log.v(TAG, "UID: " + sUID);
            }

            if (lastReadSuccess) {
                isoDep.timeout = 1000
            } else {
                isoDep.timeout = 65000
            }
            //lastTag = tag;
            verifyCardTask = VerifyCardTask(this, card, nfcManager, isoDep, this)
            verifyCardTask!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun OnReadStart(cardProtocol: CardProtocol) {
        progressBar!!.post {
            progressBar!!.visibility = View.VISIBLE
            progressBar!!.progress = 5
        }
    }

    override fun OnReadFinish(cardProtocol: CardProtocol?) {
        verifyCardTask = null
        if (cardProtocol != null) {
            if (cardProtocol.error == null) {
                progressBar!!.post {
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.GREEN)
                    val intent = Intent(this@EmptyWalletActivity, VerifyCardActivity::class.java)
                    intent.putExtra("UID", cardProtocol.card.uid)
                    intent.putExtra("Card", cardProtocol.card.asBundle)
                    startActivityForResult(intent, REQUEST_CODE_VERIFY_CARD)
                    //addCard(cardProtocol.getCard());
                }
            } else {
                // remove last UIDs because of error and no card read
                progressBar!!.post {
                    lastReadSuccess = false
                    if (cardProtocol.error is CardProtocol.TangemException_ExtendedLengthNotSupported) {
                        if (!NoExtendedLengthSupportDialog.allReadyShowed) {
                            NoExtendedLengthSupportDialog().show(fragmentManager, NoExtendedLengthSupportDialog.TAG)
                        }
                    } else {
                        Toast.makeText(this@EmptyWalletActivity, R.string.try_to_scan_again, Toast.LENGTH_LONG).show()
                    }
                    progressBar!!.progress = 100
                    progressBar!!.progressTintList = ColorStateList.valueOf(Color.RED)
                }
            }
        }

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

    override fun OnReadProgress(protocol: CardProtocol, progress: Int) {
        progressBar!!.post { progressBar!!.progress = progress }
    }

    override fun OnReadCancel() {
        verifyCardTask = null
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

    override fun OnReadWait(msec: Int) {
        WaitSecurityDelayDialog.OnReadWait(this, msec)
    }

    override fun OnReadBeforeRequest(timeout: Int) {
        WaitSecurityDelayDialog.onReadBeforeRequest(this, timeout)
    }

    override fun OnReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(this)
    }

    private fun doCreateNewWallet() {
        val intent = Intent(this, CreateNewWalletActivity::class.java)
        intent.putExtra("UID", card!!.uid)
        intent.putExtra("Card", card!!.asBundle)
        startActivityForResult(intent, REQUEST_CODE_CREATE_NEW_WALLET_ACTIVITY)
    }

}