package com.tangem.tap.network.exchangeServices

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
interface ExchangeService {
    suspend fun isBuyAllowed(): Boolean
    suspend fun availableToBuy(): List<String>
    suspend fun isSellAllowed(): Boolean
    suspend fun availableToSell(): List<String>
}

interface ExchangeUrlBuilder {
    fun getUrl(
        action: CurrencyExchangeManager.Action,
        blockchain: Blockchain,
        cryptoCurrencyName: CryptoCurrencyName,
        fiatCurrencyName: String,
        walletAddress: String,
    ): String?

    fun getSellCryptoReceiptUrl(action: CurrencyExchangeManager.Action, transactionId: String): String?

    companion object {
        const val SCHEME = "https"
        const val URL_SELL = "sell.moonpay.com"
        const val SUCCESS_URL = "tangem://success.tangem.com"
    }
}

class CurrencyExchangeManager(
    private val onramperService: ExchangeService,
    private val moonPayService: ExchangeService,
) : ExchangeService, ExchangeUrlBuilder {

    var status: CurrencyExchangeStatus? = null
        private set

    suspend fun getStatus(): CurrencyExchangeStatus {
        val isBuyAllowed = isBuyAllowed()
        val isSellAllowed = isSellAllowed()
        val availableToBuy = availableToBuy()
        val availableToSell = availableToSell()
        status = CurrencyExchangeStatus(
            isBuyAllowed,
            isSellAllowed,
            availableToBuy,
            availableToSell,
        )
        return status!!
    }

    override suspend fun isBuyAllowed(): Boolean = onramperService.isBuyAllowed()
    override suspend fun availableToBuy(): List<String> = onramperService.availableToBuy()
    override suspend fun isSellAllowed(): Boolean = moonPayService.isSellAllowed()
    override suspend fun availableToSell(): List<String> = moonPayService.availableToSell()

    override fun getUrl(
        action: Action,
        blockchain: Blockchain,
        cryptoCurrencyName: CryptoCurrencyName,
        fiatCurrencyName: String,
        walletAddress: String,
    ): String? {
        if (blockchain.isTestnet()) return blockchain.getTestnetTopUpUrl()

        val urlBuilder = getExchangeUrlBuilder(action)
        return urlBuilder.getUrl(
            action,
            blockchain,
            cryptoCurrencyName,
            fiatCurrencyName,
            walletAddress
        )
    }

    override fun getSellCryptoReceiptUrl(action: Action, transactionId: String): String? {
        val urlBuilder = getExchangeUrlBuilder(action)
        return urlBuilder.getSellCryptoReceiptUrl(action, transactionId)
    }

    private fun getExchangeUrlBuilder(action: Action): ExchangeUrlBuilder {
        return when (action) {
            Action.Buy -> onramperService
            Action.Sell -> moonPayService
        } as ExchangeUrlBuilder
    }

    enum class Action { Buy, Sell }
}

data class CurrencyExchangeStatus(
    val isBuyAllowed: Boolean,
    val isSellAllowed: Boolean,
    val availableToBuy: List<String>,
    val availableToSell: List<String>,
)

suspend fun CurrencyExchangeManager.buyErc20Tokens(walletManager: EthereumWalletManager, token: Token) {
    walletManager.safeUpdate()

    val amountToSend = Amount(walletManager.wallet.blockchain)
    val destinationAddress = token.contractAddress

    val feeResult = walletManager.getFee(amountToSend,
        destinationAddress) as? Result.Success ?: return
    val fee = feeResult.data[0]

    if ((walletManager.wallet.amounts[AmountType.Coin]?.value ?: BigDecimal.ZERO) < fee.value) {
        return
    }

    val transaction = walletManager.createTransaction(amountToSend, fee, destinationAddress)

    val signer = TangemSigner(
        tangemSdk = tangemSdk, Message()
    ) { signResponse ->
        store.dispatch(
            GlobalAction.UpdateWalletSignedHashes(
                walletSignedHashes = signResponse.totalSignedHashes,
                walletPublicKey = walletManager.wallet.publicKey.seedKey,
                remainingSignatures = signResponse.remainingSignatures
            )
        )
    }
    walletManager.send(transaction, signer)
}