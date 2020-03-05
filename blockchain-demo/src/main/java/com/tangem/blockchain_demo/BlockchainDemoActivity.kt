package com.tangem.blockchain_demo

import android.os.Bundle
import android.text.Editable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tangem.CardManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.Signer
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain_demo.databinding.ActivityBlockchainDemoBinding
import com.tangem.commands.Card
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskError
import com.tangem.tasks.TaskEvent
import kotlinx.coroutines.*
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

class BlockchainDemoActivity : AppCompatActivity() {

    private lateinit var cardManager: CardManager
    private lateinit var signer: TransactionSigner
    private lateinit var card: Card
    private lateinit var walletManager: WalletManager
    private var issuerDataCounter: Int = 1

    private lateinit var fee: BigDecimal

    private lateinit var binding: ActivityBlockchainDemoBinding

    private val parentJob = Job()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        handleError(throwable.localizedMessage)
    }
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.IO + exceptionHandler
    private val scope = CoroutineScope(coroutineContext)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBlockchainDemoBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        cardManager = CardManager.init(this)
        signer = Signer(cardManager)

        binding.btnScan.setOnClickListener { scan() }

        binding.btnCheckFee.setOnClickListener { requestFee() }

        binding.btnSend.setOnClickListener { send() }
    }


    private fun scan() {
        cardManager.scanCard { taskEvent ->
            when (taskEvent) {
                is TaskEvent.Event -> {
                    when (taskEvent.data) {
                        is ScanEvent.OnReadEvent -> {
                            card = (taskEvent.data as ScanEvent.OnReadEvent).card
                            walletManager = WalletManagerFactory.makeWalletManager(card)!!
                            getInfo()
                        }
                    }
                }
                is TaskEvent.Completion -> {
                    if (taskEvent.error != null) {
                        if (taskEvent.error !is TaskError.UserCancelled) {
                            handleError(taskEvent.error.toString())
                        }
                    }
                }
            }
        }
    }

    private fun getInfo() {
        scope.launch {
            walletManager.update()
            withContext(Dispatchers.Main) {
                binding.tvBalance.text =
                        "${walletManager.wallet.balances[AmountType.Coin]?.value
                                ?: "error"} ${walletManager.blockchain.currency}"
                val token = walletManager.wallet.balances[AmountType.Token]
                if (token != null) {
                    binding.tvBalance.text = token.currencySymbol + " " + token.value
                    binding.etSumToSend.text = Editable.Factory.getInstance().newEditable(
                            token.value.toString()
                    )
                } else {
                    binding.etSumToSend.text =
                            Editable.Factory.getInstance().newEditable(
                                    walletManager.wallet.balances[AmountType.Coin]?.value.toString()
                            )
                }
                binding.btnCheckFee.isEnabled = true
            }
        }
    }

    private fun requestFee() {
        if (binding.etReceiverAddress.text.isBlank()) {
            Toast.makeText(this, "Please enter receiver address", Toast.LENGTH_LONG).show()
            return
        } else if (binding.etSumToSend.text.isBlank()) {
            Toast.makeText(this, "Choose sum to send", Toast.LENGTH_LONG).show()
            return
        }

        scope.launch {
            val feeResult = (walletManager as FeeProvider).getFee(
                    walletManager.wallet.balances[AmountType.Token]
                            ?: walletManager.wallet.balances[AmountType.Coin]!!,
                    binding.etReceiverAddress.text.toString())
            withContext(Dispatchers.Main) {
                when (feeResult) {
                    is Result.Failure -> {
                        handleError(feeResult.error?.localizedMessage ?: "Error")
                    }
                    is Result.Success -> {
                        binding.btnSend.isEnabled = true
                        val fees = feeResult.data
                        if (fees.size == 1) {
                            binding.tvFee.text = fees[0].value.toString()
                            fee = fees[0].value ?: BigDecimal(0)
                        } else {
                            binding.tvFee.text = fees[0].value.toString() + "\n" +
                                    fees[1].value.toString() + "\n" +
                                    fees[2].value.toString()
                            fee = fees[1].value ?: BigDecimal(0)

                        }
                    }
                }
            }
        }
    }

    private fun send() {
        scope.launch {
            val result = (walletManager as TransactionSender).send(
                    formTransactionData(),
                    signer)
            withContext(Dispatchers.Main) {
                when (result) {
                    is SimpleResult.Failure -> {
                        handleError(result.error?.localizedMessage ?: "Error")
                    }
                    is SimpleResult.Success -> {
                        binding.tvFee.text = "Success"
                    }
                }
            }
        }
    }

    private fun formTransactionData(): TransactionData {
        val amount = if (walletManager.wallet.balances[AmountType.Token] != null) {
            walletManager.wallet.balances[AmountType.Token]!!
        } else {
            walletManager.wallet.balances[AmountType.Coin]!!.copy(
                    value = binding.etSumToSend.text.toString().toBigDecimal() - fee
            )
        }
        return TransactionData(
                amount,
                walletManager.wallet.balances[AmountType.Coin]!!.copy(value = fee),
                walletManager.wallet.balances[AmountType.Coin]!!.address!!,
                binding.etReceiverAddress.text.toString()
        )
    }

    private fun handleError(error: String?) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}