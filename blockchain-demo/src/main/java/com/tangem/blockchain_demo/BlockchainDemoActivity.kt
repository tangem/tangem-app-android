package com.tangem.blockchain_demo

import android.os.Bundle
import android.text.Editable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tangem.SessionError
import com.tangem.TangemSdk
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Signer
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain_demo.databinding.ActivityBlockchainDemoBinding
import com.tangem.commands.Card
import com.tangem.common.CompletionResult
import com.tangem.tangem_sdk_new.extensions.init
import kotlinx.coroutines.*
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

class BlockchainDemoActivity : AppCompatActivity() {

    private lateinit var tangemSdk: TangemSdk
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

        tangemSdk = TangemSdk.init(this)
        signer = Signer(tangemSdk)

        binding.btnScan.setOnClickListener { scan() }

        binding.btnCheckFee.setOnClickListener { requestFee() }

        binding.btnSend.setOnClickListener { send() }
    }


    private fun scan() {
        tangemSdk.scanCard { result ->
            when (result) {
                is CompletionResult.Success -> {
                            walletManager = WalletManagerFactory.makeWalletManager(result.data)!!
                            getInfo()
                        }
                is CompletionResult.Failure -> {
                        if (result.error !is SessionError.UserCancelled) {
                            handleError(result.error.toString())
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
                        "${walletManager.wallet.amounts[AmountType.Coin]?.value?.toPlainString()
                                ?: "error"} ${walletManager.wallet.blockchain.currency}"
                val token = walletManager.wallet.amounts[AmountType.Token]
                if (token != null) {
                    binding.tvBalance.text = token.value?.toPlainString() + " " + token.currencySymbol
                    binding.etSumToSend.text = Editable.Factory.getInstance().newEditable(
                            token.value?.toPlainString() ?: ""
                    )
                } else {
                    binding.etSumToSend.text =
                            Editable.Factory.getInstance().newEditable(
                                    walletManager.wallet.amounts[AmountType.Coin]?.value?.toPlainString()
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
            val feeResult = (walletManager as TransactionSender).getFee(
                    walletManager.wallet.amounts[AmountType.Token]
                            ?: walletManager.wallet.amounts[AmountType.Coin]!!,
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
        val amount = if (walletManager.wallet.amounts[AmountType.Token] != null) {
            walletManager.wallet.amounts[AmountType.Token]!!
        } else {
            walletManager.wallet.amounts[AmountType.Coin]!!.copy(
                    value = binding.etSumToSend.text.toString().toBigDecimal() - fee
            )
        }
        return TransactionData(
                amount,
                walletManager.wallet.amounts[AmountType.Coin]!!.copy(value = fee),
                walletManager.wallet.amounts[AmountType.Coin]!!.address!!,
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