package com.tangem.tap.network.exchangeServices

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tangem.domain.exchange.RampStateManager
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

internal class DefaultRampManager(
    private val sellService: Provider<SellService>,
    private val currenciesRepository: CurrenciesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : RampStateManager {

    override suspend fun availableForBuy(
        userWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): ScenarioUnavailabilityReason {
        return ScenarioUnavailabilityReason.None
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
        return ScenarioUnavailabilityReason.None
    }

    override fun getSellInitializationStatus(): Flow<SellServiceInitializationStatus> {
        return sellService.invoke().initializationStatus
    }

    override suspend fun fetchSellServiceData() {
        runCatching(dispatchers.io) {
            sellService.invoke().update()
        }
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
}