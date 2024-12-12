package com.tangem.tap.network.exchangeServices

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.exchangeservice.swap.ExpressServiceLoader
import com.tangem.datasource.local.token.ExpressAssetsStore
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.GetNetworkCoinStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.onramp.OnrampFeatureToggles
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
    private val expressServiceLoader: ExpressServiceLoader,
    private val currenciesRepository: CurrenciesRepository,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val onrampFeatureToggles: OnrampFeatureToggles,
    private val expressAssetsStore: ExpressAssetsStore,
    excludedBlockchains: ExcludedBlockchains,
) : RampStateManager {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter(excludedBlockchains)

    override fun isSellSupportedByService(cryptoCurrency: CryptoCurrency): Boolean {
        return exchangeService?.availableForSell(
            currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
        ) ?: false
    }

    override suspend fun availableForBuy(
        scanResponse: ScanResponse,
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Boolean {
        return when {
            onrampFeatureToggles.isFeatureEnabled -> getOnrampAvailable(userWalletId, cryptoCurrency)
            exchangeService != null -> exchangeService.availableForBuy(
                scanResponse = scanResponse,
                currency = cryptoCurrencyConverter.convertBack(cryptoCurrency),
            )
            else -> false
        }
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
        return expressServiceLoader.getInitializationStatus(userWalletId)
    }

    private suspend fun getExchangeableFlag(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        return withContext(dispatchers.io) {
            val asset = expressServiceLoader.getInitializationStatus(userWalletId)
                .value
                .getOrNull()
                ?.find { cryptoCurrency.findAssetPredicate(it) }

            asset?.exchangeAvailable ?: false
        }
    }

    private suspend fun getOnrampAvailable(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean {
        val asset = expressAssetsStore.getSyncOrNull(userWalletId)?.find { cryptoCurrency.findAssetPredicate(it) }

        return asset?.onrampAvailable ?: false
    }

    private fun CryptoCurrency.findAssetPredicate(asset: Asset): Boolean {
        val contractAddress = (this as? CryptoCurrency.Token)?.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE
        return asset.network == network.backendId && asset.contractAddress.equals(contractAddress, ignoreCase = true)
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
