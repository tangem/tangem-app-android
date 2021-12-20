package com.tangem.tap.domain.walletconnect

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumGasLoader
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.Companion.toKeccak
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.*
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils
import com.tangem.operations.sign.SignHashCommand
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.features.details.redux.walletconnect.*
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PersonalSignDialogData
import com.tangem.tap.features.details.ui.walletconnect.dialogs.TransactionRequestDialogData
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import com.tangem.tap.tangemSdkManager
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
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
        val factory = store.state.globalState.tapWalletManager.walletManagerFactory
        val publicKey = Wallet.PublicKey(
            session.wallet.walletPublicKey ?: return null,
            session.wallet.derivedPublicKey,
            session.wallet.derivationPath,
        )
        val blockchain = session.wallet.getBlockchainForSession()
        val walletManager = factory.makeWalletManager(
                session.wallet.cardId,
                blockchain,
                publicKey
            ) ?: return null

        try {
            walletManager.update()
        } catch (exception: Exception) {
            Timber.e(exception)
            return null
        }

        val balance =
            walletManager.wallet.amounts[AmountType.Coin]?.value ?: return null

        val gas = transaction.gas?.hexToBigDecimal()
            ?: transaction.gasLimit?.hexToBigDecimal() ?: return null

        val decimals = blockchain.decimals()

        val value = (transaction.value ?: "0").hexToBigDecimal()
            ?.movePointLeft(decimals) ?: return null

        val gasPrice = transaction.gasPrice?.hexToBigDecimal()
            ?: when (val result =
                (walletManager as? EthereumGasLoader)?.getGasPrice()) {
                is Result.Success -> result.data.toBigDecimal()
                is Result.Failure -> {
                    Timber.e(result.error)
                    return null
                }
                null -> return null
            }

        val fee = (gas * gasPrice).movePointLeft(decimals)
        val total = value + fee

        val transactionData = TransactionData(
            amount = Amount(value, blockchain),
            fee = Amount(fee, blockchain),
            sourceAddress = transaction.from,
            destinationAddress = transaction.to!!,
            extras = EthereumTransactionExtras(
                data = transaction.data.removePrefix(HEX_PREFIX).hexToBytes(),
                gasLimit = gas.toBigInteger(),
                nonce = transaction.nonce?.hexToBigDecimal()?.toBigInteger()
            )
        )
        val dialogData = TransactionRequestDialogData(
            cardId = session.wallet.cardId,
            dAppName = session.peerMeta.name,
            dAppUrl = session.peerMeta.url,
            amount = value.toFormattedString(decimals),
            gasAmount = fee.toFormattedString(decimals),
            totalAmount = total.toFormattedString(decimals),
            balance = balance.toFormattedString(decimals),
            isEnoughFundsToSend = (balance - total) >= BigDecimal.ZERO,
            session = session.session,
            id = id,
            type = type
        )
        return WcTransactionData(
            type = type,
            transaction = transactionData,
            session = session,
            id = id,
            walletManager = walletManager,
            dialogData = dialogData
        )
    }

    suspend fun completeTransaction(data: WcTransactionData): String? {
        return when (data.type) {
            WcTransactionType.EthSendTransaction -> sendTransaction(data)
            WcTransactionType.EthSignTransaction -> signTransaction(data)
        }
    }

    private suspend fun sendTransaction(data: WcTransactionData): String? {
        val result = (data.walletManager as TransactionSender).send(
            transactionData = data.transaction,
            signer = Signer(tangemSdk)
        )
        return when (result) {
            SimpleResult.Success -> {
                HEX_PREFIX + data.walletManager.wallet.recentTransactions.last().hash
            }
            is SimpleResult.Failure -> {
                (result.error as? TangemSdkError)?.let { error ->
                    FirebaseAnalyticsHandler.logCardSdkError(
                        error,
                        FirebaseAnalyticsHandler.ActionToLog.WalletConnectTransaction,
                    )
                }
                Timber.e(result.error)
                null
            }
        }
    }

    private suspend fun signTransaction(data: WcTransactionData): String? {
        val dataToSign = EthereumUtils.buildTransactionToSign(
            transactionData = data.transaction,
            nonce = null,
            blockchain = data.walletManager.wallet.blockchain,
            gasLimit = null
        ) ?: return null

        val command = SignHashCommand(
            hash = dataToSign.hash,
            walletPublicKey = data.walletManager.wallet.publicKey.seedKey,
            derivationPath = data.walletManager.wallet.publicKey.derivationPath
        )
        val result = tangemSdkManager.runTaskAsync(command, initialMessage = Message())
        return when (result) {
            is CompletionResult.Success -> {
                HEX_PREFIX + result.data
            }
            is CompletionResult.Failure -> {
                (result.error as? TangemSdkError)?.let { error ->
                    FirebaseAnalyticsHandler.logCardSdkError(
                        error,
                        FirebaseAnalyticsHandler.ActionToLog.WalletConnectSign,
                    )
                }
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
        val messageData = message.data.removePrefix(HEX_PREFIX).hexToBytes()
        val messageString = message.data.hexToAscii() ?: message.data

        val prefixData = (ETH_MESSAGE_PREFIX + messageData.size.toString()).toByteArray()
        val hashToSign = (prefixData + messageData).toKeccak()


        val dialogData = PersonalSignDialogData(
            cardId = session.wallet.cardId,
            dAppName = session.peerMeta.name,
            message = messageString,
            session = session.session,
            id = id
        )
        return WcPersonalSignData(
            hash = hashToSign,
            session = session,
            id = id,
            dialogData = dialogData
        )
    }

    private fun String.hexToAscii(): String? {
        return removePrefix(HEX_PREFIX).hexToBytes()
            .map {
                val char = it.toInt().toChar()
                if (char.isAscii()) char else return null
            }
            .joinToString("")
    }

    suspend fun signPersonalMessage(hashToSign: ByteArray, wallet: WalletForSession): String? {
        val key = wallet.derivedPublicKey ?: wallet.walletPublicKey
        val command = SignHashCommand(hashToSign, wallet.walletPublicKey!!, wallet.derivationPath)
        return when (val result = tangemSdkManager.runTaskAsync(command, wallet.cardId)) {
            is CompletionResult.Success -> {
                val hash = result.data.signature
                return EthereumUtils.prepareSignedMessageData(
                    hash, hashToSign, CryptoUtils.decompressPublicKey(key!!)
                )
            }
            is CompletionResult.Failure -> {
                (result.error as? TangemSdkError)?.let { error ->
                    FirebaseAnalyticsHandler.logCardSdkError(
                        error,
                        FirebaseAnalyticsHandler.ActionToLog.WalletConnectSign,
                    )
                }
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    companion object {
        private const val ETH_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n"
        private const val HEX_PREFIX = "0x"
    }
}