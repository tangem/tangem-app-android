package com.tangem.tap.domain.walletconnect

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumGasLoader
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.Companion.toKeccak
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.extensions.isAscii
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.operations.sign.SignHashCommand
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.analytics.events.Basic.TransactionSent.MemoType
import com.tangem.tap.common.analytics.events.WalletConnect
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.domain.walletconnect.BnbHelper.toWCBinanceTradeOrder
import com.tangem.tap.domain.walletconnect.BnbHelper.toWCBinanceTransferOrder
import com.tangem.tap.domain.walletconnect2.domain.WcEthereumSignMessage
import com.tangem.tap.domain.walletconnect2.domain.WcEthereumTransaction
import com.tangem.tap.domain.walletconnect2.domain.models.EthTransactionData
import com.tangem.tap.domain.walletconnect2.domain.models.binance.WcBinanceTradeOrder
import com.tangem.tap.domain.walletconnect2.domain.models.binance.WcBinanceTransferOrder
import com.tangem.tap.features.details.redux.walletconnect.*
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PersonalSignDialogData
import com.tangem.tap.features.details.ui.walletconnect.dialogs.TransactionRequestDialogData
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import com.tangem.tap.tangemSdkManager
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage.WCSignType.*
import timber.log.Timber
import java.math.BigDecimal

class WalletConnectSdkHelper {

    @Suppress("MagicNumber")
    suspend fun prepareTransactionData(data: EthTransactionData): WcTransactionData? {
        val transaction = data.transaction
        val blockchain = Blockchain.fromNetworkId(data.networkId) ?: return null
        val walletManager = getWalletManager(blockchain, data.rawDerivationPath) ?: return null

        walletManager.safeUpdate()
        val wallet = walletManager.wallet
        val balance = wallet.amounts[AmountType.Coin]?.value ?: return null

        val decimals = wallet.blockchain.decimals()

        val value = (transaction.value ?: "0").hexToBigDecimal()
            ?.movePointLeft(decimals) ?: return null

        val gasLimit = getGasLimitFromTx(value, walletManager, transaction)

        val gasPrice = transaction.gasPrice?.hexToBigDecimal()
            ?: when (val result = (walletManager as? EthereumGasLoader)?.getGasPrice()) {
                is Result.Success -> result.data.toBigDecimal()
                is Result.Failure -> {
                    (result.error as? Throwable)?.let { Timber.e(it, "getGasPrice failed") }
                    return null
                }
                null -> return null
            }

        val fee = (gasLimit * gasPrice).movePointLeft(decimals)
        val total = value + fee

        val transactionData = TransactionData(
            amount = Amount(value, wallet.blockchain),
            fee = Amount(fee, wallet.blockchain),
            sourceAddress = transaction.from,
            destinationAddress = transaction.to!!,
            extras = EthereumTransactionExtras(
                data = transaction.data.removePrefix(HEX_PREFIX).hexToBytes(),
                gasLimit = gasLimit.toBigInteger(),
                nonce = transaction.nonce?.hexToBigDecimal()?.toBigInteger(),
            ),
        )

        val dialogData = TransactionRequestDialogData(
            dAppName = data.metaName,
            dAppUrl = data.metaUrl,
            amount = value.toFormattedString(decimals),
            gasAmount = fee.toFormattedString(decimals),
            totalAmount = total.toFormattedString(decimals),
            balance = balance.toFormattedString(decimals),
            isEnoughFundsToSend = balance - total >= BigDecimal.ZERO,
            topic = data.topic,
            id = data.id,
            type = data.type,
        )
        return WcTransactionData(
            type = data.type,
            transaction = transactionData,
            topic = data.topic,
            id = data.id,
            walletManager = walletManager,
            dialogData = dialogData,
        )
    }

    private fun getWalletManager(blockchain: Blockchain, derivationPath: String?): WalletManager? {
        val blockchainNetwork = BlockchainNetwork(
            blockchain = blockchain,
            derivationPath = derivationPath,
            tokens = emptyList(),
        )
        return store.state.walletState.getWalletManager(blockchainNetwork)
    }

