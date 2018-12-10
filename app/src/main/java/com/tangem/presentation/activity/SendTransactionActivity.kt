package com.tangem.presentation.activity

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.Toast
import com.tangem.data.network.ElectrumRequest
import com.tangem.data.network.ServerApiElectrum
import com.tangem.data.network.ServerApiInfura
import com.tangem.data.network.model.InfuraResponse
import com.tangem.tangemcard.android.reader.NfcManager
import com.tangem.domain.wallet.*
import com.tangem.domain.wallet.eth.EthData
import com.tangem.data.Blockchain
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import java.io.IOException
import java.lang.Exception
import java.math.BigInteger

class SendTransactionActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        const val EXTRA_TX: String = "TX"
    }

    private var serverApiInfura: ServerApiInfura = ServerApiInfura()
    private var serverApiElectrum: ServerApiElectrum = ServerApiElectrum()

    private lateinit var ctx: TangemContext
    private var tx: String? = null
    private var nfcManager: NfcManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_transaction)

//        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        ctx = TangemContext.loadFromBundle(this, intent.extras)
        tx = intent.getStringExtra(EXTRA_TX)

        val engine = CoinEngineFactory.create(ctx)

        if (ctx.blockchain == Blockchain.Ethereum || ctx.blockchain == Blockchain.EthereumTestNet || ctx.blockchain == Blockchain.Token)
            requestInfura(ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION, "")
        else if (ctx.blockchain == Blockchain.Bitcoin || ctx.blockchain == Blockchain.BitcoinTestNet)
            requestElectrum(ctx, ElectrumRequest.broadcast(ctx.coinData!!.wallet, tx))
        else if (ctx.blockchain == Blockchain.BitcoinCash)
            requestElectrum(ctx, ElectrumRequest.broadcast(ctx.coinData!!.wallet, tx))

        // request electrum listener
        val electrumBodyListener: ServerApiElectrum.ElectrumRequestDataListener = object : ServerApiElectrum.ElectrumRequestDataListener {
            override fun onSuccess(electrumRequest: ElectrumRequest?) {
                if (electrumRequest!!.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
                    try {
                        if (electrumRequest.resultString.isNullOrEmpty())
                            finishWithError("Rejected by node: " + electrumRequest.getError())
                        else
                            finishWithSuccess()
                    }
                    catch (e: Exception)
                    {
                        if( e.message!=null )
                        {
                            finishWithError(e.message!!)
                        }else{
                            finishWithError(e.javaClass.name)
                        }
                    }
                }
            }

            override fun onFail(message: String?) {
                finishWithError(message!!)
            }
        }
        serverApiElectrum.setElectrumRequestData(electrumBodyListener)

        // request infura listener
        val infuraBodyListener: ServerApiInfura.InfuraBodyListener = object : ServerApiInfura.InfuraBodyListener {
            override fun onSuccess(method: String, infuraResponse: InfuraResponse) {
                when (method) {
                    ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION -> {
                        if (infuraResponse.result.isEmpty())
                            finishWithError("Rejected by node: " + infuraResponse.error)
                        else {
                            val nonce = (ctx.coinData!! as EthData).confirmedTXCount
                            nonce.add(BigInteger.valueOf(1))
                            (ctx.coinData!! as EthData).confirmedTXCount = nonce
                            finishWithSuccess()
                        }
                    }
                }
            }

            override fun onFail(method: String, message: String) {
                when (method) {
                    ServerApiInfura.INFURA_ETH_SEND_RAW_TRANSACTION -> {
                        finishWithError(message)
                    }
                }
            }
        }
        serverApiInfura.setInfuraResponse(infuraBodyListener)
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
            serverApiInfura.infura(method, 67, ctx.coinData!!.wallet, contract, tx)
        } else
            finishWithError(getString(R.string.no_connection))
    }

    private fun requestElectrum(ctx: TangemContext, electrumRequest: ElectrumRequest) {
        if (UtilHelper.isOnline(this)) {
            serverApiElectrum.electrumRequestData(ctx, electrumRequest)
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