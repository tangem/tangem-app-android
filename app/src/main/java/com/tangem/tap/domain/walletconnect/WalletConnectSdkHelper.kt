package com.tangem.tap.domain.walletconnect

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumGasLoader
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.*
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.Basic
import com.tangem.core.analytics.models.Basic.TransactionSent.MemoType
import com.tangem.operations.sign.SignHashCommand
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.extensions.toFormattedString
import com.tangem.tap.domain.walletconnect2.domain.TransactionType
import com.tangem.tap.domain.walletconnect2.domain.WcEthereumTransaction
import com.tangem.tap.domain.walletconnect2.domain.WcSignMessage
import com.tangem.tap.domain.walletconnect2.domain.models.EthTransactionData
import com.tangem.tap.domain.walletconnect2.domain.models.binance.WcBinanceTradeOrder
import com.tangem.tap.domain.walletconnect2.domain.models.binance.WcBinanceTransferOrder
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.features.details.redux.walletconnect.BinanceMessageData
import com.tangem.tap.features.details.redux.walletconnect.WcEthTransactionType
import com.tangem.tap.features.details.redux.walletconnect.WcPersonalSignData
import com.tangem.tap.features.details.redux.walletconnect.WcTransactionData
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PersonalSignDialogData
import com.tangem.tap.features.details.ui.walletconnect.dialogs.TransactionRequestDialogData
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import timber.log.Timber
import java.math.BigDecimal
import com.tangem.core.analytics.models.AnalyticsParam as CoreAnalyticsParam

@Suppress("LargeClass")
class WalletConnectSdkHelper {

    private val userWalletsListManager by lazy {
        store.inject(DaggerGraphState::generalUserWalletsListManager)
    }

    @Suppress("MagicNumber")
    suspend fun prepareTransactionData(data: EthTransactionData): WcTransactionData {
        val transaction = data.transaction
        val blockchain = requireNotNull(Blockchain.fromNetworkId(data.networkId)) {
            "Blockchain not found"
        }
        val walletManager = requireNotNull(getWalletManager(blockchain, data.rawDerivationPath)) {
            "WalletManager not found"
        }

        walletManager.safeUpdate(isDemoCard())
        val wallet = walletManager.wallet
        val balance = requireNotNull(wallet.amounts[AmountType.Coin]?.value) {
            "Coin balance not found"
        }

        val decimals = wallet.blockchain.decimals()

        val value = (transaction.value ?: "0")
            .hexToBigDecimal()
            .movePointLeft(decimals)

        requireNotNull(value) {
            "Transaction amount is null"
        }

        val gasLimit = getGasLimitFromTx(value, walletManager, transaction)

        val gasPrice = transaction.gasPrice?.hexToBigDecimal()
            ?: when (val result = (walletManager as? EthereumGasLoader)?.getGasPrice()) {
                is Result.Success -> result.data.toBigDecimal()
                is Result.Failure -> {
                    (result.error as? Throwable)?.let { Timber.e(it, "getGasPrice failed") }

                    error("Unable to get gas price: ${result.error}")
                }
                null -> error("Gas price is null")
            }

        val fee = (gasLimit * gasPrice).movePointLeft(decimals)
        val total = value + fee

        val destinationAddress = requireNotNull(transaction.to) { "Destination address is null" }

        val transactionData = TransactionData(
            amount = Amount(value, wallet.blockchain),
            // TODO refactoring
            fee = Fee.Common(Amount(fee, wallet.blockchain)),
            sourceAddress = transaction.from,
            destinationAddress = destinationAddress,
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

    fun isDemoCard(): Boolean {
        val userWallet = userWalletsListManager.selectedUserWalletSync ?: return false
        return userWallet.scanResponse.isDemoCard()
    }

    private suspend fun getWalletManager(blockchain: Blockchain, derivationPath: String?): WalletManager? {
        val userWallet = userWalletsListManager.selectedUserWalletSync ?: return null
        val walletManagerFacade = store.inject(DaggerGraphState::walletManagersFacade)
        return walletManagerFacade.getOrCreateWalletManager(
            userWalletId = userWallet.walletId,
            blockchain = blockchain,
            derivationPath = derivationPath,
        )
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
                DEFAULT_MAX_GASLIMIT.toBigDecimal() // Set high gasLimit if not provided
            }
            else -> DEFAULT_MAX_GASLIMIT.toBigDecimal() // Set high gasLimit if not provided
        }
    }

