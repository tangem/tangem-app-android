package com.tangem.tap.network.exchangeServices

import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.exchangeservice.swap.SwapServiceLoader
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultRampManager(
    private val exchangeService: ExchangeService?,
    private val buyService: Provider<ExchangeService>,
    private val sellService: Provider<ExchangeService>,
    private val swapServiceLoader: SwapServiceLoader,
    private val dispatchers: CoroutineDispatcherProvider,
) : RampStateManager {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter()

    override fun availableForBuy(scanResponse: ScanResponse, cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForBuy(
            scanResponse = scanResponse,
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override fun availableForSell(cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForSell(
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override suspend fun availableForSwap(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return getExchangeableFlag(userWalletId, cryptoCurrency) && !cryptoCurrency.isCustom
    }

    override fun getBuyInitializationStatus(): Flow<ExchangeServiceInitializationStatus> {
        return buyService.invoke().initializationStatus
    }

    override suspend fun fetchBuyServiceData() {
        withContext(dispatchers.io) {
            buyService.invoke().update()
        }
    }

    override fun getSellInitializationStatus(): Flow<ExchangeServiceInitializationStatus> {
        return sellService.invoke().initializationStatus
    }

    override suspend fun fetchSellServiceData() {
        withContext(dispatchers.io) {
            sellService.invoke().update()
        }
    }

    override fun getSwapInitializationStatus(userWalletId: UserWalletId): Flow<ExchangeServiceInitializationStatus> {
        return swapServiceLoader.getInitializationStatus(userWalletId)
    }

    private suspend fun getExchangeableFlag(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return withContext(dispatchers.io) {
            val contractAddress = (cryptoCurrency as? CryptoCurrency.Token)?.contractAddress
                ?: EMPTY_CONTRACT_ADDRESS_VALUE

            val asset = swapServiceLoader.getInitializationStatus(userWalletId).value.getOrNull()?.find {
                it.network == cryptoCurrency.network.backendId &&
                    it.contractAddress.equals(contractAddress, ignoreCase = true)
            }

            asset?.exchangeAvailable ?: false
        }
    }
}