    suspend fun completeTransaction(data: WcTransactionData, cardId: String?): String? {
        return when (data.type) {
            WcEthTransactionType.EthSendTransaction -> sendTransaction(data, cardId)
            WcEthTransactionType.EthSignTransaction -> signTransaction(data, cardId)
        }
    }

    private suspend fun getGasLimitFromTx(
        value: BigDecimal,
        walletManager: WalletManager,
        transaction: WcEthereumTransaction,
    ): BigDecimal {
        return transaction.gas?.hexToBigDecimal()
            ?: transaction.gasLimit?.hexToBigDecimal()
            ?: getGaLimitFromBlockchain(
                value = value,
                walletManager = walletManager,
                transaction = transaction,
            )
    }

    private suspend fun getGaLimitFromBlockchain(
        value: BigDecimal,
        walletManager: WalletManager,
        transaction: WcEthereumTransaction,
    ): BigDecimal {
        val gasLimitResult = (walletManager as? EthereumGasLoader)?.getGasLimit(
            amount = Amount(value, walletManager.wallet.blockchain),
            destination = transaction.to ?: "",
            data = transaction.data,
        )
        return when (gasLimitResult) {
            is Result.Success -> gasLimitResult.data.toBigDecimal().multiply(BigDecimal("1.2"))
            is Result.Failure -> {
                (gasLimitResult.error as? Throwable)?.let { Timber.e(it, "getGasLimit failed") }
                BigDecimal(DEFAULT_MAX_GASLIMIT) // Set high gasLimit if not provided
            }
            else -> BigDecimal(DEFAULT_MAX_GASLIMIT) // Set high gasLimit if not provided
        }
    }

    private suspend fun sendTransaction(data: WcTransactionData, cardId: String?): String? {
        val result = (data.walletManager as TransactionSender).send(
            transactionData = data.transaction,
            signer = CommonSigner(tangemSdk, cardId),
        )
        return when (result) {
            SimpleResult.Success -> {
                val sentFrom = AnalyticsParam.TxSentFrom.WalletConnect
                Analytics.send(Basic.TransactionSent(sentFrom = sentFrom, memoType = MemoType.Null))
                HEX_PREFIX + data.walletManager.wallet.recentTransactions.last().hash
            }
            is SimpleResult.Failure -> {
                (result.error as? TangemSdkError)?.let { Analytics.send(WalletConnect.TransactionError(it)) }
                Timber.e(result.error as BlockchainSdkError)
                null
            }
        }
    }

