package com.tangem.tap.proxy

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
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
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import java.math.BigDecimal

class TransactionManagerImpl(
    private val appStateHolder: AppStateHolder,
) : TransactionManager {

    override suspend fun sendTransaction(
        networkId: String,
        amountToSend: BigDecimal,
        currencyToSend: Currency,
        feeAmount: BigDecimal,
        estimatedGas: Int,
        destinationAddress: String,
        dataToSign: String,
    ): SendTxResult {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain)
        val amount = createAmount(amountToSend, currencyToSend, blockchain)

        val txData = walletManager.createTransaction(
            amount = amount,
            fee = Amount(value = feeAmount, blockchain = blockchain),
            destination = destinationAddress,
        ).copy(hash = dataToSign, extras = createExtras(walletManager, estimatedGas, dataToSign))

        val signer = transactionSigner(walletManager)

        val sendResult = try {
            (walletManager as TransactionSender).send(txData, signer)
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            return SendTxResult.UnknownError(ex)
        }
        return handleSendResult(sendResult)
    }

    override fun getNativeTokenDecimals(networkId: String): Int {
        return Blockchain.fromNetworkId(networkId)?.decimals() ?: error("blockchain not found")
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
    ): ProxyAmount {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain)
        val fee = (walletManager as TransactionSender).getFee(
            amount = createAmount(amountToSend, currencyToSend, blockchain),
            destination = destinationAddress,
        )
        when (fee) {
            is Result.Success -> {
                return convertToProxyAmount(fee.data.firstOrNull() ?: error("no fee found"))
            }
            is Result.Failure -> {
                error(fee.error.message ?: fee.error.customMessage)
            }
        }
    }

    private fun handleSendResult(result: SimpleResult): SendTxResult {
        when (result) {
            is SimpleResult.Success -> return SendTxResult.Success
            is SimpleResult.Failure -> {
                val error = result.error as? BlockchainSdkError ?: return SendTxResult.UnknownError()
                when (error) {
                    is BlockchainSdkError.WrappedTangemError -> {
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

    private fun transactionSigner(walletManager: WalletManager): TransactionSigner {
        val actualCard = requireNotNull(appStateHolder.getActualCard()) { "no card found" }
        return TangemSigner(
            card = actualCard,
            tangemSdk = tangemSdk,
            initialMessage = Message("test transaction title", "test description"),
        ) { signResponse ->
            store.dispatch(
                GlobalAction.UpdateWalletSignedHashes(
                    walletSignedHashes = signResponse.totalSignedHashes,
                    walletPublicKey = walletManager.wallet.publicKey.seedKey,
                    remainingSignatures = signResponse.remainingSignatures,
                ),
            )
        }
    }

    private fun getActualWalletManager(blockchain: Blockchain): WalletManager {
        val card = appStateHolder.getActualCard()
        if (card != null) {
            val blockchainNetwork = BlockchainNetwork(blockchain, card)
            val walletManager = appStateHolder.walletState?.getWalletManager(blockchainNetwork)
            if (walletManager != null) {
                return walletManager
            } else {
                error("no wallet manager found")
            }
        } else {
            error("card not found")
        }
    }

    private fun createExtras(
        walletManager: WalletManager,
        estimatedGas: Int,
        transactionHash: String,
    ): TransactionExtras? {
        return when (walletManager) {
            is EthereumWalletManager -> {
                return EthereumTransactionExtras(
                    data = transactionHash.removePrefix(HEX_PREFIX).hexToBytes(),
                    gasLimit = estimatedGas.toBigInteger(),
                )
            }
            else -> {
                null
            }
        }
    }

    private fun createAmount(
        amount: BigDecimal,
        currency: Currency,
        blockchain: Blockchain,
    ): Amount {
        return when (currency) {
            is Currency.NativeToken -> {
                Amount(value = amount, blockchain = blockchain)
            }
            is Currency.NonNativeToken -> {
                Amount(convertNonNativeToken(currency), amount)
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
    }
}
