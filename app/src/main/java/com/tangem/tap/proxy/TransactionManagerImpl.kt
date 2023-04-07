package com.tangem.tap.proxy

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.optimism.OptimismWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.TransactionExtras
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.isNetworkError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.ProxyFee
import com.tangem.lib.crypto.models.ProxyNetworkInfo
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.tangemSdk
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

@Suppress("LargeClass")
class TransactionManagerImpl(
    private val appStateHolder: AppStateHolder,
    private val analytics: AnalyticsEventHandler,
) : TransactionManager {

    override suspend fun sendApproveTransaction(
        networkId: String,
        feeAmount: BigDecimal,
        gasLimit: Int,
        destinationAddress: String,
        dataToSign: String,
        derivationPath: String?,
    ): SendTxResult {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        walletManager.update()
        val amount = Amount(value = BigDecimal.ZERO, blockchain = blockchain)
        return sendTransactionInternal(
            walletManager = walletManager,
            amount = amount,
            blockchain = blockchain,
            feeAmount = feeAmount,
            gasLimit = gasLimit,
            destinationAddress = destinationAddress,
            dataToSign = dataToSign,
        )
    }

    override suspend fun sendTransaction(
        networkId: String,
        amountToSend: BigDecimal,
        feeAmount: BigDecimal,
        gasLimit: Int,
        destinationAddress: String,
        dataToSign: String,
        isSwap: Boolean,
        currencyToSend: Currency,
        derivationPath: String?,
    ): SendTxResult {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        walletManager.update()
        val amount = if (isSwap) {
            createAmountForSwap(amountToSend, currencyToSend, blockchain)
        } else {
            createAmount(amountToSend, currencyToSend, blockchain)
        }
        return sendTransactionInternal(
            walletManager = walletManager,
            amount = amount,
            blockchain = blockchain,
            feeAmount = feeAmount,
            gasLimit = gasLimit,
            destinationAddress = destinationAddress,
            dataToSign = dataToSign,
        )
    }

    @Suppress("LongParameterList")
    private suspend fun sendTransactionInternal(
        walletManager: WalletManager,
        amount: Amount,
        blockchain: Blockchain,
        feeAmount: BigDecimal,
        gasLimit: Int,
        destinationAddress: String,
        dataToSign: String,
    ): SendTxResult {
        val txData = walletManager.createTransaction(
            amount = amount,
            fee = Amount(value = feeAmount, blockchain = blockchain),
            destination = destinationAddress,
        ).copy(hash = dataToSign, extras = createExtras(walletManager, gasLimit, dataToSign))

        val signer = transactionSigner(walletManager)

        val sendResult = try {
            (walletManager as? TransactionSender)?.send(txData, signer) ?: error("Cannot cast to TransactionSender")
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            return SendTxResult.UnknownError(ex)
        }
        return handleSendResult(sendResult)
    }

    override fun getExplorerTransactionLink(networkId: String, txAddress: String): String {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: error("blockchain not found")
        return blockchain.getExploreTxUrl(txAddress)
    }

    override fun getNativeTokenDecimals(networkId: String): Int {
        return Blockchain.fromNetworkId(networkId)?.decimals() ?: error("blockchain not found")
    }

    override suspend fun updateWalletManager(networkId: String, derivationPath: String?) {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        getActualWalletManager(blockchain, derivationPath).update()
    }

    override fun calculateFee(networkId: String, gasPrice: String, estimatedGas: Int): BigDecimal {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val gasPriceValue = requireNotNull(gasPrice.toLongOrNull()) { "gasprice should be Long" }
        return (gasPriceValue * estimatedGas).toBigDecimal().movePointLeft(blockchain.decimals())
    }

    @Throws(IllegalStateException::class)
    override suspend fun getFee(
        networkId: String,
        amountToSend: BigDecimal,
        currencyToSend: Currency,
        destinationAddress: String,
        increaseBy: Int?,
        data: String?,
        derivationPath: String?,
    ): ProxyFee {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        if (walletManager is EthereumWalletManager) {
            if (walletManager is OptimismWalletManager) {
                return getFeeForOptimismBlockchain(
                    walletManager = walletManager,
                    amount = createAmount(amountToSend, currencyToSend, blockchain),
                    destinationAddress = destinationAddress,
                    data = data,
                )
            }
            return getFeeForEthereumBlockchain(
                walletManager = walletManager,
                blockchain = blockchain,
                amountToSend = amountToSend,
                currency = currencyToSend,
                destinationAddress = destinationAddress,
                data = data,
                increaseBy = increaseBy,
            )
        } else {
            return getFeeForBlockchain(
                walletManager = walletManager,
                amountToSend = amountToSend,
                currency = currencyToSend,
                blockchain = blockchain,
                destinationAddress = destinationAddress,
            )
        }
    }

    override fun getBlockchainInfo(networkId: String): ProxyNetworkInfo {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        return ProxyNetworkInfo(
            name = blockchain.fullName,
            blockchainId = blockchain.id,
            blockchainCurrency = blockchain.currency,
        )
    }

    private suspend fun getFeeForBlockchain(
        walletManager: WalletManager,
        amountToSend: BigDecimal,
        currency: Currency,
        blockchain: Blockchain,
        destinationAddress: String,
    ): ProxyFee {
        val fee = (walletManager as? TransactionSender)?.getFee(
            amount = createAmount(amountToSend, currency, blockchain),
            destination = destinationAddress,
        ) ?: error("Cannot cast to TransactionSender")
        return when (fee) {
            is Result.Success -> {
                // for not EVM blockchains set gasLimit ZERO for now
                ProxyFee(
                    gasLimit = BigInteger.ZERO,
                    fee = convertToProxyAmount(fee.data.firstOrNull() ?: error("no fee found")),
                )
            }
            is Result.Failure -> {
                error(fee.error.message ?: fee.error.customMessage)
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun getFeeForEthereumBlockchain(
        walletManager: EthereumWalletManager,
        blockchain: Blockchain,
        amountToSend: BigDecimal,
        currency: Currency,
        destinationAddress: String,
        data: String?,
        increaseBy: Int?,
    ): ProxyFee {
        val gasLimit = getGasLimit(
            evmWalletManager = walletManager,
            blockchain = blockchain,
            amount = amountToSend,
            currency = currency,
            destinationAddress = destinationAddress,
            data = data,
        ).let {
            if (increaseBy != null && increaseBy != 0) {
                it.multiply(increaseBy.toBigInteger()).divide(BigInteger("100"))
            } else {
                it
            }
        }
        return when (val gasPrice = walletManager.getGasPrice()) {
            is Result.Success -> {
                val fee = gasLimit.multiply(gasPrice.data).toBigDecimal(
                    scale = blockchain.decimals(),
                    mathContext = MathContext(blockchain.decimals(), RoundingMode.HALF_EVEN),
                )
                ProxyFee(
                    gasLimit = gasLimit,
                    fee = ProxyAmount(
                        currencySymbol = blockchain.currency,
                        value = fee,
                        decimals = blockchain.decimals(),
                    ),
                )
            }
            is Result.Failure -> {
                error(gasPrice.error.message ?: gasPrice.error.customMessage)
            }
        }
    }

    private suspend fun getFeeForOptimismBlockchain(
        walletManager: OptimismWalletManager,
        amount: Amount,
        destinationAddress: String,
        data: String?,
    ): ProxyFee {
        val fee = if (data.isNullOrEmpty()) {
            walletManager.getFee(amount, destinationAddress)
        } else {
            walletManager.getFee(amount, destinationAddress, data)
        }
        when (fee) {
            is Result.Success -> {
                return ProxyFee(
                    gasLimit = walletManager.gasLimit ?: BigInteger.ZERO,
                    fee = convertToProxyAmount(fee.data.lastOrNull() ?: error("no fee found")),
                )
            }
            is Result.Failure -> {
                error(fee.error.message ?: fee.error.customMessage)
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun getGasLimit(
        evmWalletManager: EthereumWalletManager,
        blockchain: Blockchain,
        amount: BigDecimal,
        currency: Currency,
        destinationAddress: String,
        data: String?,
    ): BigInteger {
        val result = if (data.isNullOrEmpty()) {
            evmWalletManager.getGasLimit(
                amount = createAmount(amount, currency, blockchain),
                destination = destinationAddress,
            )
        } else {
            evmWalletManager.getGasLimit(
                amount = createAmount(amount, currency, blockchain),
                destination = destinationAddress,
                data = data,
            )
        }
        when (result) {
            is Result.Success -> {
                return result.data
            }
            is Result.Failure -> {
                error(result.error.message ?: result.error.customMessage)
            }
        }
    }

    private fun handleSendResult(result: SimpleResult): SendTxResult {
        when (result) {
            is SimpleResult.Success -> {
                analytics.send(Basic.TransactionSent(AnalyticsParam.TxSentFrom.Swap))
                return SendTxResult.Success
            }
            is SimpleResult.Failure -> {
                if (result.isNetworkError()) return SendTxResult.NetworkError(result.error)
                val error = result.error as? BlockchainSdkError ?: return SendTxResult.UnknownError()
                when (error) {
                    is BlockchainSdkError.WrappedTangemError -> {
                        val errorByCode = mapErrorByCode(error)
                        if (errorByCode != null) {
                            return errorByCode
                        }
                        val tangemSdkError = error.tangemError as? TangemSdkError ?: return SendTxResult.UnknownError()
                        if (tangemSdkError is TangemSdkError.UserCancelled) return SendTxResult.UserCancelledError
                        return SendTxResult.TangemSdkError(tangemSdkError.code, tangemSdkError.cause)
                    }
                    else -> {
                        return SendTxResult.TangemSdkError(error.code, error.cause)
                    }
                }
            }
        }
    }

    private fun mapErrorByCode(error: BlockchainSdkError.WrappedTangemError): SendTxResult? {
        return when (error.code) {
            USER_CANCELLED_ERROR_CODE -> {
                return SendTxResult.UserCancelledError
            }
            else -> {
                null
            }
        }
    }

    private fun transactionSigner(walletManager: WalletManager): TransactionSigner {
        val actualCard = requireNotNull(appStateHolder.getActualCard()) { "no card found" }
        return TangemSigner(
            card = actualCard,
            tangemSdk = tangemSdk,
            initialMessage = Message(),
        ) { signResponse ->
            appStateHolder.mainStore?.dispatch(
                GlobalAction.UpdateWalletSignedHashes(
                    walletSignedHashes = signResponse.totalSignedHashes,
                    walletPublicKey = walletManager.wallet.publicKey.seedKey,
                    remainingSignatures = signResponse.remainingSignatures,
                ),
            )
        }
    }

    private fun getActualWalletManager(blockchain: Blockchain, derivationPath: String?): WalletManager {
        val blockchainNetwork = BlockchainNetwork(blockchain, derivationPath, emptyList())
        val walletManager = appStateHolder.walletState?.getWalletManager(blockchainNetwork)
        return requireNotNull(walletManager) { "no wallet manager found" }
    }

    private fun createExtras(
        walletManager: WalletManager,
        gasLimit: Int,
        transactionHash: String,
    ): TransactionExtras? {
        return when (walletManager) {
            is EthereumWalletManager -> {
                return EthereumTransactionExtras(
                    data = transactionHash.removePrefix(HEX_PREFIX).hexToBytes(),
                    gasLimit = gasLimit.toBigInteger(),
                )
            }
            else -> {
                null
            }
        }
    }

    private fun createAmount(amount: BigDecimal, currency: Currency, blockchain: Blockchain): Amount {
        return when (currency) {
            is Currency.NativeToken -> {
                Amount(value = amount, blockchain = blockchain)
            }
            is Currency.NonNativeToken -> {
                Amount(convertNonNativeToken(currency), amount)
            }
        }
    }

    private fun createAmountForSwap(amount: BigDecimal, currency: Currency?, blockchain: Blockchain): Amount {
        return when (currency) {
            is Currency.NativeToken,
            null,
            -> {
                Amount(value = amount, blockchain = blockchain)
            }
            is Currency.NonNativeToken -> {
                // 1. when creates swap amount for NonNativeToken, amount should be ZERO
                // 2. Amount has .Coin type, as workaround to use destinationAddress in bsdk, not contractAddress
                Amount(
                    currencySymbol = currency.symbol,
                    value = BigDecimal.ZERO,
                    decimals = currency.decimalCount,
                )
            }
        }
    }

    private fun convertNonNativeToken(token: Currency.NonNativeToken): Token {
        return Token(
            name = token.name,
            symbol = token.symbol,
            contractAddress = token.contractAddress,
            decimals = token.decimalCount,
            id = token.id,
        )
    }

    private fun convertToProxyAmount(amount: Amount): ProxyAmount {
        return ProxyAmount(
            currencySymbol = amount.currencySymbol,
            value = amount.value ?: BigDecimal.ZERO,
            decimals = amount.decimals,
        )
    }

    companion object {
        private const val HEX_PREFIX = "0x"
        private const val USER_CANCELLED_ERROR_CODE = 50002
    }
}
