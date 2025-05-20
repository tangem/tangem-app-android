package com.tangem.tap.network.exchangeServices

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.features.demo.isDemoCard
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class CurrencyExchangeManager(
    private val buyService: ExchangeService,
    private val sellService: ExchangeService,
    private val primaryRules: ExchangeRules,
) : ExchangeService {

    override val initializationStatus: StateFlow<ExchangeServiceInitializationStatus>
        get() = _initializationStatus

    private val _initializationStatus: MutableStateFlow<ExchangeServiceInitializationStatus> =
        MutableStateFlow(value = lceLoading())

    override suspend fun update() {
        _initializationStatus.value = lceLoading()

        if (!store.inject(DaggerGraphState::onrampFeatureToggles).isFeatureEnabled) {
            buyService.update()
        }

        sellService.update()

        _initializationStatus.value = lceContent()
    }

    override fun availableForBuy(scanResponse: ScanResponse, currency: Currency): Boolean {
        return primaryRules.availableForBuy(scanResponse, currency) &&
            buyService.availableForBuy(scanResponse, currency)
    }

    override fun availableForSell(currency: Currency): Boolean {
        return primaryRules.availableForSell(currency) && sellService.availableForSell(currency)
    }

    override fun getUrl(
        action: Action,
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyName: String,
        walletAddress: String,
        isDarkTheme: Boolean,
    ): String? {
        val blockchain = Blockchain.fromId(cryptoCurrency.network.rawId)
        if (blockchain.isTestnet()) return blockchain.getTestnetTopUpUrl()

        val urlBuilder = getExchangeUrlBuilder(action)
        return urlBuilder.getUrl(
            action,
            cryptoCurrency,
            fiatCurrencyName,
            walletAddress,
            isDarkTheme,
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
        }
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

@Deprecated("Useless")
suspend fun buyErc20TestnetTokens(card: CardDTO, walletManager: EthereumWalletManager, destinationAddress: String) {
    walletManager.safeUpdate(card.isDemoCard())

    val amountToSend = Amount(walletManager.wallet.blockchain)

    val feeResult = walletManager.getFee(amountToSend, destinationAddress) as? Result.Success ?: return
    val fee = when (val feeForTx = feeResult.data) {
        is TransactionFee.Choosable -> feeForTx.minimum
        is TransactionFee.Single -> feeForTx.normal
    }

    val coinValue = walletManager.wallet.amounts[AmountType.Coin]?.value ?: BigDecimal.ZERO
    if (coinValue < fee.amount.value) return

    val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins
    val signer = TangemSigner(
        cardId = card.cardId.takeIf { isCardNotBackedUp },
        tangemSdk = store.inject(DaggerGraphState::cardSdkConfigRepository).sdk,
        initialMessage = Message(),
        twinKey = null,
    ) { signResponse ->
        store.dispatch(
            GlobalAction.UpdateWalletSignedHashes(
                walletSignedHashes = signResponse.totalSignedHashes,
                walletPublicKey = walletManager.wallet.publicKey.seedKey,
                remainingSignatures = signResponse.remainingSignatures,
            ),
        )

        store.dispatch(action = GlobalAction.IsSignWithRing(signResponse.isRing))
    }

    walletManager.send(
        transactionData = walletManager.createTransaction(
            amount = amountToSend,
            fee = fee,
            destination = destinationAddress,
        ),
        signer = signer,
    )
}