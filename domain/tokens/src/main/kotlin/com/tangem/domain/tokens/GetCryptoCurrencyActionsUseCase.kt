package com.tangem.domain.tokens

import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.repositories.StakingRepository
import com.tangem.domain.tokens.model.*
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.domain.transaction.models.AssetRequirementsCondition
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Use case to determine which TokenActions are available for a [CryptoCurrency]
 *
 * @property rampManager Ramp manager to check ramp availability
 */
@Suppress("LongParameterList")
class GetCryptoCurrencyActionsUseCase(
    private val rampManager: RampStateManager,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val quotesRepository: QuotesRepository,
    private val networksRepository: NetworksRepository,
    private val stakingRepository: StakingRepository,
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
        val requirements = withTimeoutOrNull(REQUEST_EXCHANGE_DATA_TIMEOUT) {
            walletManagersFacade.getAssetRequirements(userWallet.walletId, cryptoCurrencyStatus.currency)
        }
        return flow {
            val networkFlow = if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                operations.getNetworkCoinForSingleWalletWithTokenFlow(networkId)
            } else if (!userWallet.isMultiCurrency) {
                operations.getPrimaryCurrencyStatusFlow(includeQuotes = false)
            } else {
                operations.getNetworkCoinFlow(
                    networkId = networkId,
                    derivationPath = cryptoCurrencyStatus.currency.network.derivationPath,
                    includeQuotes = false,
                )
            }

            val flow = networkFlow.mapLatest { maybeCoinStatus ->
                createTokenActionsState(
                    userWallet = userWallet,
                    coinStatus = maybeCoinStatus.getOrNull(),
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                    requirements = requirements,
                )
            }

            emitAll(flow)
        }.flowOn(dispatchers.io)
    }

    private suspend fun createTokenActionsState(
        userWallet: UserWallet,
        coinStatus: CryptoCurrencyStatus?,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        requirements: AssetRequirementsCondition?,
    ): TokenActionsState {
        return TokenActionsState(
            walletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            states = createListOfActions(
                userWallet = userWallet,
                coinStatus = coinStatus,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                requirements = requirements,
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
        requirements: AssetRequirementsCondition?,
    ): List<TokenActionsState.ActionState> {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.MissedDerivation) {
            return listOf(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))
        }
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Unreachable) {
            return getActionsForUnreachableCurrency(userWallet, cryptoCurrencyStatus, requirements)
        }

        val activeList = mutableListOf<TokenActionsState.ActionState>()
        val disabledList = mutableListOf<TokenActionsState.ActionState>()

        // markets
        // not a custom token
        if (cryptoCurrencyStatus.currency.id.rawCurrencyId != null) {
            activeList.add(TokenActionsState.ActionState.Analytics(ScenarioUnavailabilityReason.None))
        }

        // copy address
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            activeList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }

        // receive
        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            val scenario = getReceiveScenario(requirements)
            activeList.add(TokenActionsState.ActionState.Receive(scenario))
        }

        // staking
        if (isStakingAvailable(userWallet, cryptoCurrency)) {
            val yield = kotlin.runCatching {
                stakingRepository.getYield(
                    cryptoCurrencyId = cryptoCurrency.id,
                    symbol = cryptoCurrency.symbol,
                )
            }.getOrNull()
            activeList.add(
                TokenActionsState.ActionState.Stake(
                    unavailabilityReason = ScenarioUnavailabilityReason.None,
                    yield = yield,
                ),
            )
        } else {
            disabledList.add(
                TokenActionsState.ActionState.Stake(
                    unavailabilityReason = ScenarioUnavailabilityReason.StakingUnavailable(cryptoCurrency.name),
                    yield = null,
                ),
            )
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
            val isExchangeable = withTimeoutOrNull(REQUEST_EXCHANGE_DATA_TIMEOUT) {
                rampManager.availableForSwap(userWallet.walletId, cryptoCurrency)
            } ?: false
            if (isExchangeable && cryptoCurrencyStatus.value !is CryptoCurrencyStatus.NoQuote) {
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
        if (rampManager.availableForBuy(userWallet.scanResponse, userWallet.walletId, cryptoCurrency)) {
            activeList.add(TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.None))
        } else {
            disabledList.add(
                TokenActionsState.ActionState.Buy(ScenarioUnavailabilityReason.BuyUnavailable(cryptoCurrency.symbol)),
            )
        }

        // region sell
        rampManager.availableForSell(
            userWalletId = userWallet.walletId,
            status = cryptoCurrencyStatus,
        )
            .onRight {
                activeList.add(TokenActionsState.ActionState.Sell(ScenarioUnavailabilityReason.None))
            }
            .onLeft { reason ->
                disabledList.add(TokenActionsState.ActionState.Sell(reason))
            }
        // endregion

        // hide
        activeList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return activeList + disabledList
    }

    private suspend fun getActionsForUnreachableCurrency(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        requirements: AssetRequirementsCondition?,
    ): List<TokenActionsState.ActionState> {
        val actionsList = mutableListOf<TokenActionsState.ActionState>()

        if (isAddressAvailable(cryptoCurrencyStatus.value.networkAddress)) {
            actionsList.add(TokenActionsState.ActionState.CopyAddress(ScenarioUnavailabilityReason.None))
        }
        if (rampManager.availableForBuy(userWallet.scanResponse, userWallet.walletId, cryptoCurrencyStatus.currency)) {
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
            val scenario = getReceiveScenario(requirements)
            actionsList.add(TokenActionsState.ActionState.Receive(scenario))
        }
        actionsList.add(TokenActionsState.ActionState.Stake(ScenarioUnavailabilityReason.Unreachable, null))
        actionsList.add(TokenActionsState.ActionState.HideToken(ScenarioUnavailabilityReason.None))

        return actionsList
    }

    private fun getReceiveScenario(requirements: AssetRequirementsCondition?): ScenarioUnavailabilityReason {
        return if (requirements is AssetRequirementsCondition.PaidTransaction ||
            requirements is AssetRequirementsCondition.PaidTransactionWithFee
        ) {
            ScenarioUnavailabilityReason.UnassociatedAsset
        } else {
            ScenarioUnavailabilityReason.None
        }
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

    private suspend fun isStakingAvailable(userWallet: UserWallet, cryptoCurrency: CryptoCurrency): Boolean {
        return stakingRepository.getStakingAvailability(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrency,
        ) is StakingAvailability.Available
    }

    private companion object {
        const val REQUEST_EXCHANGE_DATA_TIMEOUT = 1000L
    }
}