package com.tangem.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.ProgressBar
import android.widget.Toast

import com.tangem.data.network.request.ElectrumRequest
import com.tangem.data.network.request.InfuraRequest
import com.tangem.data.network.task.send_transaction.ConnectTask
import com.tangem.data.network.task.send_transaction.ETHRequestTask
import com.tangem.domain.wallet.Blockchain
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemCard
import com.tangem.wallet.R

class SendTransactionActivity : AppCompatActivity() {

    var card: TangemCard? = null
    private var tx: String? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_transaction)

        MainActivity.commonInit(applicationContext)

        progressBar = findViewById(R.id.progressBar)

        val intent = intent
        card = TangemCard(getIntent().getStringExtra("UID"))
        card!!.loadFromBundle(intent.extras!!.getBundle("Card"))
        tx = intent.getStringExtra("TX")

        val engine = CoinEngineFactory.create(card!!.blockchain)
        if (card!!.blockchain == Blockchain.Ethereum || card!!.blockchain == Blockchain.EthereumTestNet || card!!.blockchain == Blockchain.Token) {
            val task = ETHRequestTask(this@SendTransactionActivity, card!!.blockchain)
            val req = InfuraRequest.SendTransaction(card!!.wallet, tx)
            req.id = 67
            req.setBlockchain(card!!.blockchain)
            task.execute(req)
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