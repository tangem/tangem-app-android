package com.tangem.blockchain.eth

import com.tangem.blockchain.common.*
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.tasks.TaskEvent
import org.kethereum.DEFAULT_GAS_LIMIT
import org.kethereum.ETH_IN_WEI
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.*
import java.math.BigInteger

class EthereumWalletManager(
        private val cardId: String,
        private val walletPublicKey: ByteArray,
        private val isTestNet: Boolean,
        walletConfig: WalletConfig
) : WalletManager,
        TransactionSender,
        FeeProvider {

    override val blockchain: Blockchain = Blockchain.Ethereum
    private val address = blockchain.makeAddress(walletPublicKey)
    override var wallet: Wallet = CurrencyWallet(walletConfig, address)


    override fun update() {

    }

    override fun send(transactionData: TransactionData, signer: TransactionSigner) {
        val builder = EthereumTransactionBuilder()
        val hashes = builder.buildToSign(transactionData, walletPublicKey)
        signer.sign(hashes.toTypedArray(), cardId) {
            when (it) {
                is TaskEvent.Event -> builder.buildToSend(it.data.signature, walletPublicKey)
            }
        }
    }

    override fun getFee(amount: Amount, source: String, destination: String): List<Amount> {
        val gasPrices: List<Long> = getEthGasPrices()
        val gasLimit = getGasLimit(amount).value

        val fees = mutableListOf<Amount>()
        for (gasPrice in gasPrices) {
            val feeValue = (gasPrice * gasLimit).toBigDecimal().divide(ETH_IN_WEI.toBigDecimal())
            fees.add(Amount(blockchain.currency, feeValue, address, blockchain.decimals))
        }
    }

}


private class EthereumTransactionBuilder {
    var nonce: BigInteger? = null
    var transaction: Transaction? = null
    var hashToSign: ByteArray? = null

    fun buildToSign(transactionData: TransactionData, publicKey: ByteArray): List<ByteArray> {
        val from = Address(transactionData.sourceAddress)
        val to = Address(transactionData.destinationAddress)
        val value = transactionData.amount.value!!
                .movePointRight(transactionData.amount.decimals.toInt()).toBigInteger()
        val fee = transactionData.fee!!.value!!
                .movePointRight(transactionData.fee.decimals.toInt()).toBigInteger()
        val gasPrice = fee.divide(DEFAULT_GAS_LIMIT)

        transaction = createTransactionWithDefaults(
                from = from,
                to = to,
                value = value,
                gasPrice = gasPrice,
                gasLimit = DEFAULT_GAS_LIMIT,
                nonce = nonce,
                chain = ChainId(Chain.Mainnet.id)
        )

        val transactionToSign = transaction!!
                .encodeRLP(SignatureData().apply { v = transaction!!.chain!! })
        hashToSign = transactionToSign.keccak()

        return listOf(hashToSign!!)
    }

    fun buildToSend(signature: ByteArray, walletPublicKey: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        var s = BigInteger(1, signature.copyOfRange(32, 64))

        val ecdsaSignature = ECDSASignature(r, s).canonicalise()

        val recId = ecdsaSignature.determineRecId(hashToSign!!, PublicKey(walletPublicKey))
        val v = (recId + 27 + Chain.Mainnet.id).toBigInteger() // TODO: where to put chainId?
        val signatureData = SignatureData(ecdsaSignature.r, ecdsaSignature.s, v)

        return transaction!!.encodeRLP(signatureData)
    }
}

private enum class Chain(val id: Long) {
    Mainnet(1),
    Morden(2),
    Ropsten(3),
    Rinkeby(4),
    Rootstock_mainnet(30),
    Rootstock_testnet(31),
    Kovan(42),
    Ethereum_Classic_mainnet(61),
    Ethereum_Classic_testnet(62),
    Geth_private_chains(1337),
    Matic_Testnet(8995);
}

private enum class GasLimit(val value: Long) {
    Default(210000),
    Token(60000),
    High(300000)
}

private fun getGasLimit(amount: Amount): GasLimit {
    return when (amount.currencySymbol) {
        "ETH" -> GasLimit.Default
        "DGX" -> GasLimit.High
        else -> GasLimit.Token
    }
}