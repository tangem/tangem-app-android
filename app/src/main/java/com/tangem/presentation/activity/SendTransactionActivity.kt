package com.tangem.presentation.activity

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.Toast
import com.tangem.data.network.ElectrumRequest
import com.tangem.data.network.ServerApiHelper
import com.tangem.data.network.ServerApiHelperElectrum
import com.tangem.data.network.model.InfuraResponse
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.Blockchain
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemCard
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import org.json.JSONException
import java.io.IOException
import java.math.BigInteger

class SendTransactionActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        const val EXTRA_TX: String = "TX"
    }

    private var serverApiHelper: ServerApiHelper = ServerApiHelper()
    private var serverApiHelperElectrum: ServerApiHelperElectrum = ServerApiHelperElectrum()

    private var card: TangemCard? = null
    private var tx: String? = null
    private var nfcManager: NfcManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_transaction)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        val intent = intent
        card = TangemCard(getIntent().getStringExtra(TangemCard.EXTRA_UID))
        card!!.loadFromBundle(intent.extras!!.getBundle(TangemCard.EXTRA_CARD))
        tx = intent.getStringExtra(EXTRA_TX)

        val engine = CoinEngineFactory.create(card!!.blockchain)

        if (card!!.blockchain == Blockchain.Ethereum || card!!.blockchain == Blockchain.EthereumTestNet || card!!.blockchain == Blockchain.Token)
            requestInfura(ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION, "")
        else if (card!!.blockchain == Blockchain.Bitcoin || card!!.blockchain == Blockchain.BitcoinTestNet)
            requestElectrum(card!!, ElectrumRequest.broadcast(card!!.wallet, tx))
        else if (card!!.blockchain == Blockchain.BitcoinCash || card!!.blockchain == Blockchain.BitcoinCashTestNet)
            requestElectrum(card!!, ElectrumRequest.broadcast(card!!.wallet, tx))

        // request electrum listener
        val electrumBodyListener: ServerApiHelperElectrum.ElectrumRequestDataListener = object : ServerApiHelperElectrum.ElectrumRequestDataListener {
            override fun onSuccess(electrumRequest: ElectrumRequest?) {
                if (electrumRequest!!.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
                    try {
                        var hashTX = electrumRequest.resultString
                        try {
                            if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                hashTX = hashTX.substring(2)
                            }
                            finishWithSuccess()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            requestElectrum(card!!, ElectrumRequest.broadcast(card!!.wallet, tx))
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        requestElectrum(card!!, ElectrumRequest.broadcast(card!!.wallet, tx))
                    }
                }
            }

            override fun onFail(message: String?) {
                finishWithError(message!!)
            }
        }
        serverApiHelperElectrum.setElectrumRequestData(electrumBodyListener)

        // request infura listener
        val infuraBodyListener: ServerApiHelper.InfuraBodyListener = object : ServerApiHelper.InfuraBodyListener {
            override fun onSuccess(method: String, infuraResponse: InfuraResponse) {
                when (method) {
                    ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION -> {
                        try {
                            var hashTX: String
                            try {
                                val tmp = infuraResponse.result
                                hashTX = tmp
                            } catch (e: JSONException) {
                                e.printStackTrace()
                                requestInfura(ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION, "")
                                return
                            }

                            if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                hashTX = hashTX.substring(2)
                            }

                            val nonce = card!!.confirmedTXCount
                            nonce.add(BigInteger.valueOf(1))
                            card!!.confirmedTXCount = nonce

                            finishWithSuccess()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            requestInfura(ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION, "")
                        }
                    }
                }
            }

            override fun onFail(method: String, message: String) {
                when (method) {
                    ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION -> {
                        finishWithError(message)
                    }
                }
            }
        }
        serverApiHelper.setInfuraResponse(infuraBodyListener)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                Toast.makeText(this, R.string.please_wait, Toast.LENGTH_LONG).show()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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

    override fun onTagDiscovered(tag: Tag) {
        try {
            nfcManager!!.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun requestInfura(method: String, contract: String) {
        if (UtilHelper.isOnline(this)) {
            serverApiHelper.infura(method, 67, card!!.wallet, contract, tx)
        } else
            finishWithError(getString(R.string.no_connection))
    }

    private fun requestElectrum(card: TangemCard, electrumRequest: ElectrumRequest) {
        if (UtilHelper.isOnline(this)) {
            serverApiHelperElectrum.electrumRequestData(card, electrumRequest)
        } else
            finishWithError(getString(R.string.no_connection))
    }

    private fun finishWithSuccess() {
        val intent = Intent()
        intent.putExtra("message", getString(R.string.transaction_has_been_successfully_signed))
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun finishWithError(message: String) {
        val intent = Intent()
        intent.putExtra("message", String.format(getString(R.string.try_again_failed_to_send_transaction), message))
        setResult(RESULT_CANCELED, intent)
        finish()
    }

}