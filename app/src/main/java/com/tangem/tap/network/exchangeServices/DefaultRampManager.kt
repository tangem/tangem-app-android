package com.tangem.tap.network.exchangeServices

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.exchange.ExpressAvailabilityState
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultRampManager(
    private val sellService: Provider<SellService>,
    private val expressServiceFetcher: ExpressServiceFetcher,
    private val currenciesRepository: CurrenciesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : RampStateManager {

    override suspend fun availableForBuy(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): ScenarioUnavailabilityReason {
        val availabilityState = runCatching { getOnrampAvailableState(userWallet.walletId, cryptoCurrency) }
            .getOrNull()
            ?: ExpressAvailabilityState.Error

        return availabilityState.toReason(cryptoCurrency.name)
    }

    override suspend fun availableForSell(
        userWalletId: UserWalletId,
        status: CryptoCurrencyStatus,
        sendUnavailabilityReason: ScenarioUnavailabilityReason?,
    ): Either<ScenarioUnavailabilityReason, Unit> {
        return either {
            val isSellSupportedByService = catch(
                block = {
                    val serviceCurrency = CryptoCurrencyConverter.convert(status.currency)

                    sellService().availableForSell(currency = serviceCurrency)
                },
                catch = { raise(ScenarioUnavailabilityReason.NotSupportedBySellService(status.currency.name)) },
            )

            val reason = sendUnavailabilityReason
                ?: getSendUnavailabilityReason(userWalletId = userWalletId, cryptoCurrencyStatus = status)

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

            ensure(condition = isSellSupportedByService) {
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

    override fun getSellInitializationStatus(): Flow<SellServiceInitializationStatus> {
        return sellService.invoke().initializationStatus
    }

    override suspend fun fetchSellServiceData() {
        runCatching(dispatchers.io) {
            sellService.invoke().update()
        }
    }

    override fun getExpressInitializationStatus(userWalletId: UserWalletId): Flow<SellServiceInitializationStatus> {
        return expressServiceFetcher.getInitializationStatus(userWalletId)
    }

    override suspend fun getSendUnavailabilityReason(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): ScenarioUnavailabilityReason {
        val yieldSupplyStatus = cryptoCurrencyStatus.value.yieldSupplyStatus
        return when {
            cryptoCurrencyStatus.value.amount.isNullOrZero() -> {
                ScenarioUnavailabilityReason.EmptyBalance(ScenarioUnavailabilityReason.WithdrawalScenario.SEND)
            }
            currenciesRepository.isSendBlockedByPendingTransactions(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ) -> {
                ScenarioUnavailabilityReason.PendingTransaction(
                    withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SEND,
                    networkName = cryptoCurrencyStatus.currency.network.name,
                )
            }
            yieldSupplyStatus?.isAllowedToSpend == false && yieldSupplyStatus.isActive -> {
                ScenarioUnavailabilityReason.YieldSupplyApprovalRequired
            }
            else -> ScenarioUnavailabilityReason.None
        }
    }

    override fun checkAssetRequirements(requirements: AssetRequirementsCondition?): Boolean {
        return when (requirements) {
            AssetRequirementsCondition.PaidTransaction,
            is AssetRequirementsCondition.PaidTransactionWithFee,
            is AssetRequirementsCondition.RequiredTrustline,
            -> false
            is AssetRequirementsCondition.IncompleteTransaction,
            null,
            -> true
        }
    }

    private suspend fun getExchangeableState(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ExpressAvailabilityState {
        val asset = expressServiceFetcher.getInitializationStatus(userWalletId).firstOrNull()
            ?: return ExpressAvailabilityState.Loading
        return when (asset) {
            is Lce.Error -> ExpressAvailabilityState.Error
            is Lce.Loading -> ExpressAvailabilityState.Loading
            is Lce.Content -> {
                val foundAsset = asset.getOrNull()?.find { cryptoCurrency.findAssetPredicate(assetId = it.id) }
                foundAsset?.isExchangeAvailable?.toSwapAvailabilityState()
                    ?: ExpressAvailabilityState.AssetNotFound
            }
        }
    }

    private suspend fun getOnrampAvailableState(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): ExpressAvailabilityState {
        val asset = expressServiceFetcher.getInitializationStatus(userWalletId).firstOrNull()
            ?: return ExpressAvailabilityState.Loading

        return when (asset) {
            is Lce.Error -> ExpressAvailabilityState.Error
            is Lce.Loading -> ExpressAvailabilityState.Loading
            is Lce.Content -> {
                val foundAsset = asset.getOrNull()?.find { cryptoCurrency.findAssetPredicate(assetId = it.id) }
                foundAsset?.isOnrampAvailable?.toOnrampAvailabilityState()
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

    private fun CryptoCurrency.findAssetPredicate(assetId: ExpressAsset.ID): Boolean {
        val currencyAssedId = ExpressAsset.ID(
            networkId = this.network.backendId,
            contractAddress = (this as? CryptoCurrency.Token)?.contractAddress,
        )

        return assetId.networkId == currencyAssedId.networkId &&
            assetId.contractAddress.equals(currencyAssedId.contractAddress, ignoreCase = true)
    }
}