    private suspend fun signTransaction(data: WcTransactionData, cardId: String?): String? {
        val dataToSign = EthereumUtils.buildTransactionToSign(
            transactionData = data.transaction,
            nonce = null,
            blockchain = data.walletManager.wallet.blockchain,
            gasLimit = null,
        ) ?: return null

        val command = SignHashCommand(
            hash = dataToSign.hash,
            walletPublicKey = data.walletManager.wallet.publicKey.seedKey,
            derivationPath = data.walletManager.wallet.publicKey.derivationPath,
        )
        val result = tangemSdkManager.runTaskAsync(command, initialMessage = Message(), cardId = cardId)
        return when (result) {
            is CompletionResult.Success -> {
                HEX_PREFIX + EthereumUtils.prepareTransactionToSend(
                    signature = result.data.signature,
                    transactionToSign = dataToSign,
                    walletPublicKey = data.walletManager.wallet.publicKey,
                    blockchain = data.walletManager.wallet.blockchain,
                ).toHexString()
            }
            is CompletionResult.Failure -> {
                (result.error as? TangemSdkError)?.let { Analytics.send(WalletConnect.SignError(it)) }
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    fun prepareBnbTradeOrder(data: WcBinanceTradeOrder): BinanceMessageData.Trade {
        return BnbHelper.createMessageData(data.toWCBinanceTradeOrder())
    }

    fun prepareBnbTransferOrder(data: WcBinanceTransferOrder): BinanceMessageData.Transfer {
        return BnbHelper.createMessageData(data.toWCBinanceTransferOrder())
    }

    suspend fun signBnbTransaction(
        data: ByteArray,
        networkId: String,
        derivationPath: String?,
        cardId: String?,
    ): String? {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return null
        val wallet = getWalletManager(blockchain, derivationPath)?.wallet ?: return null

        val command = SignHashCommand(
            hash = data,
            walletPublicKey = wallet.publicKey.seedKey,
            derivationPath = wallet.publicKey.derivationPath,
        )
        val result = tangemSdkManager.runTaskAsync(command, initialMessage = Message(), cardId = cardId)
        return when (result) {
            is CompletionResult.Success -> {
                val key = wallet.publicKey.blockchainKey.toDecompressedPublicKey()
                getBnbResultString(
                    key.toHexString(),
                    result.data.signature.toHexString(),
                )
            }
            is CompletionResult.Failure -> {
                (result.error as? TangemSdkError)?.let { Analytics.send(WalletConnect.TransactionError(it)) }
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    fun prepareDataForPersonalSign(
        message: WcEthereumSignMessage,
        topic: String,
        metaName: String,
        id: Long,
    ): WcPersonalSignData {
        val messageData = when (message.type) {
            WcEthereumSignMessage.WCSignType.MESSAGE,
            WcEthereumSignMessage.WCSignType.PERSONAL_MESSAGE,
            -> createMessageData(message)
            WcEthereumSignMessage.WCSignType.TYPED_MESSAGE -> EthereumUtils.makeTypedDataHash(message.data)
        }

        val messageString = message.data.hexToAscii()
            ?: EthSignHelper.tryToParseEthTypedMessageString(message.data)
            ?: message.data

        val dialogData = PersonalSignDialogData(
            dAppName = metaName,
            message = messageString,
            topic = topic,
            id = id,
        )
        return WcPersonalSignData(
            hash = messageData,
            topic = topic,
            id = id,
            dialogData = dialogData,
            type = message.type,
        )
    }

    private fun createMessageData(message: WcEthereumSignMessage): ByteArray {
        val messageData = try {
            message.data.removePrefix(HEX_PREFIX).hexToBytes()
        } catch (exception: Exception) {
            message.data.asciiToHex()?.hexToBytes() ?: byteArrayOf()
        }

        val prefixData = (ETH_MESSAGE_PREFIX + messageData.size.toString()).toByteArray()
        return (prefixData + messageData).toKeccak()
    }

    private fun String.hexToAscii(): String? {
        return try {
            removePrefix(HEX_PREFIX).hexToBytes()
                .map {
                    val char = it.toInt().toChar()
                    if (char.isAscii()) char else return null
                }
                .joinToString("")
        } catch (exception: Exception) {
            return null
        }
    }

    private fun String.asciiToHex(): String? {
        return map {
            if (!it.isAscii()) return null
            Integer.toHexString(it.code)
        }.joinToString("")
    }

    suspend fun signPersonalMessage(
        hashToSign: ByteArray,
        networkId: String,
        derivationPath: String?,
        cardId: String?,
    ): String? {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: return null
        val wallet = getWalletManager(blockchain, derivationPath)?.wallet ?: return null

        val command = SignHashCommand(
            hash = hashToSign,
            walletPublicKey = wallet.publicKey.seedKey,
            derivationPath = wallet.publicKey.derivationPath,
        )
        return when (val result = tangemSdkManager.runTaskAsync(command, cardId)) {
            is CompletionResult.Success -> {
                val hash = result.data.signature
                return EthereumUtils.prepareSignedMessageData(
                    signedHash = hash,
                    hashToSign = hashToSign,
                    publicKey = wallet.publicKey.blockchainKey.toDecompressedPublicKey(),
                )
            }
            is CompletionResult.Failure -> {
                (result.error as? TangemSdkError)?.let { Analytics.send(WalletConnect.SignError(it)) }
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    companion object {
        private const val ETH_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n"
        private const val HEX_PREFIX = "0x"
        private const val DEFAULT_MAX_GASLIMIT = 350000
        fun getBnbResultString(publicKey: String, signature: String): String {
            return "{\"signature\":\"$signature\",\"publicKey\":\"$publicKey\"}"
        }
    }
}
