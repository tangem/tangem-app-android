package com.tangem.tap.network.exchangeServices

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.exchangeservice.swap.SwapServiceLoader
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.GetNetworkCoinStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class DefaultRampManager(
    private val exchangeService: ExchangeService?,
    private val buyService: Provider<ExchangeService>,
    private val sellService: Provider<ExchangeService>,
    private val swapServiceLoader: SwapServiceLoader,
    private val currenciesRepository: CurrenciesRepository,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    excludedBlockchains: ExcludedBlockchains,
) : RampStateManager {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter(excludedBlockchains)

    override fun isSellSupportedByService(cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForSell(
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override fun availableForBuy(scanResponse: ScanResponse, cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForBuy(
            scanResponse = scanResponse,
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override suspend fun availableForSell(userWalletId: UserWalletId, status: CryptoCurrencyStatus): Boolean {
        val sellSupportedByService = isSellSupportedByService(cryptoCurrency = status.currency)

        if (!sellSupportedByService) return false

        val reason = getSendUnavailabilityReason(userWalletId, status)

        return reason == ScenarioUnavailabilityReason.None
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

    private suspend fun getSendUnavailabilityReason(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): ScenarioUnavailabilityReason {
        val coinStatus = getNetworkCoinStatusUseCase.invokeSync(
            userWalletId = userWalletId,
            networkId = cryptoCurrencyStatus.currency.network.id,
            derivationPath = cryptoCurrencyStatus.currency.network.derivationPath,
            isSingleWalletWithTokens = false,
        ).getOrNull()

        return when {
            cryptoCurrencyStatus.value.amount.isNullOrZero() -> {
                ScenarioUnavailabilityReason.EmptyBalance(ScenarioUnavailabilityReason.WithdrawalScenario.SEND)
            }
            currenciesRepository.isSendBlockedByPendingTransactions(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                coinStatus = coinStatus,
            ) -> {
                ScenarioUnavailabilityReason.PendingTransaction(
                    withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SEND,
                    networkName = coinStatus?.currency?.network?.name.orEmpty(),
                )
            }
            else -> {
                ScenarioUnavailabilityReason.None
            }
        }
    }
}