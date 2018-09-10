package com.tangem.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.widget.ProgressBar
import android.widget.Toast
import com.tangem.data.network.ServerApiHelper
import com.tangem.data.network.model.InfuraResponse

import com.tangem.data.network.request.ElectrumRequest
import com.tangem.data.network.request.InfuraRequest
import com.tangem.data.network.task.send_transaction.ConnectTask
import com.tangem.data.network.task.send_transaction.ETHRequestTask
import com.tangem.domain.wallet.Blockchain
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.LastSignStorage
import com.tangem.domain.wallet.TangemCard
import com.tangem.wallet.R
import org.json.JSONException
import java.math.BigInteger

class SendTransactionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_UID: String = "UID"
        const val EXTRA_CARD: String = "Card"
        const val EXTRA_TX: String = "TX"
    }

    private var serverApiHelper: ServerApiHelper? = null
    var card: TangemCard? = null
    private var tx: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_transaction)

        MainActivity.commonInit(applicationContext)

        serverApiHelper = ServerApiHelper()

        val intent = intent
        card = TangemCard(getIntent().getStringExtra(EXTRA_UID))
        card!!.loadFromBundle(intent.extras!!.getBundle(EXTRA_CARD))
        tx = intent.getStringExtra(EXTRA_TX)

        val engine = CoinEngineFactory.create(card!!.blockchain)
        if (card!!.blockchain == Blockchain.Ethereum || card!!.blockchain == Blockchain.EthereumTestNet || card!!.blockchain == Blockchain.Token) {
//            val task = ETHRequestTask(this@SendTransactionActivity, card!!.blockchain)
//            val req = InfuraRequest.SendTransaction(card!!.wallet, tx)
//            req.id = 67
//            req.setBlockchain(card!!.blockchain)
//            task.execute(req)

            requestInfura(ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION, "")

        } else if (card!!.blockchain == Blockchain.Bitcoin || card!!.blockchain == Blockchain.BitcoinTestNet) {
            val nodeAddress = engine!!.getNode(card)
            val nodePort = engine.getNodePort(card)
            val connectTask = ConnectTask(this@SendTransactionActivity, nodeAddress, nodePort, 3)
            connectTask.execute(ElectrumRequest.broadcast(card!!.wallet, tx))
        } else if (card!!.blockchain == Blockchain.BitcoinCash || card!!.blockchain == Blockchain.BitcoinCashTestNet) {
            val nodeAddress = engine!!.getNode(card)
            val nodePort = engine.getNodePort(card)
            val connectTask = ConnectTask(this@SendTransactionActivity, nodeAddress, nodePort, 3)
            connectTask.execute(ElectrumRequest.broadcast(card!!.wallet, tx))
        }

        // request eth sendRawTransaction
        val infuraBodyListener: ServerApiHelper.InfuraBodyListener = object : ServerApiHelper.InfuraBodyListener {
            override fun onInfuraSuccess(method: String, infuraResponse: InfuraResponse) {
                when (method) {
                    ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION -> {
                        try {
                            var hashTX: String
                            try {
                                val tmp = infuraResponse.result
                                hashTX = tmp
                            } catch (e: JSONException) {
//                                val msg = request.getAnswer()
//                                val err = msg!!.getJSONObject("error")
//                                hashTX = err.getString("message")
//                                LastSignStorage.setLastMessage(card!!.wallet, hashTX)
                                return@onInfuraSuccess
                            }

                            if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                hashTX = hashTX.substring(2)
                            }
                            val bigInt = BigInteger(hashTX, 16) //TODO: очень плохой способ
                            LastSignStorage.setTxWasSend(card!!.wallet)
                            LastSignStorage.setLastMessage(card!!.wallet, "")
                            Log.e("TX_RESULT", hashTX)


                            val nonce = card!!.GetConfirmTXCount()
                            nonce.add(BigInteger.valueOf(1))
                            card!!.setConfirmTXCount(nonce)
                            Log.e("TX_RESULT", hashTX)

                            finishWithSuccess()

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    else -> {

                    }
                }
            }

            override fun onInfuraFail(method: String, message: String) {
                when (method) {
                    ServerApiHelper.INFURA_ETH_SEND_RAW_TRANSACTION -> {
                        finishWithError(message)
                    }

                    else -> {

                    }
                }
            }
        }

        serverApiHelper!!.setInfuraResponse(infuraBodyListener)
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

    private fun requestInfura(method: String, contract: String) {
        serverApiHelper!!.infura(method, 67, card!!.wallet, contract, tx)
    }

    fun finishWithError(message: String) {
        val intent = Intent()
        intent.putExtra("message", String.format(getString(R.string.try_again_failed_to_send_transaction), message))
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    fun finishWithSuccess() {
        val intent = Intent()
        intent.putExtra("message", getString(R.string.transaction_has_been_successfully_signed))
        setResult(RESULT_OK, intent)
        finish()
    }

}