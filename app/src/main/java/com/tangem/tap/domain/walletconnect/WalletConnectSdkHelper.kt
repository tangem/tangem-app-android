package com.tangem.tap.domain.walletconnect

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumGasLoader
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.Companion.toKeccak
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.CommonSigner
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.extensions.isAscii
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.CryptoUtils
import com.tangem.operations.sign.SignHashCommand
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.WalletConnect
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.features.details.redux.walletconnect.WalletForSession
import com.tangem.tap.features.details.redux.walletconnect.WcPersonalSignData
import com.tangem.tap.features.details.redux.walletconnect.WcTransactionData
import com.tangem.tap.features.details.redux.walletconnect.WcTransactionType
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PersonalSignDialogData
import com.tangem.tap.features.details.ui.walletconnect.dialogs.TransactionRequestDialogData
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import com.tangem.tap.tangemSdkManager
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage.WCSignType.MESSAGE
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage.WCSignType.PERSONAL_MESSAGE
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage.WCSignType.TYPED_MESSAGE
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import timber.log.Timber
import java.math.BigDecimal

class WalletConnectSdkHelper {

    suspend fun prepareTransactionData(
        transaction: WCEthereumTransaction,
        session: WalletConnectSession,
        id: Long,
        type: WcTransactionType,
    ): WcTransactionData? {
        val walletManager = getWalletManager(session) ?: return null

        walletManager.safeUpdate()
        val wallet = walletManager.wallet
        val balance = wallet.amounts[AmountType.Coin]?.value ?: return null

        val gas = transaction.gas?.hexToBigDecimal()
            ?: transaction.gasLimit?.hexToBigDecimal()
            ?: BigDecimal(300000) //Set high gasLimit if not provided

        val decimals = wallet.blockchain.decimals()

        val value = (transaction.value ?: "0").hexToBigDecimal()
            ?.movePointLeft(decimals) ?: return null

        val gasPrice = transaction.gasPrice?.hexToBigDecimal()
            ?: when (val result =
                (walletManager as? EthereumGasLoader)?.getGasPrice()) {
                is Result.Success -> result.data.toBigDecimal()
                is Result.Failure -> {
                    (result.error as? Throwable)?.let { Timber.e(it) }
                    return null
                }
                null -> return null
            }

        val fee = (gas * gasPrice).movePointLeft(decimals)
        val total = value + fee

        val transactionData = TransactionData(
            amount = Amount(value, wallet.blockchain),
            fee = Amount(fee, wallet.blockchain),
            sourceAddress = transaction.from,
            destinationAddress = transaction.to!!,
            extras = EthereumTransactionExtras(
                data = transaction.data.removePrefix(HEX_PREFIX).hexToBytes(),
                gasLimit = gas.toBigInteger(),
                nonce = transaction.nonce?.hexToBigDecimal()?.toBigInteger(),
            ),
        )
        val dialogData = TransactionRequestDialogData(
            dAppName = session.peerMeta.name,
            dAppUrl = session.peerMeta.url,
            amount = value.toFormattedString(decimals),
            gasAmount = fee.toFormattedString(decimals),
            totalAmount = total.toFormattedString(decimals),
            balance = balance.toFormattedString(decimals),
            isEnoughFundsToSend = (balance - total) >= BigDecimal.ZERO,
            session = session.session,
            id = id,
            type = type,
        )
        return WcTransactionData(
            type = type,
            transaction = transactionData,
            session = session,
            id = id,
            walletManager = walletManager,
            dialogData = dialogData,
        )
    }

    private fun getWalletManager(session: WalletConnectSession): WalletManager? {
        val factory = store.state.globalState.tapWalletManager.walletManagerFactory
        val publicKey = Wallet.PublicKey(
            session.wallet.walletPublicKey ?: return null,
            session.wallet.derivedPublicKey,
            session.wallet.derivationPath,
        )
        val blockchain = session.wallet.getBlockchainForSession()
        return factory.makeWalletManager(
            blockchain = blockchain,
            publicKey = publicKey,
        )
    }

    suspend fun completeTransaction(data: WcTransactionData, cardId: String?): String? {
        return when (data.type) {
            WcTransactionType.EthSendTransaction -> sendTransaction(data, cardId)
            WcTransactionType.EthSignTransaction -> signTransaction(data, cardId)
        }
    }

    private suspend fun sendTransaction(data: WcTransactionData, cardId: String?): String? {
        val result = (data.walletManager as TransactionSender).send(
            transactionData = data.transaction,
            signer = CommonSigner(tangemSdk, cardId),
        )
        return when (result) {
            SimpleResult.Success -> {
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
                HEX_PREFIX + result.data
            }
            is CompletionResult.Failure -> {
                (result.error as? TangemSdkError)?.let { Analytics.send(WalletConnect.SignError(it)) }
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    suspend fun signBnbTransaction(data: ByteArray, session: WalletConnectActiveData, cardId: String?): String? {
        val command = SignHashCommand(
            hash = data,
            walletPublicKey = session.wallet.walletPublicKey ?: return null,
            derivationPath = session.wallet.derivationPath,
        )
        val result = tangemSdkManager.runTaskAsync(command, initialMessage = Message(), cardId = cardId)
        return when (result) {
            is CompletionResult.Success -> {
                val key = session.wallet.derivedPublicKey?.toDecompressedPublicKey()
                    ?: session.wallet.walletPublicKey.toDecompressedPublicKey()
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
        message: WCEthereumSignMessage,
        session: WalletConnectSession,
        id: Long,
    ): WcPersonalSignData {
        val messageData = when (message.type) {
            MESSAGE, PERSONAL_MESSAGE -> createMessageData(message)
            TYPED_MESSAGE -> EthereumUtils.makeTypedDataHash(message.data)
        }

        val messageString = message.data.hexToAscii()
            ?: EthSignHelper.tryToParseEthTypedMessageString(message.data)
            ?: message.data

        val dialogData = PersonalSignDialogData(
            dAppName = session.peerMeta.name,
            message = messageString,
            session = session.session,
            id = id,
        )
        return WcPersonalSignData(
            hash = messageData,
            session = session,
            id = id,
            dialogData = dialogData,
        )
    }

    private fun createMessageData(message: WCEthereumSignMessage): ByteArray {
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

    suspend fun signPersonalMessage(hashToSign: ByteArray, wallet: WalletForSession, cardId: String?): String? {
        val key = wallet.derivedPublicKey ?: wallet.walletPublicKey
        val command = SignHashCommand(hashToSign, wallet.walletPublicKey!!, wallet.derivationPath)
        return when (val result = tangemSdkManager.runTaskAsync(command, cardId)) {
            is CompletionResult.Success -> {
                val hash = result.data.signature
                return EthereumUtils.prepareSignedMessageData(
                    hash, hashToSign, CryptoUtils.decompressPublicKey(key!!),
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
        fun getBnbResultString(publicKey: String, signature: String): String {
            return "{\"signature\":\"$signature\",\"publicKey\":\"$publicKey\"}"
        }
    }
}
