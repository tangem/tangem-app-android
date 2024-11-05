package com.tangem.tap.network.exchangeServices

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.Provider
import kotlinx.coroutines.flow.Flow

class DefaultRampManager(
    private val exchangeService: ExchangeService?,
    private val buyService: Provider<ExchangeService>,
    private val sellService: Provider<ExchangeService>,
    private val marketsCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
) : RampStateManager {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter()

    override fun availableForBuy(scanResponse: ScanResponse, cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForBuy(
            scanResponse,
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override fun availableForSell(cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForSell(
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override suspend fun availableForSwap(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return marketsCryptoCurrencyRepository.isExchangeable(userWalletId, cryptoCurrency)
    }

    override fun getBuyInitializationStatus(): Flow<ExchangeServiceInitializationStatus> {
        return buyService.invoke().initializationStatus
    }

    override fun getSellInitializationStatus(): Flow<ExchangeServiceInitializationStatus> {
        return sellService.invoke().initializationStatus
    }
}