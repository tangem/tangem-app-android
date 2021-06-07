package com.tangem.tap.domain.walletconnect

import com.google.common.base.CharMatcher
import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumGasLoader
import com.tangem.blockchain.blockchains.ethereum.EthereumHelper
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.Signer
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.commands.SignCommand
import com.tangem.commands.wallet.WalletIndex
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.features.details.redux.walletconnect.*
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PersonalSignDialogData
import com.tangem.tap.features.details.ui.walletconnect.dialogs.TransactionRequestDialogData
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import com.tangem.tap.tangemSdkManager
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import org.kethereum.crypto.api.ec.ECDSASignature
import org.kethereum.crypto.determineRecId
import org.kethereum.crypto.impl.ec.canonicalise
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.PublicKey
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

class WalletConnectSdkHelper {

    suspend fun prepareTransactionData(
        transaction: WCEthereumTransaction,
        session: WalletConnectSession,
        id: Long,
        type: WcTransactionType,
    ): WcTransactionData? {
        val factory = store.state.globalState.tapWalletManager.walletManagerFactory
        val walletManager = factory.makeEthereumWalletManager(
            session.wallet.cardId,
            session.wallet.walletPublicKey.hexToBytes(),
            emptyList(),
            isTestNet = session.wallet.isTestNet
        ) ?: return null


        try {
            walletManager.update()
        } catch (exception: Exception) {
            Timber.e(exception)
            return null
        }

        val blockchain = walletManager.wallet.blockchain

        val balance =
            walletManager.wallet.amounts[AmountType.Coin]?.value ?: return null

        val gas = transaction.gas?.hexToBigDecimal()
            ?: transaction.gasLimit?.hexToBigDecimal() ?: return null

        val decimals = blockchain.decimals()

        val value = transaction.value?.hexToBigDecimal()
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
                Timber.e(result.error)
                null
            }
        }
    }

    private suspend fun signTransaction(data: WcTransactionData): String? {
        val dataToSign = EthereumHelper.buildTransactionToSign(
            transactionData = data.transaction,
            nonce = null,
            blockchain = data.walletManager.wallet.blockchain,
            gasLimit = null
        ) ?: return null

        val command = SignCommand(
            hashes = arrayOf(dataToSign.hash),
            walletIndex = WalletIndex.PublicKey(data.walletManager.wallet.publicKey)
        )
        val result = tangemSdkManager.runTaskAsync(command, initialMessage = Message())
        return when (result) {
            is CompletionResult.Success -> {
                HEX_PREFIX + result.data
            }
            is CompletionResult.Failure -> {
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
        val hashToSign = (prefixData + messageData).keccak()


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

    private fun Char.isAscii(): Boolean = CharMatcher.ascii().matches(this)

    suspend fun signPersonalMessage(hashToSign: ByteArray, wallet: WalletForSession): String? {
        val publicKey = wallet.walletPublicKey.hexToBytes()
        val command = SignCommand(
            arrayOf(hashToSign),
            WalletIndex.PublicKey(publicKey))
        return when (val result = tangemSdkManager.runTaskAsync(command, wallet.cardId)) {
            is CompletionResult.Success -> {
                val hash = result.data.signatures.first()

                val r = BigInteger(1, hash.copyOfRange(0, 32))
                val s = BigInteger(1, hash.copyOfRange(32, 64))

                val ecdsaSignature = ECDSASignature(r, s).canonicalise()

                val recId = ecdsaSignature.determineRecId(hashToSign,
                    PublicKey(publicKey.sliceArray(1..64)))
                val v = (recId + 27).toBigInteger()

                return HEX_PREFIX + ecdsaSignature.r.toString(16) + ecdsaSignature.s.toString(16) +
                        v.toString(16)
            }
            is CompletionResult.Failure -> {
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