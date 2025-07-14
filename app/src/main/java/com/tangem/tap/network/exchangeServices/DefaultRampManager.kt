package com.tangem.tap.network.exchangeServices

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.datasource.api.express.models.TangemExpressValues.EMPTY_CONTRACT_ADDRESS_VALUE
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.exchangeservice.swap.ExpressServiceLoader
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.exchange.ExpressAvailabilityState
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.GetNetworkCoinStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Suppress("LongParameterList")
internal class DefaultRampManager(
    private val sellService: Provider<ExchangeService>,
    private val expressServiceLoader: ExpressServiceLoader,
    private val currenciesRepository: CurrenciesRepository,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    excludedBlockchains: ExcludedBlockchains,
) : RampStateManager {

    private val cryptoCurrencyConverter = CryptoCurrencyConverter(excludedBlockchains)

    override suspend fun availableForBuy(
        scanResponse: ScanResponse,
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ScenarioUnavailabilityReason {
        val availabilityState = runCatching { getOnrampAvailableState(userWalletId, cryptoCurrency) }
            .getOrNull()
            ?: ExpressAvailabilityState.Error

        return availabilityState.toReason(cryptoCurrency.name)
    }

    override suspend fun availableForSell(
        userWallet: UserWallet,
        status: CryptoCurrencyStatus,
    ): Either<ScenarioUnavailabilityReason, Unit> {
        return either {
            val sellSupportedByService = catch(
                block = {
                    val serviceCurrency = cryptoCurrencyConverter.convertBack(status.currency)

                    sellService().availableForSell(currency = serviceCurrency)
                },
                catch = { raise(ScenarioUnavailabilityReason.NotSupportedBySellService(status.currency.name)) },
            )

            val reason = getSendUnavailabilityReason(userWallet = userWallet, cryptoCurrencyStatus = status)

            ensure(condition = reason is ScenarioUnavailabilityReason.None) {
                when (reason) {
                    is ScenarioUnavailabilityReason.EmptyBalance -> {
                        reason.copy(withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SELL)
                    }
                    is ScenarioUnavailabilityReason.PendingTransaction -> {
                        reason.copy(withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SELL)
                    }
                    else -> reason
                }
            }

            ensure(condition = sellSupportedByService) {
                ScenarioUnavailabilityReason.NotSupportedBySellService(status.currency.name)
            }

            Unit.right()
        }
    }

    override suspend fun availableForSwap(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ScenarioUnavailabilityReason {
        val availabilityState = runCatching {
            getExchangeableState(userWalletId, cryptoCurrency)
        }.getOrNull() ?: ExpressAvailabilityState.Error
        return availabilityState.toReason(cryptoCurrency.name)
    }

    override fun getSellInitializationStatus(): Flow<ExchangeServiceInitializationStatus> {
        return sellService.invoke().initializationStatus
    }

    override suspend fun fetchSellServiceData() {
        runCatching(dispatchers.io) {
            sellService.invoke().update()
        }
    }

    override fun getExpressInitializationStatus(userWalletId: UserWalletId): Flow<ExchangeServiceInitializationStatus> {
        return expressServiceLoader.getInitializationStatus(userWalletId)
    }

    private suspend fun getExchangeableState(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ExpressAvailabilityState {
        val asset = expressServiceLoader.getInitializationStatus(userWalletId).firstOrNull()
            ?: return ExpressAvailabilityState.Loading
        return when (asset) {
            is Lce.Error -> ExpressAvailabilityState.Error
            is Lce.Loading -> ExpressAvailabilityState.Loading
            is Lce.Content -> {
                val foundAsset = asset.getOrNull()?.find { cryptoCurrency.findAssetPredicate(it) }
                foundAsset?.exchangeAvailable?.toSwapAvailabilityState()
                    ?: ExpressAvailabilityState.AssetNotFound
            }
        }
    }

    private suspend fun getOnrampAvailableState(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ExpressAvailabilityState {
        val asset = expressServiceLoader.getInitializationStatus(userWalletId).firstOrNull()
            ?: return ExpressAvailabilityState.Loading

        return when (asset) {
            is Lce.Error -> ExpressAvailabilityState.Error
            is Lce.Loading -> ExpressAvailabilityState.Loading
            is Lce.Content -> {
                val foundAsset = asset.getOrNull()?.find { cryptoCurrency.findAssetPredicate(it) }
                foundAsset?.onrampAvailable?.toOnrampAvailabilityState()
                    ?: ExpressAvailabilityState.AssetNotFound
            }
        }
    }

    private fun ExpressAvailabilityState.toReason(currencyName: String): ScenarioUnavailabilityReason {
        return when (this) {
            ExpressAvailabilityState.Available -> {
                ScenarioUnavailabilityReason.None
            }
            ExpressAvailabilityState.AssetNotFound -> ScenarioUnavailabilityReason.AssetNotFound(currencyName)
            ExpressAvailabilityState.Error -> ScenarioUnavailabilityReason.ExpressUnreachable(currencyName)
            ExpressAvailabilityState.Loading -> ScenarioUnavailabilityReason.ExpressLoading(currencyName)
            ExpressAvailabilityState.NotOnrampable -> ScenarioUnavailabilityReason.BuyUnavailable(currencyName)
            ExpressAvailabilityState.NotExchangeable -> ScenarioUnavailabilityReason.NotExchangeable(currencyName)
        }
    }

    private fun Boolean.toSwapAvailabilityState(): ExpressAvailabilityState {
        return if (this) {
            ExpressAvailabilityState.Available
        } else {
            ExpressAvailabilityState.NotExchangeable
        }
    }

    private fun Boolean.toOnrampAvailabilityState(): ExpressAvailabilityState {
        return if (this) {
            ExpressAvailabilityState.Available
        } else {
            ExpressAvailabilityState.NotOnrampable
        }
    }

    private fun CryptoCurrency.findAssetPredicate(asset: Asset): Boolean {
        val contractAddress = (this as? CryptoCurrency.Token)?.contractAddress ?: EMPTY_CONTRACT_ADDRESS_VALUE
        return asset.network == network.backendId && asset.contractAddress.equals(contractAddress, ignoreCase = true)
    }

    private suspend fun getSendUnavailabilityReason(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): ScenarioUnavailabilityReason {
        val coinStatus = getNetworkCoinStatusUseCase.invokeSync(
            userWallet = userWallet,
            networkId = cryptoCurrencyStatus.currency.network.id,
            derivationPath = cryptoCurrencyStatus.currency.network.derivationPath,
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