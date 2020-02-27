package com.tangem.blockchain.ethereum

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.ethereum.network.EthereumNetworkManager
import com.tangem.blockchain.ethereum.network.EthereumResponse
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.common.extensions.toHexString
import com.tangem.tasks.TaskEvent
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.*
import java.math.BigDecimal
import java.math.BigInteger

class EthereumWalletManager(
        private val cardId: String,
        private val walletPublicKey: ByteArray,
        chain: Chain,
        walletConfig: WalletConfig
) : WalletManager,
        TransactionSender,
        FeeProvider {

    override val blockchain: Blockchain = Blockchain.Ethereum
    private val address = blockchain.makeAddress(walletPublicKey)
    private val currencyWallet = CurrencyWallet(walletConfig, address)
    override var wallet: Wallet = currencyWallet
    private val builder = EthereumTransactionBuilder(chain)
    private val networkManager = EthereumNetworkManager()
    private var pendingTxCount = -1L
    private var txCount = -1L

    override suspend fun update() {
        val result = networkManager.getInfo(address, currencyWallet.balances[AmountType.Token]?.address)
        when (result) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: EthereumResponse) {
        currencyWallet.balances[AmountType.Coin]?.value = data.balance
        currencyWallet.balances[AmountType.Token]?.value = data.tokenBalance
        txCount = data.txCount
        pendingTxCount = data.pendingTxCount
        if (txCount == pendingTxCount) {
            currencyWallet.pendingTransactions.forEach { it.status = TransactionStatus.Confirmed }
        } else if (currencyWallet.pendingTransactions.isEmpty()) {
            currencyWallet.pendingTransactions.add(TransactionData(
                    Amount(blockchain.currency, decimals = blockchain.decimals),
                    null,
                    "unknown",
                    currencyWallet.address))
        }
    }

    private fun updateError(error: Throwable?) {

    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val transactionToSign = builder.buildToSign(transactionData, txCount.toBigInteger())
                ?: return SimpleResult.Failure(Exception("Not enough data"))
        when (val signerResponse = signer.sign(transactionToSign.hashes.toTypedArray(), cardId)) {
            is TaskEvent.Event -> {
                val transactionToSend = builder.buildToSend(signerResponse.data.signature, transactionToSign, walletPublicKey)
                return networkManager.sendTransaction(String.format("0x%s", transactionToSend.toHexString()))
            }
            is TaskEvent.Completion -> return SimpleResult.Failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, source: String, destination: String): Result<List<Amount>> {
        val result = networkManager.getFee(getGasLimit(amount).value)
        when (result) {
            is Result.Success -> {
                val feeValues: List<BigDecimal> = result.data
                return Result.Success(
                        feeValues.map { Amount(blockchain.currency, it, address, blockchain.decimals) })
            }
            is Result.Failure -> return result
        }
    }
}

private class EthereumTransactionBuilder(private val chain: Chain) {

    fun buildToSign(transactionData: TransactionData, nonce: BigInteger?): TransactionToSign? {

        val amount: BigDecimal = transactionData.amount.value ?: return null
        val transactionFee: BigDecimal = transactionData.fee?.value ?: return null

        val value = amount.movePointRight(transactionData.amount.decimals.toInt()).toBigInteger()
        val fee = transactionFee.movePointRight(transactionData.fee.decimals.toInt()).toBigInteger()

        val transaction = createTransactionWithDefaults(
                from = Address(transactionData.sourceAddress),
                to = Address(transactionData.destinationAddress),
                value = value,
                gasPrice = fee.divide(DEFAULT_GAS_LIMIT),
                gasLimit = DEFAULT_GAS_LIMIT,
                nonce = nonce,
                chain = ChainId(chain.id.toLong())
        )
        val hash = transaction.encodeRLP(SignatureData(v = chain.id.toBigInteger())).keccak()
        return TransactionToSign(transaction, listOf(hash))
    }

    fun buildToSend(signature: ByteArray, transactionToSign: TransactionToSign, walletPublicKey: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(transactionToSign.hashes[0], PublicKey(walletPublicKey.sliceArray(1..64)))
        val v = (recId + 27 + 8 + (chain.id * 2)).toBigInteger()
        val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, v)

        return transactionToSign.transaction.encodeRLP(signatureData)
    }
}

private class TransactionToSign(val transaction: Transaction, val hashes: List<ByteArray>)

enum class GasLimit(val value: Long) {
    Default(21000),
    Token(60000),
    High(300000)
}

private fun getGasLimit(amount: Amount): GasLimit {
    return when (amount.currencySymbol) {
        Blockchain.Ethereum.currency -> GasLimit.Default
        "DGX" -> GasLimit.High
        "CGT" -> GasLimit.High
        else -> GasLimit.Token
    }
}