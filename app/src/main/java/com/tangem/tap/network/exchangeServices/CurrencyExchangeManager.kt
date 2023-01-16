package com.tangem.tap.network.exchangeServices

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.common.CardDTO
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import java.math.BigDecimal

/**
 * Created by Anton Zhilenkov on 02/03/2022.
 */
class CurrencyExchangeManager(
    private val buyService: ExchangeService,
    private val sellService: ExchangeService,
    private val primaryRules: ExchangeRules,
) : ExchangeService, ExchangeUrlBuilder {

    override fun featureIsSwitchedOn(): Boolean = primaryRules.featureIsSwitchedOn()

    override suspend fun update() {
        buyService.update()
        sellService.update()
    }

    override fun isBuyAllowed(): Boolean = primaryRules.isBuyAllowed() && buyService.isBuyAllowed()
    override fun isSellAllowed(): Boolean = primaryRules.isSellAllowed() && sellService.isSellAllowed()

    override fun availableForBuy(currency: Currency): Boolean {
        return primaryRules.availableForBuy(currency) && buyService.availableForBuy(currency)
    }

    override fun availableForSell(currency: Currency): Boolean {
        return primaryRules.availableForSell(currency) && sellService.availableForSell(currency)
    }

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
            walletAddress,
        )
    }

    override fun getSellCryptoReceiptUrl(action: Action, transactionId: String): String? {
        val urlBuilder = getExchangeUrlBuilder(action)
        return urlBuilder.getSellCryptoReceiptUrl(action, transactionId)
    }

    private fun getExchangeUrlBuilder(action: Action): ExchangeUrlBuilder {
        return when (action) {
            Action.Buy -> buyService
            Action.Sell -> sellService
        } as ExchangeUrlBuilder
    }

    enum class Action { Buy, Sell }

    companion object {
        fun dummy(): CurrencyExchangeManager = CurrencyExchangeManager(
            buyService = ExchangeService.dummy(),
            sellService = ExchangeService.dummy(),
            primaryRules = ExchangeRules.dummy(),
        )
    }
}

suspend fun CurrencyExchangeManager.buyErc20TestnetTokens(
    card: CardDTO,
    walletManager: EthereumWalletManager,
    token: Token,
) {
    walletManager.safeUpdate()

    val amountToSend = Amount(walletManager.wallet.blockchain)
    val destinationAddress = token.contractAddress

    val feeResult =
        walletManager.getFee(
            amountToSend,
            destinationAddress,
        ) as? Result.Success ?: return
    val fee = feeResult.data[0]

    val coinValue = walletManager.wallet.amounts[AmountType.Coin]?.value ?: BigDecimal.ZERO
    if (coinValue < fee.value) return

    val transaction = walletManager.createTransaction(amountToSend, fee, destinationAddress)

    val signer = TangemSigner(
        card = card,
        tangemSdk = tangemSdk,
        initialMessage = Message(),
    ) { signResponse ->
        store.dispatch(
            GlobalAction.UpdateWalletSignedHashes(
                walletSignedHashes = signResponse.totalSignedHashes,
                walletPublicKey = walletManager.wallet.publicKey.seedKey,
                remainingSignatures = signResponse.remainingSignatures,
            ),
        )
    }
    walletManager.send(transaction, signer)
}