    private suspend fun sendTransaction(data: WcTransactionData, cardId: String?): String? {
        val result = (data.walletManager as TransactionSender).send(
            transactionData = data.transaction,
            signer = CommonSigner(
                tangemSdk = store.inject(DaggerGraphState::cardSdkConfigRepository).sdk,
                cardId = cardId,
            ),
        )
        return when (result) {
            is Result.Success -> {
                val sentFrom = CoreAnalyticsParam.TxSentFrom.WalletConnect
                Analytics.send(Basic.TransactionSent(sentFrom = sentFrom, memoType = MemoType.Null))
                val hash = result.data.hash
                if (hash.startsWith(HEX_PREFIX)) {
                    hash
                } else {
                    HEX_PREFIX + hash
                }
            }
            is Result.Failure -> {
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
        ) ?: return null

        val command = SignHashCommand(
            hash = dataToSign.hash,
            walletPublicKey = data.walletManager.wallet.publicKey.seedKey,
            derivationPath = data.walletManager.wallet.publicKey.derivationPath,
        )
        return when (val result = tangemSdkManager.runTaskAsync(command, initialMessage = Message(), cardId = cardId)) {
            is CompletionResult.Success -> {
                val hash = EthereumUtils.prepareTransactionToSend(
                    signature = result.data.signature,
                    transactionToSign = dataToSign,
                    walletPublicKey = data.walletManager.wallet.publicKey,
                    blockchain = data.walletManager.wallet.blockchain,
                ).toHexString()
                if (hash.startsWith(HEX_PREFIX)) {
                    hash
                } else {
                    HEX_PREFIX + hash
                }
            }
            is CompletionResult.Failure -> {
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    fun prepareBnbTradeOrder(data: WcBinanceTradeOrder): BinanceMessageData.Trade {
        return BnbHelper.createMessageData(data)
    }

    fun prepareBnbTransferOrder(data: WcBinanceTransferOrder): BinanceMessageData.Transfer {
        return BnbHelper.createMessageData(data)
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
        return when (val result = tangemSdkManager.runTaskAsync(command, initialMessage = Message(), cardId = cardId)) {
            is CompletionResult.Success -> {
                val key = wallet.publicKey.blockchainKey.toDecompressedPublicKey()
                getBnbResultString(
                    key.toHexString(),
                    result.data.signature.toHexString(),
                )
            }
            is CompletionResult.Failure -> {
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    fun prepareDataForPersonalSign(
        message: WcSignMessage,
        topic: String,
        metaName: String,
        id: Long,
    ): WcPersonalSignData {
        val messageData = when (message.type) {
            WcSignMessage.WCSignType.MESSAGE,
            WcSignMessage.WCSignType.PERSONAL_MESSAGE,
            -> createMessageData(message)
            WcSignMessage.WCSignType.TYPED_MESSAGE -> EthereumUtils.makeTypedDataHash(message.data)
            WcSignMessage.WCSignType.SOLANA_MESSAGE -> message.data.decodeBase58()
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
            hash = requireNotNull(messageData) { "Message data must not be null" },
            topic = topic,
            id = id,
            dialogData = dialogData,
            type = message.type,
        )
    }

    private fun createMessageData(message: WcSignMessage): ByteArray {
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
        type: WcSignMessage.WCSignType,
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
                val signedHash = result.data.signature

                return when (type) {
                    WcSignMessage.WCSignType.SOLANA_MESSAGE -> getSolanaResultString(signedHash)
                    else -> EthereumUtils.prepareSignedMessageData(
                        signedHash = signedHash,
                        hashToSign = hashToSign,
                        publicKey = wallet.publicKey.blockchainKey.toDecompressedPublicKey(),
                    )
                }
            }
            is CompletionResult.Failure -> {
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    suspend fun signTransaction(
        hashToSign: ByteArray,
        networkId: String,
        type: TransactionType,
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
                val signedHash = result.data.signature

                when (type) {
                    TransactionType.SOLANA_TX -> {
                        getSolanaResultString(signedHash)
                    }
                }
            }
            is CompletionResult.Failure -> {
                Timber.e(result.error.customMessage)
                null
            }
        }
    }

    private fun getSolanaResultString(signedHash: ByteArray) = "{ signature: \"${signedHash.encodeBase58()}\" }"

    private fun getBnbResultString(publicKey: String, signature: String) =
        "{\"signature\":\"$signature\",\"publicKey\":\"$publicKey\"}"

    private companion object {
        const val ETH_MESSAGE_PREFIX = "\u0019Ethereum Signed Message:\n"
        const val HEX_PREFIX = "0x"
        const val DEFAULT_MAX_GASLIMIT = 350000
    }
}