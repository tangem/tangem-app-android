package com.tangem.domain.tokens

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
@Suppress("LongParameterList")
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val walletManagersFacade: WalletManagersFacade,
    private val marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
    private val stakingFeatureToggles: StakingFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Flow<TokenActionsState> {
        val operations = CurrenciesStatusesOperations(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            stakingRepository = stakingRepository,
            userWalletId = userWallet.walletId,
        )
        val networkId = cryptoCurrencyStatus.currency.network.id
        val requirements = walletManagersFacade.getAssetRequirements(userWallet.walletId, cryptoCurrencyStatus.currency)
        return flow {
            val networkFlow = if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                operations.getNetworkCoinForSingleWalletWithTokenFlow(networkId)
            } else if (!userWallet.isMultiCurrency) {
                operations.getPrimaryCurrencyStatusFlow()
            } else {
                operations.getNetworkCoinFlow(networkId, cryptoCurrencyStatus.currency.network.derivationPath)
            }

            val flow = networkFlow.mapLatest { maybeCoinStatus ->
                createTokenActionsState(
                    userWallet = userWallet,
                    coinStatus = maybeCoinStatus.getOrNull(),
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    needAssociateAsset = requirements != null,
                )
            }

            emitAll(flow)
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        needAssociateAsset: Boolean,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            states = createListOfActions(
                userWallet = userWallet,
                coinStatus = coinStatus,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                needAssociateAsset = needAssociateAsset,
            ),
        )
    }

    /**
     * Creates list of action for expected order
     * Actions priority: [Receive Send Swap Buy Sell]
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private suspend fun createListOfActions(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        needAssociateAsset: Boolean,
    ): List<TokenActionsState.ActionState> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation) {
            return listOf(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))
        }
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable) {
            return getActionsForUnreachableCurrency(userWallet, cryptoCurrencyStatus, needAssociateAsset)
        }

        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        // copy address
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            activeList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }

        // receive
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            val scenario = if (needAssociateAsset) {
                ScenarioUnavailabilityReason.UnassociatedAsset
            } else {
                ScenarioUnavailabilityReason.None
            }
            activeList.add(TokenActionsState.ActionState.Receive(scenario))
        }

        // staking
        if (stakingFeatureToggles.isStakingEnabled) {
            if (isStakingAvailable(cryptoCurrency)) {
                activeList.add(TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.None))
            } else {
                disabledList.add(
                    TokenActionsState.ActionState.Stake(
                        unavailabilityReason = ScenarioUnavailabilityReason.StakingUnavailable(cryptoCurrency.name),
                    ),
                )
            }
        }

        // send
        val sendUnavailabilityReason = getSendUnavailabilityReason(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            coinStatus = coinStatus,
        )
        if (sendUnavailabilityReason == ScenarioUnavailabilityReason.None) {
            activeList.add(TokenActionsState.ActionState.Send(sendUnavailabilityReason))
        } else {
            disabledList.add(TokenActionsState.ActionState.Send(sendUnavailabilityReason))
        }

        // swap
        if (userWallet.isMultiCurrency) {
            if (
                marketCryptoCurrencyRepository.isExchangeable(userWallet.walletId, cryptoCurrency) &&
                cryptoCurrencyStatus.value !is CryptoCurrencyStatus.NoQuote
            ) {
                activeList.add(TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.None))
            } else {
                disabledList.add(
                    TokenActionsState.ActionState.Swap(
                        unavailabilityReason = ScenarioUnavailabilityReason.NotExchangeable(cryptoCurrency.name),
                    ),
                )
            }
        }

        // buy
        if (rampManager.availableForBuy(userWallet.scanResponse, cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            disabledList.add(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrency.symbol)),
            )
        }

        // sell
        val sellSupportedByService = rampManager.availableForSell(cryptoCurrency)
        val sendAvailable = sendUnavailabilityReason is ScenarioUnavailabilityReason.None

        when {
            sellSupportedByService && sendAvailable -> {
                activeList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None))
            }
            sellSupportedByService && !sendAvailable -> {
                (sendUnavailabilityReason as? ScenarioUnavailabilityReason.EmptyBalance)?.let {
                    disabledList.add(
                        TokenActionsState.ActionState.Sell(
                            unavailabilityReason = it.copy(
                                withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SELL,
                            ),
                        ),
                    )
                }
                (sendUnavailabilityReason as? ScenarioUnavailabilityReason.PendingTransaction)?.let {
                    disabledList.add(
                        TokenActionsState.ActionState.Sell(
                            unavailabilityReason = it.copy(
                                withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SELL,
                            ),
                        ),
                    )
                }
            }
            else -> {
                disabledList.add(
                    TokenActionsState.ActionState.Sell(
                        unavailabilityReason = ScenarioUnavailabilityReason.NotSupportedBySellService(
                            cryptoCurrency.name,
                        ),
                    ),
                )
            }
        }

        // hide
        activeList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return activeList + disabledList
    }

    private fun getActionsForUnreachableCurrency(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        needAssociateAsset: Boolean,
    ): List<TokenActionsState.ActionState> {
        val actionsList = mutableListOf<TokenActionsState.ActionState>()

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }
        if (rampManager.availableForBuy(userWallet.scanResponse, cryptoCurrencyStatus.currency)) {
            actionsList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            actionsList.add(
                TokenActionsState.ActionState.Buy(
                    ScenarioUnavailabilityReason.BuyUnavailable(
                        cryptoCurrencyName = cryptoCurrencyStatus.currency.name,
                    ),
                ),
            )
        }
        actionsList.add(TokenActionsState.ActionState.Send(ScenarioUnavailabilityReason.Unreachable))
        actionsList.add(TokenActionsState.ActionState.Swap(ScenarioUnavailabilityReason.Unreachable))
        actionsList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.Unreachable))
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            val scenario = if (needAssociateAsset) {
                ScenarioUnavailabilityReason.UnassociatedAsset
            } else {
                ScenarioUnavailabilityReason.None
            }
            actionsList.add(TokenActionsState.ActionState.Receive(scenario))
        }
        if (stakingFeatureToggles.isStakingEnabled) {
            actionsList.add(TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.Unreachable))
        }
        actionsList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return actionsList
    }

    private fun getSendUnavailabilityReason(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        coinStatus: CryptoCurrencyStatus?,
    ): ScenarioUnavailabilityReason {
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

    private fun isAddressAvailable(networkAddress: NetworkAddress?): Boolean {
        return networkAddress != null && networkAddress.defaultAddress.value.isNotEmpty()
    }

    private suspend fun isStakingAvailable(cryptoCurrency: CryptoCurrency): Boolean {
        return stakingRepository.getStakingAvailabilityForActions(
            cryptoCurrencyId = cryptoCurrency.id,
            symbol = cryptoCurrency.symbol,
        ) is StakingAvailability.Available
    }
}
