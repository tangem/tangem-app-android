package com.tangem.blockchain.binance

import com.tangem.blockchain.binance.client.BinanceDexApiClientFactory
import com.tangem.blockchain.binance.client.BinanceDexEnvironment
import com.tangem.blockchain.binance.client.domain.broadcast.TransactionOption
import com.tangem.blockchain.binance.client.domain.broadcast.Transfer
import com.tangem.blockchain.binance.client.encoding.message.MessageType
import com.tangem.blockchain.binance.client.encoding.message.TransactionRequestAssemblerExtSign
import com.tangem.blockchain.binance.client.encoding.message.TransferMessage
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toCompressedPublicKey
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Utils
import java.math.BigInteger

class BinanceTransactionBuilder(publicKey: ByteArray, isTestNet: Boolean) {
    val chainId = if (isTestNet) "Binance-Chain-Nile" else "Binance-Chain-Tigris"
    val prefixedPubKey = MessageType.PubKey.typePrefixBytes + 33.toByte() + publicKey.toCompressedPublicKey()
    val client = BinanceDexApiClientFactory.newInstance().newRestClient(
            if (isTestNet) BinanceDexEnvironment.PROD.baseUrl else BinanceDexEnvironment.TEST_NET.baseUrl
    )

    var accountNumber: Long? = null
    var sequence: Long? = null
    var transactionAssembler: TransactionRequestAssemblerExtSign? = null
    var transferMessage: TransferMessage? = null

    fun buildToSign(transactionData: TransactionData): ByteArray {
        val transfer = Transfer()
        transfer.coin = transactionData.amount.currencySymbol
        transfer.fromAddress = transactionData.sourceAddress
        transfer.toAddress = transactionData.destinationAddress
        transfer.amount = transactionData.amount.value!!
                .setScale(Blockchain.Binance.decimals.toInt()).toPlainString()

        val options = TransactionOption.DEFAULT_INSTANCE

        val accountData = BinanceAccountData(chainId, accountNumber!!, sequence!!)

        transactionAssembler = client.prepareTransfer(transfer, accountData, prefixedPubKey, options, true)
        transferMessage = transactionAssembler!!.createTransferMessage(transfer)

        return transactionAssembler!!.prepareForSign(transferMessage).calculateSha256()
    }

    fun buildToSend(signature: ByteArray): ByteArray {
        val r = BigInteger(1, signature.copyOfRange(0, 32))
        val s = BigInteger(1, signature.copyOfRange(32, 64))
        val canonicalS = ECKey.ECDSASignature(r, s).toCanonicalised().s

        //bigIntegerToBytes cuts leading zero if present
        val canonicalSignature = Utils.bigIntegerToBytes(r, 32) + Utils.bigIntegerToBytes(canonicalS, 32)
        val encodedSignature = transactionAssembler!!.encodeSignature(canonicalSignature)

        val encodedTransferMessage = transactionAssembler!!.encodeTransferMessage(transferMessage)

        return transactionAssembler!!.encodeStdTx(encodedTransferMessage, encodedSignature)
    }
}

data class BinanceAccountData(
        val chainId: String,
        val accountNumber: Long,
        val sequence: Long
)